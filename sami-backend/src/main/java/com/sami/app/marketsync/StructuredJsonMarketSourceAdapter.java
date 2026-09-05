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
import java.util.Map;

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
            RestClient client=RestClient.builder().requestFactory(requests).build();
            String authEnv=text(source.config(),"authEnv"),authHeader=text(source.config(),"authHeader");
            String credential=authEnv==null?null:System.getenv(authEnv);
            if(authEnv!=null&&(credential==null||credential.isBlank()))throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,"Configured market source credential is unavailable");
            String body = client.get().uri(source.endpointUrl()).headers(h->{if(credential!=null)h.set(authHeader==null?"Authorization":authHeader,credential);}).retrieve().body(String.class);
            if(body!=null&&body.length()>10_000_000)throw new ApiException(ErrorCode.BAD_REQUEST,"Market source response exceeds 10 MB");
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
    private String text(Map<String,Object> values,String key){Object value=values.get(key);return value==null||String.valueOf(value).isBlank()?null:String.valueOf(value);}
}
