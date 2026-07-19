package com.sami.app.scheduling.provider;

import com.sami.app.scheduling.spi.ConflictResolutionStrategy;
import com.sami.app.scheduling.spi.ReminderChannelHandler;
import com.sami.app.scheduling.spi.ScheduleSubjectProvider;
import com.sami.app.scheduling.spi.SlotAllocationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the module's {@code handler_key} columns to their beans.
 *
 * <p>A configured key with no bean is inert, not fatal — the same contract the
 * files and automation registries use. That is what allows V24 to seed repair,
 * trade-in and WhatsApp handlers today: the rows describe intent, and the
 * behaviour appears when the owning module ships.
 */
public final class SchedulingRegistries {

    private SchedulingRegistries() { }

    private static <T> TreeMap<String, T> index(List<T> beans, Function<T, String> keyFn) {
        return beans.stream().collect(Collectors.toMap(
                keyFn, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    @Slf4j
    @Component
    public static class SubjectProviders {
        private final TreeMap<String, ScheduleSubjectProvider> byKey;

        public SubjectProviders(List<ScheduleSubjectProvider> providers) {
            this.byKey = index(providers, ScheduleSubjectProvider::key);
            log.info("Schedule subject providers registered: {}", byKey.keySet());
        }

        public Optional<ScheduleSubjectProvider> find(String key) {
            return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key));
        }

        public List<String> keys() {
            return List.copyOf(byKey.keySet());
        }
    }

    @Slf4j
    @Component
    public static class ConflictStrategies {
        private final TreeMap<String, ConflictResolutionStrategy> byKey;

        public ConflictStrategies(List<ConflictResolutionStrategy> strategies) {
            this.byKey = index(strategies, ConflictResolutionStrategy::key);
        }

        public Optional<ConflictResolutionStrategy> find(String key) {
            return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key));
        }

        /** Falls back to "prevent" — the conservative choice when misconfigured. */
        public ConflictResolutionStrategy requireOrPrevent(String key) {
            return find(key).orElseGet(() -> {
                log.warn("No conflict strategy for key '{}'; falling back to prevent", key);
                return byKey.get("prevent");
            });
        }
    }

    @Slf4j
    @Component
    public static class AllocationStrategies {
        private final TreeMap<String, SlotAllocationStrategy> byKey;

        public AllocationStrategies(List<SlotAllocationStrategy> strategies) {
            this.byKey = index(strategies, SlotAllocationStrategy::key);
        }

        public SlotAllocationStrategy resolve(String key) {
            SlotAllocationStrategy found = key == null ? null : byKey.get(key);
            return found != null ? found : byKey.get(EarliestFirstAllocation.KEY);
        }
    }

    @Slf4j
    @Component
    public static class ReminderChannels {
        private final TreeMap<String, ReminderChannelHandler> byKey;

        public ReminderChannels(List<ReminderChannelHandler> handlers) {
            this.byKey = index(handlers, ReminderChannelHandler::key);
            if (byKey.isEmpty()) {
                log.info("No reminder channel handlers registered; reminders will be "
                        + "queued but not delivered until a notification module provides them");
            }
        }

        public Optional<ReminderChannelHandler> find(String key) {
            return key == null ? Optional.empty() : Optional.ofNullable(byKey.get(key));
        }
    }
}
