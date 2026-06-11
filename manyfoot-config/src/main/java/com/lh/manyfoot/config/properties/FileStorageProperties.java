package com.lh.manyfoot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置类
 *
 * @author airx
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "many-foot.file-storage")
public class FileStorageProperties {

    /**
     * 下载文件的最大字节数，默认 50 MiB
     */
    private long maxDownloadBytes = 52_428_800L;
}
