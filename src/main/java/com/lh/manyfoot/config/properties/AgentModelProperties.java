package com.lh.manyfoot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "many-foot.model")
public class AgentModelProperties {

    /**
     * 模型厂商
     */
    private VendorEnums vendor;

    /**
     * 密钥
     */
    private String apiKey;

    /**
     * 地址
     */
    private String baseUrl;
}
