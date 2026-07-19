package com.sami.app.config;

import com.sami.app.common.storage.StorageProperties;
import com.sami.app.crm.CrmProperties;
import com.sami.app.demo.DemoProperties;
import com.sami.app.purchasing.PurchasingProperties;
import com.sami.app.supplier.SupplierProperties;
import com.sami.app.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers all type-safe {@code @ConfigurationProperties} beans in one place. */
@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        CorsProperties.class,
        BootstrapProperties.class,
        StorageProperties.class,
        CrmProperties.class,
        PurchasingProperties.class,
        SupplierProperties.class,
        DemoProperties.class
})
public class PropertiesConfig {
}
