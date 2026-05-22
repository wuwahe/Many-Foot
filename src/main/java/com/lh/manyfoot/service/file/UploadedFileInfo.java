package com.lh.manyfoot.service.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传结果 DTO
 *
 * @author airx
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedFileInfo {

    /**
     * 文件存储路径（相对于存储根目录）
     */
    private String path;

    /**
     * 文件 MIME 类型
     */
    private String mimeType;

    /**
     * 文件类型分类（如 image、document 等）
     */
    private String type;
}
