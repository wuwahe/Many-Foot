package com.lh.manyfoot.controller;

import com.lh.manyfoot.service.file.DownloadFile;
import com.lh.manyfoot.service.file.FileStorageException;
import com.lh.manyfoot.service.file.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void download_shouldReturnAttachmentBytes() throws Exception {
        when(fileStorageService.loadForDownload("session-1", "/workspace/data/hello.txt"))
            .thenReturn(new DownloadFile("hello.txt", "text/plain", 14, "hello ManyFoot".getBytes()));

        mockMvc.perform(get("/api/files/download")
                .param("sessionId", "session-1")
                .param("path", "/workspace/data/hello.txt"))
            .andExpect(status().isOk())
            .andExpect(content().bytes("hello ManyFoot".getBytes()))
            .andExpect(header().string("Content-Type", "text/plain"))
            .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"hello.txt\""));
    }

    @Test
    void download_shouldReturn400_whenBadRequest() throws Exception {
        when(fileStorageService.loadForDownload(any(), any()))
            .thenThrow(FileStorageException.badRequest("invalid path"));

        mockMvc.perform(get("/api/files/download")
                .param("sessionId", "session-1")
                .param("path", "../../etc/passwd"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void download_shouldReturn404_whenNotFound() throws Exception {
        when(fileStorageService.loadForDownload(any(), any()))
            .thenThrow(FileStorageException.notFound("file not found"));

        mockMvc.perform(get("/api/files/download")
                .param("sessionId", "session-1")
                .param("path", "/workspace/data/missing.txt"))
            .andExpect(status().isNotFound());
    }

    @Test
    void download_shouldReturn413_whenPayloadTooLarge() throws Exception {
        when(fileStorageService.loadForDownload(any(), any()))
            .thenThrow(FileStorageException.payloadTooLarge("file too large"));

        mockMvc.perform(get("/api/files/download")
                .param("sessionId", "session-1")
                .param("path", "/workspace/data/huge.bin"))
            .andExpect(status().isPayloadTooLarge());
    }
}
