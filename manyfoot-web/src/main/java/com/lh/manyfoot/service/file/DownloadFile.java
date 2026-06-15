package com.lh.manyfoot.service.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件下载 DTO，封装二进制文件内容及元数据
 *
 * @author airx
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadFile {

    /**
     * 文件名（含扩展名）
     */
    private String filename;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private long contentLength;

    /**
     * 文件二进制内容
     */
    private byte[] content;
}
