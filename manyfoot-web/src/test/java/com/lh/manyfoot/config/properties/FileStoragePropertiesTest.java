package com.lh.manyfoot.config.properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FileStorageProperties 默认值验证
 *
 * @author airx
 */
class FileStoragePropertiesTest {

    @Test
    void defaultMaxDownloadBytes_shouldBe50MiB() {
        FileStorageProperties properties = new FileStorageProperties();
        assertThat(properties.getMaxDownloadBytes()).isEqualTo(52_428_800L);
    }
}
