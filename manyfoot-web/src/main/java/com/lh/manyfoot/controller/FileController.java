package com.lh.manyfoot.controller;

import com.lh.manyfoot.service.file.DownloadFile;
import com.lh.manyfoot.service.file.FileStorageException;
import com.lh.manyfoot.service.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam String sessionId,
                                            @RequestParam String path) {
        try {
            DownloadFile downloadFile = fileStorageService.loadForDownload(sessionId, path);

            String safeFilename = downloadFile.getFilename()
                .replace("\"", "")
                .replace("\r", "")
                .replace("\n", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(downloadFile.getContentType()));
            headers.setContentLength(downloadFile.getContentLength());
            headers.setContentDispositionFormData("attachment", safeFilename);

            return new ResponseEntity<>(downloadFile.getContent(), headers, HttpStatus.OK);
        } catch (FileStorageException e) {
            throw new ResponseStatusException(e.getStatus(), e.getMessage());
        }
    }
}
