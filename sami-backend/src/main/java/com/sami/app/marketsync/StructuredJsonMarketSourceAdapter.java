package com.sami.app.marketsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Opt-in adapter for an authorized JSON contract: {items:[{number,price,cost?}]}. */
@Component
@RequiredArgsConstructor
public class StructuredJsonMarketSourceAdapter implements MarketSourceAdapter {
    private final ObjectMapper mapper;
    @Override public String key() { return "STRUCTURED_JSON_V1"; }
    @Override public FetchResult fetch(SourceConfig source) {
        if (source.endpointUrl() == null || !source.endpointUrl().startsWith("https://"))
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "An HTTPS structured endpoint is required");
        try {
            SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
            requests.setConnectTimeout(Duration.ofSeconds(5)); requests.setReadTimeout(Duration.ofSeconds(15));
            String body = RestClient.builder().requestFactory(requests).build()
                    .get().uri(source.endpointUrl()).retrieve().body(String.class);
            JsonNode root = mapper.readTree(body); JsonNode items = root.path("items");
            if (!items.isArray()) throw new IllegalArgumentException("items array missing");
            List<SourceItem> parsed = new ArrayList<>(); List<String> warnings = new ArrayList<>();
            for (JsonNode item : items) {
                try { parsed.add(new SourceItem(item.path("number").asText(), item.path("price").decimalValue(),
                        item.hasNonNull("cost") ? item.path("cost").decimalValue() : null)); }
                catch (RuntimeException ex) { warnings.add("Malformed source row ignored"); }
            }
            return new FetchResult(parsed, warnings);
        } catch (Exception ex) { throw new ApiException(ErrorCode.BAD_REQUEST, "Market source fetch failed: " + ex.getMessage()); }
    }
}
