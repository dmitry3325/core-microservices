package com.corems.common.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "corems.security.jwt")
public class CoremsJwtProperties {

    private Internal internal = new Internal();
    private External external = new External();

    @Getter
    @Setter
    public static class Internal {
        private boolean enabled = true;
        private String jwkPublicKey;
        private String jwkPrivateKey;
    }

    @Getter
    @Setter
    public static class External {
        private boolean enabled = true;
        private String jwkPublicKey;
    }
}

