package com.sami.app.config;

import com.sami.app.common.storage.StorageProperties;
import com.sami.app.crm.CrmProperties;
import com.sami.app.common.scheduler.SchedulerProperties;
import com.sami.app.demo.DemoProperties;
import com.sami.app.files.FilesProperties;
import com.sami.app.portal.PortalProperties;
import com.sami.app.notification.DemoNotificationProperties;
import com.sami.app.licensing.LicensingProperties;
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
        DemoProperties.class,
        LicensingProperties.class,
        FilesProperties.class,
        SchedulerProperties.class,
        PortalProperties.class,
        DemoNotificationProperties.class
})
public class PropertiesConfig {
}
