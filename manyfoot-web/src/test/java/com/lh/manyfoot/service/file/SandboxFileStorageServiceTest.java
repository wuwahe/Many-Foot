package com.lh.manyfoot.service.file;

import com.lh.manyfoot.config.properties.FileStorageProperties;
import com.lh.manyfoot.config.properties.SandboxConfig;
import com.lh.manyfoot.domain.SandboxContainer;
import com.lh.manyfoot.service.SandboxContainerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SandboxFileStorageServiceTest {

    @Mock
    private ObjectProvider<SandboxContainerManager> sandboxContainerManagerProvider;

    @Mock
    private SandboxContainerManager sandboxContainerManager;

    @Mock
    private SandboxConfig sandboxConfig;

    private FileStorageProperties fileStorageProperties;

    private SandboxFileStorageService service;

    @BeforeEach
    void setUp() {
        fileStorageProperties = new FileStorageProperties();
        fileStorageProperties.setMaxDownloadBytes(1024 * 1024); // 1 MB for tests
        service = new SandboxFileStorageService(sandboxContainerManagerProvider, sandboxConfig, fileStorageProperties);
    }

    @Test
    void loadForDownload_shouldReadLocalUploadedCopy(@TempDir Path tempDir) throws IOException {
        // Arrange
        String sessionId = "test-session";
        String path = "/workspace/data/hello.txt";
        String localPath = tempDir.resolve(sessionId).resolve("data").resolve("hello.txt").toString();
        
        when(sandboxConfig.getWorkspaceMount()).thenReturn("/workspace");
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(sandboxContainerManager);
        when(sandboxContainerManager.toLocalAttachmentPath(sessionId, path)).thenReturn(localPath);
        
        // Create local file
        Files.createDirectories(Path.of(localPath).getParent());
        Files.writeString(Path.of(localPath), "hello ManyFoot");

        // Act
        DownloadFile result = service.loadForDownload(sessionId, path);

        // Assert
        assertThat(result.getFilename()).isEqualTo("hello.txt");
        assertThat(new String(result.getContent())).isEqualTo("hello ManyFoot");
        assertThat(result.getContentType()).isEqualTo("text/plain");
        verify(sandboxContainerManager, never()).readFileBytes(any(), any(), anyLong());
    }

    @Test
    void loadForDownload_shouldFallbackToSandbox_whenLocalMissing() {
        // Arrange
        String sessionId = "test-session";
        String path = "/workspace/reports/result.txt";
        String containerId = "container-1";
        
        when(sandboxConfig.getWorkspaceMount()).thenReturn("/workspace");
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(sandboxContainerManager);
        when(sandboxContainerManager.toLocalAttachmentPath(sessionId, path))
            .thenThrow(new IllegalArgumentException("local path not available"));
        
        SandboxContainer container = new SandboxContainer();
        container.setContainerId(containerId);
        when(sandboxContainerManager.getContainerBySession(sessionId)).thenReturn(container);
        when(sandboxContainerManager.readFileBytes(containerId, path, 1024 * 1024))
            .thenReturn("generated report".getBytes());

        // Act
        DownloadFile result = service.loadForDownload(sessionId, path);

        // Assert
        assertThat(result.getFilename()).isEqualTo("result.txt");
        assertThat(new String(result.getContent())).isEqualTo("generated report");
        verify(sandboxContainerManager).readFileBytes(containerId, path, 1024 * 1024);
    }

    @Test
    void loadForDownload_shouldRejectPathTraversal() {
        when(sandboxConfig.getWorkspaceMount()).thenReturn("/workspace");

        assertThatThrownBy(() -> service.loadForDownload("session", "../../application.yml"))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.loadForDownload("session", "/etc/passwd"))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST));
    }

    @Test
    void loadForDownload_shouldRejectBlankSessionIdOrPath() {
        assertThatThrownBy(() -> service.loadForDownload(null, "/workspace/file.txt"))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> service.loadForDownload("session", null))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST));
    }

    @Test
    void loadForDownload_shouldRejectOversizedFile(@TempDir Path tempDir) throws IOException {
        // Arrange
        String sessionId = "test-session";
        String path = "/workspace/data/large.bin";
        String localPath = tempDir.resolve(sessionId).resolve("data").resolve("large.bin").toString();
        
        fileStorageProperties.setMaxDownloadBytes(10);
        service = new SandboxFileStorageService(sandboxContainerManagerProvider, sandboxConfig, fileStorageProperties);
        
        when(sandboxConfig.getWorkspaceMount()).thenReturn("/workspace");
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(sandboxContainerManager);
        when(sandboxContainerManager.toLocalAttachmentPath(sessionId, path)).thenReturn(localPath);
        
        // Create oversized file (100 bytes > 10 bytes limit)
        Files.createDirectories(Path.of(localPath).getParent());
        Files.write(Path.of(localPath), new byte[100]);

        // Act & Assert
        assertThatThrownBy(() -> service.loadForDownload(sessionId, path))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void loadForDownload_shouldReturn503_whenSandboxUnavailable() {
        when(sandboxConfig.getWorkspaceMount()).thenReturn("/workspace");
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.loadForDownload("session", "/workspace/file.txt"))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void upload_shouldSanitizeFilename() throws IOException {
        // Arrange
        String sessionId = "test-session";
        MockMultipartFile file = new MockMultipartFile("file", "../../evil.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
        
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(sandboxContainerManager);
        when(sandboxContainerManager.uploadFile(eq(sessionId), eq("evil.txt"), any())).thenReturn("/workspace/data/evil.txt");

        // Act
        UploadedFileInfo result = service.upload(sessionId, file);

        // Assert
        assertThat(result.getPath()).isEqualTo("/workspace/data/evil.txt");
        verify(sandboxContainerManager).uploadFile(sessionId, "evil.txt", "content".getBytes());
    }

    @Test
    void upload_shouldReturn503_whenSandboxUnavailable() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
        when(sandboxContainerManagerProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> service.upload("session", file))
            .isInstanceOf(FileStorageException.class)
            .satisfies(ex -> assertThat(((FileStorageException) ex).getStatus()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    }
}
