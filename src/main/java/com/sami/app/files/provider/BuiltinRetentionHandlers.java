package com.sami.app.files.provider;

import com.sami.app.files.spi.RetentionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The four retention outcomes, as beans keyed by
 * {@code retention_policies.action_on_expiry}.
 *
 * <p>These deliberately do not destroy data themselves — they return the
 * intended outcome and {@code RetentionService} performs the state change inside
 * its own transaction, so a handler can never partially delete.
 */
@Slf4j
@Configuration
public class BuiltinRetentionHandlers {

    @Bean
    public RetentionHandler deleteRetentionHandler() {
        return handler("delete", "Marked for deletion by retention policy");
    }

    @Bean
    public RetentionHandler archiveRetentionHandler() {
        return handler("archive", "Archived by retention policy");
    }

    @Bean
    public RetentionHandler anonymiseRetentionHandler() {
        return handler("anonymise", "Metadata anonymised by retention policy");
    }

    @Bean
    public RetentionHandler notifyRetentionHandler() {
        return handler("notify", "Retention expiry notified; file retained");
    }

    private RetentionHandler handler(String action, String message) {
        return new RetentionHandler() {
            @Override
            public String action() {
                return action;
            }

            @Override
            public String apply(Long fileId) {
                return message;
            }
        };
    }
}
