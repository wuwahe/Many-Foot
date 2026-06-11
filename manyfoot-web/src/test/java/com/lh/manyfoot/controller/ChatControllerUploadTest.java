package com.lh.manyfoot.controller;

import com.lh.manyfoot.agent.impl.SupervisorAgent;
import com.lh.manyfoot.agent.stream.ConversationEventStreamFactory;
import com.lh.manyfoot.service.SandboxContainerManager;
import com.lh.manyfoot.service.file.FileStorageService;
import com.lh.manyfoot.service.file.UploadedFileInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerUploadTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupervisorAgent supervisorAgent;

    @MockBean
    private SandboxContainerManager sandboxContainerManager;

    @MockBean
    private ConversationEventStreamFactory eventStreamFactory;

    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void upload_shouldKeepExistingResponseShape() throws Exception {
        when(fileStorageService.upload(any(), any()))
            .thenReturn(new UploadedFileInfo("/workspace/data/test.txt", "text/plain", "file"));

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());

        mockMvc.perform(multipart("/api/chat/upload")
                .file(file)
                .param("sessionId", "test-session"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.path").value("/workspace/data/test.txt"))
            .andExpect(jsonPath("$.mimeType").value("text/plain"))
            .andExpect(jsonPath("$.type").value("file"));
    }
}
