package com.lh.manyfoot.service.file;

import com.lh.manyfoot.config.properties.FileStorageProperties;
import com.lh.manyfoot.config.properties.SandboxConfig;
import com.lh.manyfoot.service.SandboxContainerManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxFileStorageService implements FileStorageService {

    private final ObjectProvider<SandboxContainerManager> sandboxContainerManagerProvider;
    private final SandboxConfig sandboxConfig;
    private final FileStorageProperties fileStorageProperties;

    @Override
    public UploadedFileInfo upload(String sessionId, MultipartFile file) {
        if (sessionId == null || sessionId.isBlank()) {
            throw FileStorageException.badRequest("sessionId cannot be blank");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw FileStorageException.badRequest("filename cannot be blank");
        }

        String safeFilename = Path.of(originalName).getFileName().toString();
        if (safeFilename.isBlank() || ".".equals(safeFilename) || "..".equals(safeFilename)) {
            throw FileStorageException.badRequest("invalid filename: " + safeFilename);
        }

        SandboxContainerManager manager = sandboxContainerManagerProvider.getIfAvailable();
        if (manager == null) {
            throw FileStorageException.serviceUnavailable("sandbox not available");
        }

        try {
            byte[] bytes = file.getBytes();
            String path = manager.uploadFile(sessionId, safeFilename, bytes);

            String mimeType = file.getContentType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = Files.probeContentType(Path.of(safeFilename));
            }
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String type = mimeType.toLowerCase().startsWith("image/") ? "image" : "file";
            return new UploadedFileInfo(path, mimeType, type);
        } catch (IOException e) {
            throw FileStorageException.serverError("upload failed", e);
        }
    }

    @Override
    public DownloadFile loadForDownload(String sessionId, String path) {
        if (sessionId == null || sessionId.isBlank()) {
            throw FileStorageException.badRequest("sessionId cannot be blank");
        }
        if (path == null || path.isBlank()) {
            throw FileStorageException.badRequest("path cannot be blank");
        }

        String workspaceMount = sandboxConfig.getWorkspaceMount();
        if (!path.equals(workspaceMount) && !path.startsWith(workspaceMount + "/")) {
            throw FileStorageException.badRequest("path must be under workspace: " + workspaceMount);
        }

        String safeFilename;
        try {
            safeFilename = Path.of(path).getFileName().toString();
        } catch (InvalidPathException e) {
            throw FileStorageException.badRequest("invalid path format");
        }

        long maxBytes = fileStorageProperties.getMaxDownloadBytes();

        // Try local copy first
        try {
            SandboxContainerManager manager = sandboxContainerManagerProvider.getIfAvailable();
            if (manager != null) {
                String localPath = manager.toLocalAttachmentPath(sessionId, path);
                Path localFile = Path.of(localPath);
                if (Files.exists(localFile) && Files.isRegularFile(localFile)) {
                    long size = Files.size(localFile);
                    if (maxBytes > 0 && size > maxBytes) {
                        throw FileStorageException.payloadTooLarge(
                            "file too large: " + size + " bytes, max: " + maxBytes);
                    }
                    byte[] bytes = Files.readAllBytes(localFile);
                    String contentType = Files.probeContentType(localFile);
                    if (contentType == null || contentType.isBlank()) {
                        contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
                    }
                    return new DownloadFile(safeFilename, contentType, size, bytes);
                }
            }
        } catch (IllegalArgumentException e) {
            log.debug("Local attachment path not available: {}", e.getMessage());
        } catch (IOException e) {
            log.warn("Failed to read local copy: sessionId={}, path={}", sessionId, path, e);
        }

        // Fallback to sandbox
        SandboxContainerManager manager = sandboxContainerManagerProvider.getIfAvailable();
        if (manager == null) {
            throw FileStorageException.serviceUnavailable("sandbox not available");
        }

        var container = manager.getContainerBySession(sessionId);
        if (container == null) {
            throw FileStorageException.notFound("container not found for session: " + sessionId);
        }

        try {
            byte[] bytes = manager.readFileBytes(container.getContainerId(), path, maxBytes);
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            try {
                String detected = Files.probeContentType(Path.of(safeFilename));
                if (detected != null && !detected.isBlank()) {
                    contentType = detected;
                }
            } catch (IOException ignored) {
            }
            return new DownloadFile(safeFilename, contentType, bytes.length, bytes);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("过大")) {
                throw FileStorageException.payloadTooLarge(msg);
            }
            if (msg != null && (msg.contains("不存在") || msg.contains("not found"))) {
                throw FileStorageException.notFound(msg);
            }
            throw FileStorageException.serverError("download failed", e);
        }
    }
}
