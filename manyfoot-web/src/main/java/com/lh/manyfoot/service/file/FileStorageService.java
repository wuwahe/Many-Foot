package com.lh.manyfoot.service.file;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 * <p>
 * 定义文件上传与下载的统一契约，屏蔽底层存储实现细节（本地文件系统、OSS 等）。
 * 所有路径均相对于会话隔离的存储根目录，不暴露容器 ID 或宿主机路径。
 *
 * @author airx
 */
public interface FileStorageService {

    /**
     * 上传文件到指定会话的存储空间
     *
     * @param sessionId 会话 ID，用于隔离不同会话的文件
     * @param file      上传的文件
     * @return 上传结果，包含存储路径、MIME 类型和文件类型分类
     * @throws FileStorageException 上传失败时抛出
     */
    UploadedFileInfo upload(String sessionId, MultipartFile file);

    /**
     * 加载文件用于下载
     *
     * @param sessionId 会话 ID
     * @param path      文件相对路径（由 upload 返回）
     * @return 下载文件 DTO，包含文件名、内容类型、大小和二进制内容
     * @throws FileStorageException 文件不存在或读取失败时抛出
     */
    DownloadFile loadForDownload(String sessionId, String path);
}
