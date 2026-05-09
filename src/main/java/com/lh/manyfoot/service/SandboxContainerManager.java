package com.lh.manyfoot.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.lh.manyfoot.config.properties.SandboxConfig;
import com.lh.manyfoot.domain.ContainerStatus;
import com.lh.manyfoot.domain.ExecutionResult;
import com.lh.manyfoot.domain.ExecutionStatus;
import com.lh.manyfoot.domain.SandboxContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * 沙箱容器管理服务
 * 负责 Docker 容器的生命周期管理
 *
 * @author airx
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "many-foot.sandbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SandboxContainerManager {

    private final DockerClient dockerClient;
    private final SandboxConfig sandboxConfig;
    private final ResourceLoader resourceLoader;

    public SandboxContainerManager(@Lazy DockerClient dockerClient, SandboxConfig sandboxConfig, ResourceLoader resourceLoader) {
        this.dockerClient = dockerClient;
        this.sandboxConfig = sandboxConfig;
        this.resourceLoader = resourceLoader;
    }

    // Redis 键前缀
    private static final String CONTAINER_KEY_PREFIX = "many-foot:container:";
    private static final String SESSION_CONTAINER_KEY = "many-foot:session:container:";

    // 本地容器缓存
    private final Map<String, SandboxContainer> containerCache = new ConcurrentHashMap<>();

    /**
     * 为会话获取或创建沙箱容器
     *
     * @param sessionId 会话ID
     * @param tenantId  租户ID
     * @param userId    用户ID
     * @return 沙箱容器信息
     */
    public SandboxContainer getOrCreateContainer(String sessionId, Long tenantId, Long userId) {
        // 1. 检查是否已有关联的容器
        String existingContainerId = RedisUtils.getCacheObject(SESSION_CONTAINER_KEY + sessionId);
        if (StrUtil.isNotBlank(existingContainerId)) {
            SandboxContainer container = getContainer(existingContainerId);
            if (container != null && container.isAvailable()) {
                container.touch();
                saveContainer(container);
                return container;
            }
        }

        // 2. 检查容器数量限制
        if (containerCache.size() >= sandboxConfig.getMaxContainers()) {
            // 尝试清理空闲容器
            cleanupIdleContainers();
            if (containerCache.size() >= sandboxConfig.getMaxContainers()) {
                throw new RuntimeException("已达到最大容器数量限制: " + sandboxConfig.getMaxContainers());
            }
        }

        // 3. 创建新容器
        return createContainer(sessionId, tenantId, userId);
    }

    /**
     * 创建新的沙箱容器
     */
    private SandboxContainer createContainer(String sessionId, Long tenantId, Long userId) {
        String containerId = null;
        try {
            // 生成容器名称
            String containerName = sandboxConfig.getContainerPrefix() + IdUtil.fastSimpleUUID().substring(0, 8);

            // 创建工作目录
            String workspacePath = sandboxConfig.getHostWorkspacePath() + "/" + sessionId;
            Path workDir = Path.of(workspacePath);
            if (!Files.exists(workDir)) {
                Files.createDirectories(workDir);
            }

            // 构建容器配置
            HostConfig hostConfig = HostConfig.newHostConfig()
                .withMemory(sandboxConfig.getMemoryLimit() * 1024 * 1024) // 转换为字节
                .withCpuCount(sandboxConfig.getCpuLimit().longValue())
                .withBinds(new Bind(workspacePath, new Volume(sandboxConfig.getWorkspaceMount())))
                .withNetworkMode(sandboxConfig.getNetworkEnabled() ? sandboxConfig.getNetworkMode() : "none")
                .withAutoRemove(false)
                .withPrivileged(sandboxConfig.getPrivileged())
                .withReadonlyRootfs(sandboxConfig.getReadOnlyRootfs());

            // 创建容器
            CreateContainerResponse response = dockerClient.createContainerCmd(sandboxConfig.getImageName())
                .withName(containerName)
                .withHostConfig(hostConfig)
                .withWorkingDir(sandboxConfig.getWorkspaceMount())
                .withTty(true)
                .withStdinOpen(true)
                .withCmd("/bin/bash", "-c", "tail -f /dev/null") // 保持容器运行
                .exec();

            containerId = response.getId();

            // 启动容器
            dockerClient.startContainerCmd(containerId).exec();
            syncSkillsToContainer(containerId);

            // 获取容器信息
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            String ipAddress = "";
            if (inspect.getNetworkSettings() != null && inspect.getNetworkSettings().getNetworks() != null) {
                Map<String, ContainerNetwork> networks = inspect.getNetworkSettings().getNetworks();
                if (!networks.isEmpty()) {
                    ipAddress = networks.values().iterator().next().getIpAddress();
                }
            }

            // 构建容器实体
            SandboxContainer container = SandboxContainer.builder()
                .containerId(containerId)
                .containerName(containerName)
                .sessionId(sessionId)
                .tenantId(tenantId)
                .userId(userId)
                .status(ContainerStatus.RUNNING)
                .createTime(LocalDateTime.now())
                .lastActiveTime(LocalDateTime.now())
                .workspacePath(workspacePath)
                .ipAddress(ipAddress)
                .imageName(sandboxConfig.getImageName())
                .memoryLimit(sandboxConfig.getMemoryLimit())
                .cpuLimit(sandboxConfig.getCpuLimit())
                .build();

            // 保存到 Redis 和本地缓存
            saveContainer(container);
            RedisUtils.setCacheObject(SESSION_CONTAINER_KEY + sessionId, containerId, Duration.ofHours(24));

            log.info("沙箱容器创建成功: containerId={}, sessionId={}", containerId, sessionId);
            return container;

        } catch (Exception e) {
            log.error("创建沙箱容器失败: sessionId={}", sessionId, e);
            // 清理失败的容器
            if (containerId != null) {
                try {
                    dockerClient.removeContainerCmd(containerId).withForce(true).exec();
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException("创建沙箱容器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在容器中执行命令
     *
     * @param containerId 容器ID
     * @param command     命令数组
     * @param timeout     超时时间(秒)
     * @return 执行结果
     */
    public ExecutionResult executeCommand(String containerId, String[] command, int timeout) {
        String executionId = IdUtil.fastSimpleUUID();
        long startTime = System.currentTimeMillis();

        try {
            // 创建执行命令
            ExecCreateCmdResponse execCreateResponse = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd(command)
                .withWorkingDir(sandboxConfig.getWorkspaceMount())
                .exec();

            String execId = execCreateResponse.getId();

            // 捕获输出
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            // 执行命令
            dockerClient.execStartCmd(execId)
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        try {
                            if (frame.getStreamType() == StreamType.STDOUT) {
                                stdout.write(frame.getPayload());
                            } else if (frame.getStreamType() == StreamType.STDERR) {
                                stderr.write(frame.getPayload());
                            }
                        } catch (IOException e) {
                            log.error("写入输出流失败", e);
                        }
                    }
                })
                .awaitCompletion(timeout, TimeUnit.SECONDS);

            // 获取退出码
            Long exitCode = dockerClient.inspectExecCmd(execId).exec().getExitCodeLong();
            long executionTime = System.currentTimeMillis() - startTime;

            // 更新容器活动时间
            updateLastActiveTime(containerId);

            // 构建结果
            if (exitCode == null) {
                return ExecutionResult.timeout(executionId, executionTime);
            }

            return ExecutionResult.builder()
                .executionId(executionId)
                .exitCode(exitCode.intValue())
                .stdout(stdout.toString(StandardCharsets.UTF_8))
                .stderr(stderr.toString(StandardCharsets.UTF_8))
                .executionTime(executionTime)
                .timeout(false)
                .status(exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.timeout(executionId, System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("执行命令失败: containerId={}", containerId, e);
            return ExecutionResult.error(executionId, e.getMessage());
        }
    }

    /**
     * 向容器写入文件
     * 使用 Docker exec 在容器内直接写入，避免卷挂载问题
     */
    public void writeFile(String containerId, String path, String content) {
        SandboxContainer container = getContainer(containerId);
        if (container == null) {
            throw new RuntimeException("容器不存在: " + containerId);
        }

        try {
            // 确保路径以 /workspace 开头
            String containerPath = path.startsWith("/") ? path : "/" + path;
            if (!containerPath.startsWith(sandboxConfig.getWorkspaceMount())) {
                containerPath = sandboxConfig.getWorkspaceMount() + containerPath;
            }

            // 获取父目录
            String parentDir = containerPath.substring(0, containerPath.lastIndexOf('/'));
            if (StrUtil.isNotBlank(parentDir)) {
                // 创建父目录
                executeInContainer(containerId, new String[]{"mkdir", "-p", parentDir});
            }

            // 使用 base64 编码内容，避免特殊字符问题
            String base64Content = java.util.Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

            // 使用 echo + base64 解码写入文件
            String[] command = {
                "/bin/bash", "-c",
                String.format("echo '%s' | base64 -d > '%s'", base64Content, containerPath)
            };

            ExecutionResult result = executeInContainer(containerId, command);
            if (!result.isSuccess()) {
                throw new RuntimeException("写入文件失败: " + result.getStderr());
            }

            updateLastActiveTime(containerId);
            log.debug("文件写入成功: containerId={}, path={}", containerId, containerPath);
        } catch (Exception e) {
            throw new RuntimeException("写入文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在容器内执行命令（内部方法）
     */
    private ExecutionResult executeInContainer(String containerId, String[] command) {
        String executionId = IdUtil.fastSimpleUUID();
        try {
            ExecCreateCmdResponse execCreate = dockerClient.execCreateCmd(containerId)
                .withCmd(command)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            dockerClient.execStartCmd(execCreate.getId())
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        if (frame.getStreamType() == StreamType.STDOUT) {
                            stdout.writeBytes(frame.getPayload());
                        } else if (frame.getStreamType() == StreamType.STDERR) {
                            stderr.writeBytes(frame.getPayload());
                        }
                    }
                })
                .awaitCompletion(30, TimeUnit.SECONDS);

            Long exitCode = dockerClient.inspectExecCmd(execCreate.getId()).exec().getExitCodeLong();

            return ExecutionResult.builder()
                .executionId(executionId)
                .exitCode(exitCode != null ? exitCode.intValue() : -1)
                .stdout(stdout.toString(StandardCharsets.UTF_8))
                .stderr(stderr.toString(StandardCharsets.UTF_8))
                .status(exitCode != null && exitCode == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED)
                .build();
        } catch (Exception e) {
            return ExecutionResult.error(executionId, e.getMessage());
        }
    }

    /**
     * 从容器读取文件
     * 使用 Docker exec 在容器内直接读取
     */
    public String readFile(String containerId, String path) {
        SandboxContainer container = getContainer(containerId);
        if (container == null) {
            throw new RuntimeException("容器不存在: " + containerId);
        }

        try {
            // 确保路径以 /workspace 开头
            String containerPath = path.startsWith("/") ? path : "/" + path;
            if (!containerPath.startsWith(sandboxConfig.getWorkspaceMount())) {
                containerPath = sandboxConfig.getWorkspaceMount() + containerPath;
            }

            // 使用 cat 命令读取文件内容
            ExecutionResult result = executeInContainer(containerId, new String[]{"cat", containerPath});

            if (!result.isSuccess()) {
                throw new RuntimeException("文件不存在或无法读取: " + path);
            }

            updateLastActiveTime(containerId);
            return result.getStdout();
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从容器读取二进制文件。
     * 使用 base64 作为传输格式，避免 PDF、Office 等二进制内容在 Docker 输出流中被 UTF-8 解码破坏。
     */
    public byte[] readFileBytes(String containerId, String path) {
        return readFileBytes(containerId, path, 0);
    }

    /**
     * 从容器读取二进制文件，并限制最大读取字节数。
     */
    public byte[] readFileBytes(String containerId, String path, long maxBytes) {
        SandboxContainer container = getContainer(containerId);
        if (container == null) {
            throw new RuntimeException("容器不存在: " + containerId);
        }

        try {
            String containerPath = resolveWorkspacePath(path);
            assertReadableFileSize(containerId, containerPath, maxBytes);

            String command = "base64 " + shellQuote(containerPath) + " | tr -d '\\n'";
            ExecutionResult result = executeInContainer(containerId, new String[]{"/bin/bash", "-c", command});
            if (!result.isSuccess()) {
                throw new RuntimeException("文件不存在或无法读取: " + path);
            }

            updateLastActiveTime(containerId);
            return Base64.getDecoder().decode(result.getStdout());
        } catch (Exception e) {
            throw new RuntimeException("读取二进制文件失败: " + e.getMessage(), e);
        }
    }

    private void assertReadableFileSize(String containerId, String containerPath, long maxBytes) {
        if (maxBytes <= 0) {
            return;
        }
        String command = "stat -c %s -- " + shellQuote(containerPath);
        ExecutionResult result = executeInContainer(containerId, new String[]{"/bin/bash", "-c", command});
        if (!result.isSuccess()) {
            throw new RuntimeException("文件不存在或无法读取: " + containerPath);
        }
        try {
            long fileSize = Long.parseLong(result.getStdout().trim());
            if (fileSize > maxBytes) {
                throw new RuntimeException("文件过大，最大支持 " + maxBytes + " bytes，当前文件 " + fileSize + " bytes");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("无法读取文件大小: " + containerPath, e);
        }
    }

    private String resolveWorkspacePath(String path) {
        if (StrUtil.isBlank(path)) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        try {
            Path workspacePath = Path.of(sandboxConfig.getWorkspaceMount()).normalize();
            String requestedPath = path.startsWith("/") ? path : "/" + path;
            Path containerPath;
            if (requestedPath.equals(sandboxConfig.getWorkspaceMount())
                || requestedPath.startsWith(sandboxConfig.getWorkspaceMount() + "/")) {
                containerPath = Path.of(requestedPath).normalize();
            } else {
                containerPath = workspacePath.resolve(requestedPath.substring(1)).normalize();
            }

            if (!containerPath.equals(workspacePath) && !containerPath.startsWith(workspacePath)) {
                throw new IllegalArgumentException("文件路径不能越过沙箱工作目录: " + sandboxConfig.getWorkspaceMount());
            }
            return containerPath.toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("文件路径格式非法", e);
        }
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    /**
     * 列出目录内容
     * 使用 Docker exec 在容器内直接列出
     */
    public List<String> listDirectory(String containerId, String path) {
        SandboxContainer container = getContainer(containerId);
        if (container == null) {
            throw new RuntimeException("容器不存在: " + containerId);
        }

        try {
            // 确保路径以 /workspace 开头
            String containerPath = path.startsWith("/") ? path : "/" + path;
            if (!containerPath.startsWith(sandboxConfig.getWorkspaceMount())) {
                containerPath = sandboxConfig.getWorkspaceMount() + containerPath;
            }

            // 使用 ls -1F 命令列出目录内容（-1 每行一个，-F 添加文件类型标识）
            ExecutionResult result = executeInContainer(containerId, new String[]{"ls", "-1F", containerPath});

            if (!result.isSuccess()) {
                throw new RuntimeException("目录不存在: " + path);
            }

            List<String> entries = new ArrayList<>();
            String output = result.getStdout();
            if (StrUtil.isNotBlank(output)) {
                for (String line : output.split("\n")) {
                    if (StrUtil.isNotBlank(line)) {
                        entries.add(line.trim());
                    }
                }
            }

            updateLastActiveTime(containerId);
            return entries;
        } catch (Exception e) {
            throw new RuntimeException("列出目录失败: " + e.getMessage(), e);
        }
    }

    private void syncSkillsToContainer(String containerId) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
            Resource[] resources = resolver.getResources("classpath*:skills/**");

            ExecutionResult mkdirResult = executeInContainer(containerId,
                new String[]{"mkdir", "-p", sandboxConfig.getWorkspaceMount() + "/skills"});
            if (!mkdirResult.isSuccess()) {
                throw new IOException("创建容器 skills 目录失败: " + mkdirResult.getStderr());
            }
            int copiedCount = copySkillsArchiveToContainer(containerId, resources);
            if (copiedCount == 0) {
                throw new IOException("classpath 下未找到可同步的 skills 文件");
            }
            log.info("skills 同步完成: containerId={}, target={}, copiedFiles={}",
                containerId, sandboxConfig.getWorkspaceMount() + "/skills", copiedCount);
        } catch (Exception e) {
            throw new RuntimeException("同步 skills 到容器失败: " + e.getMessage(), e);
        }
    }

    private int copySkillsArchiveToContainer(String containerId, Resource[] resources) throws IOException {
        int copiedCount = 0;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             TarArchiveOutputStream tar = new TarArchiveOutputStream(baos)) {
            for (Resource resource : resources) {
                if (!resource.exists() || !resource.isReadable()) {
                    continue;
                }
                String relativePath = resolveSkillResourceRelativePath(resource);
                if (StrUtil.isBlank(relativePath) || relativePath.endsWith("/")) {
                    continue;
                }

                String archivePath = resolveSkillArchivePath(relativePath);
                byte[] content;
                try (var inputStream = resource.getInputStream()) {
                    content = inputStream.readAllBytes();
                }
                TarArchiveEntry entry = new TarArchiveEntry(archivePath);
                entry.setSize(content.length);
                tar.putArchiveEntry(entry);
                tar.write(content);
                tar.closeArchiveEntry();
                copiedCount++;
            }
            tar.finish();
            dockerClient.copyArchiveToContainerCmd(containerId)
                .withRemotePath(sandboxConfig.getWorkspaceMount())
                .withTarInputStream(new ByteArrayInputStream(baos.toByteArray()))
                .exec();
        }
        return copiedCount;
    }

    private String resolveSkillArchivePath(String relativePath) throws IOException {
        Path normalizedPath = Path.of(relativePath).normalize();
        if (normalizedPath.isAbsolute() || normalizedPath.startsWith("..")) {
            throw new IOException("非法 skills 资源路径: " + relativePath);
        }
        return "skills/" + normalizedPath.toString().replace(File.separatorChar, '/');
    }

    private String resolveSkillResourceRelativePath(Resource resource) throws IOException {
        String resourcePath = resource.getURL().toString();
        int skillsIndex = resourcePath.indexOf("/skills/");
        if (skillsIndex < 0) {
            return null;
        }
        String relativePath = resourcePath.substring(skillsIndex + "/skills/".length());
        return URLDecoder.decode(relativePath, StandardCharsets.UTF_8);
    }

    /**
     * 停止容器
     */
    public void stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).withTimeout(10).exec();
            SandboxContainer container = getContainer(containerId);
            if (container != null) {
                container.setStatus(ContainerStatus.STOPPED);
                saveContainer(container);
            }
            log.info("容器已停止: {}", containerId);
        } catch (Exception e) {
            log.error("停止容器失败: {}", containerId, e);
        }
    }

    /**
     * 销毁容器
     */
    public void destroyContainer(String containerId) {
        try {
            SandboxContainer container = getContainer(containerId);

            // 停止并删除容器
            try {
                dockerClient.stopContainerCmd(containerId).withTimeout(5).exec();
            } catch (Exception ignored) {
            }
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();

            // 清理工作目录
            if (container != null && container.getWorkspacePath() != null) {
                try {
                    deleteDirectory(new File(container.getWorkspacePath()));
                } catch (Exception e) {
                    log.warn("清理工作目录失败: {}", container.getWorkspacePath());
                }
            }

            // 从缓存和 Redis 中移除
            containerCache.remove(containerId);
            RedisUtils.deleteObject(CONTAINER_KEY_PREFIX + containerId);
            if (container != null) {
                RedisUtils.deleteObject(SESSION_CONTAINER_KEY + container.getSessionId());
            }

            log.info("容器已销毁: {}", containerId);
        } catch (Exception e) {
            log.error("销毁容器失败: {}", containerId, e);
        }
    }

    /**
     * 更新容器活动时间
     */
    public void updateLastActiveTime(String containerId) {
        SandboxContainer container = getContainer(containerId);
        if (container != null) {
            container.touch();
            saveContainer(container);
        }
    }

    /**
     * 获取容器信息
     */
    public SandboxContainer getContainer(String containerId) {
        // 先从本地缓存获取
        SandboxContainer container = containerCache.get(containerId);
        if (container != null) {
            return container;
        }

        // 从 Redis 获取
        container = RedisUtils.getCacheObject(CONTAINER_KEY_PREFIX + containerId);
        if (container != null) {
            containerCache.put(containerId, container);
        }
        return container;
    }

    /**
     * 保存容器信息
     */
    private void saveContainer(SandboxContainer container) {
        containerCache.put(container.getContainerId(), container);
        RedisUtils.setCacheObject(CONTAINER_KEY_PREFIX + container.getContainerId(), container, Duration.ofHours(24));
    }

    /**
     * 获取容器状态
     */
    public ContainerStatus getContainerStatus(String containerId) {
        try {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(containerId).exec();
            InspectContainerResponse.ContainerState state = inspect.getState();
            if (state == null) {
                return ContainerStatus.FAILED;
            }
            if (Boolean.TRUE.equals(state.getRunning())) {
                return ContainerStatus.RUNNING;
            } else if (Boolean.TRUE.equals(state.getPaused())) {
                return ContainerStatus.PAUSED;
            } else {
                return ContainerStatus.STOPPED;
            }
        } catch (Exception e) {
            log.error("获取容器状态失败: {}", containerId, e);
            return ContainerStatus.FAILED;
        }
    }

    /**
     * 定时清理空闲容器
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cleanupIdleContainers() {
        log.debug("开始清理空闲容器...");
        int cleanedCount = 0;

        for (SandboxContainer container : new ArrayList<>(containerCache.values())) {
            if (container.isIdleTimeout(sandboxConfig.getIdleTimeout())) {
                log.info("清理空闲容器: containerId={}, lastActiveTime={}",
                    container.getContainerId(), container.getLastActiveTime());
                destroyContainer(container.getContainerId());
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("已清理 {} 个空闲容器", cleanedCount);
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    /**
     * 通过会话ID获取容器
     */
    public SandboxContainer getContainerBySession(String sessionId) {
        String containerId = RedisUtils.getCacheObject(SESSION_CONTAINER_KEY + sessionId);
        if (StrUtil.isNotBlank(containerId)) {
            return getContainer(containerId);
        }
        return null;
    }

    /**
     * 上传文件到容器的 /workspace/data 目录，同时在应用本机保存一份副本。
     * <p>
     * 容器可能运行在远程 Docker 宿主机上，应用进程无法直接读取容器内文件。
     * 因此上传时会在 {@code localAttachmentPath}/{sessionId}/data/ 下保存一份副本，
     * 供多模态模型读取图片时使用。
     *
     * @param sessionId 会话ID，用于定位容器和本地存储目录
     * @param filename  文件名
     * @param content   文件内容字节数组
     * @return 容器内文件路径，格式 /workspace/data/{filename}
     */
    public String uploadFile(String sessionId, String filename, byte[] content) {
        SandboxContainer container = getOrCreateContainer(sessionId, null, null);
        String targetDir = sandboxConfig.getWorkspaceMount() + "/data";
        executeInContainer(container.getContainerId(), new String[]{"mkdir", "-p", targetDir});
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             TarArchiveOutputStream tar = new TarArchiveOutputStream(baos)) {
            TarArchiveEntry entry = new TarArchiveEntry(filename);
            entry.setSize(content.length);
            tar.putArchiveEntry(entry);
            tar.write(content);
            tar.closeArchiveEntry();
            tar.finish();
            dockerClient.copyArchiveToContainerCmd(container.getContainerId())
                    .withRemotePath(targetDir)
                    .withTarInputStream(new ByteArrayInputStream(baos.toByteArray()))
                    .exec();
        } catch (IOException e) {
            throw new RuntimeException("上传文件失败: " + e.getMessage(), e);
        }
        saveLocalCopy(sessionId, filename, content);
        return targetDir + "/" + filename;
    }

    /**
     * 将容器内路径转换为应用本机的附件副本路径。
     * <p>
     * 容器可能运行在远程 Docker 宿主机上，应用进程无法直接访问容器宿主机文件系统。
     * 上传文件时会同时在本机保存副本，此方法用于将容器路径映射到本机副本路径，
     * 供 {@code AgentMessageFactory} 构建多模态 {@code Media} 时读取图片。
     *
     * @param sessionId    会话ID
     * @param containerPath 容器内路径，必须位于 {@code workspaceMount} 下
     * @return 本机附件副本路径
     * @throws IllegalArgumentException 路径为空、不在工作目录内或包含路径遍历
     */
    public String toLocalAttachmentPath(String sessionId, String containerPath) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId 不能为空");
        }
        if (containerPath == null || containerPath.isBlank()) {
            throw new IllegalArgumentException("附件路径不能为空");
        }
        String workspaceMount = sandboxConfig.getWorkspaceMount();
        if (!containerPath.equals(workspaceMount) && !containerPath.startsWith(workspaceMount + "/")) {
            throw new IllegalArgumentException("附件路径必须位于沙箱工作目录内: " + workspaceMount);
        }
        try {
            Path relativePath = Path.of(workspaceMount).relativize(Path.of(containerPath)).normalize();
            if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
                throw new IllegalArgumentException("附件路径不能越过会话工作目录");
            }
            Path localBase = Path.of(sandboxConfig.getLocalAttachmentPath(), sessionId).normalize();
            Path localPath = localBase.resolve(relativePath).normalize();
            if (!localPath.startsWith(localBase)) {
                throw new IllegalArgumentException("附件本地路径不能越过会话工作目录");
            }
            return localPath.toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException("附件路径格式非法", e);
        }
    }

    /**
     * 在应用本机保存文件副本。
     * <p>
     * 容器与应用分离部署时，多模态模型需要从本地读取图片文件。
     * 此方法将上传的文件保存到 {@code localAttachmentPath}/{sessionId}/data/ 目录下，
     * 路径与容器内结构保持一致，便于 {@link #toLocalAttachmentPath} 进行路径映射。
     */
    private void saveLocalCopy(String sessionId, String filename, byte[] content) {
        Path localDir = Path.of(sandboxConfig.getLocalAttachmentPath(), sessionId, "data");
        try {
            Files.createDirectories(localDir);
            Files.write(localDir.resolve(filename), content);
        } catch (IOException e) {
            log.warn("保存附件本地副本失败: sessionId={}, filename={}", sessionId, filename, e);
        }
    }
}
