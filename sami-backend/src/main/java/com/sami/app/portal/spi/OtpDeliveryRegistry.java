package com.sami.app.portal.spi;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves an OTP delivery channel. Empty by default — see {@link OtpDeliveryChannel}. */
@Component
public class OtpDeliveryRegistry {

    private final TreeMap<String, OtpDeliveryChannel> byKey;

    public OtpDeliveryRegistry(List<OtpDeliveryChannel> channels) {
        this.byKey = channels.stream().collect(Collectors.toMap(
                OtpDeliveryChannel::key, Function.identity(), (a, b) -> a, TreeMap::new));
    }

    public Optional<OtpDeliveryChannel> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    public List<String> keys() {
        return List.copyOf(byKey.keySet());
    }
}
