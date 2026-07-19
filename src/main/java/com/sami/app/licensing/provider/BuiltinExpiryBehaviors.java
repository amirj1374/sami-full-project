package com.sami.app.licensing.provider;

import com.sami.app.licensing.spi.ExpiryBehaviorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The four shipped post-expiry modes. Each only decides what stays permitted —
 * none touches business data, so an expired subscription can never corrupt it.
 * Additional modes register as further {@link ExpiryBehaviorHandler} beans.
 */
@Configuration
public class BuiltinExpiryBehaviors {

    /** Full access while inside the grace window; nothing afterwards. */
    @Bean
    ExpiryBehaviorHandler graceExpiryBehavior() {
        return new ExpiryBehaviorHandler() {
            @Override
            public String code() {
                return "grace";
            }

            @Override
            public boolean permits(String featureCode, boolean coreFeature, boolean withinGrace) {
                return withinGrace;
            }

            @Override
            public boolean permitsWrites(boolean withinGrace) {
                return withinGrace;
            }
        };
    }

    /** Everything remains readable; writes are rejected. */
    @Bean
    ExpiryBehaviorHandler readOnlyExpiryBehavior() {
        return new ExpiryBehaviorHandler() {
            @Override
            public String code() {
                return "read-only";
            }

            @Override
            public boolean permits(String featureCode, boolean coreFeature, boolean withinGrace) {
                return true;
            }

            @Override
            public boolean permitsWrites(boolean withinGrace) {
                return false;
            }
        };
    }

    /** Only features flagged as core survive; writes follow the grace window. */
    @Bean
    ExpiryBehaviorHandler limitedExpiryBehavior() {
        return new ExpiryBehaviorHandler() {
            @Override
            public String code() {
                return "limited";
            }

            @Override
            public boolean permits(String featureCode, boolean coreFeature, boolean withinGrace) {
                return coreFeature;
            }

            @Override
            public boolean permitsWrites(boolean withinGrace) {
                return withinGrace;
            }
        };
    }

    /** All licensed functionality is refused. */
    @Bean
    ExpiryBehaviorHandler blockedExpiryBehavior() {
        return new ExpiryBehaviorHandler() {
            @Override
            public String code() {
                return "blocked";
            }

            @Override
            public boolean permits(String featureCode, boolean coreFeature, boolean withinGrace) {
                return false;
            }

            @Override
            public boolean permitsWrites(boolean withinGrace) {
                return false;
            }
        };
    }
}
