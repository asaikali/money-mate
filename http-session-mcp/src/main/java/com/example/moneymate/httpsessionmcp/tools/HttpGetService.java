package com.example.moneymate.httpsessionmcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class HttpGetService {

    private static final String INVALID_REQUEST = "INVALID_REQUEST";
    private static final String NETWORK_ERROR = "NETWORK_ERROR";
    private static final String UNSUPPORTED_CONTENT_TYPE = "UNSUPPORTED_CONTENT_TYPE";
    private static final String INVALID_JSON_RESPONSE = "INVALID_JSON_RESPONSE";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpGetService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public Object get(String url, Map<String, String> headers) {
        URI uri = validateUrl(url);
        if (uri == null) {
            return new HttpGatewayError(
                INVALID_REQUEST,
                "URL must be an absolute http or https URL."
            );
        }

        try {
            return restClient.get()
                .uri(uri)
                .headers(httpHeaders -> applyHeaders(httpHeaders, headers))
                .exchange((request, response) -> {
                    HttpHeaders responseHeaders = response.getHeaders();
                    MediaType contentType = responseHeaders.getContentType();
                    byte[] bodyBytes = response.getBody().readAllBytes();

                    if (!isSupportedContentType(contentType)) {
                        return new HttpGatewayError(
                            UNSUPPORTED_CONTENT_TYPE,
                            "Only textual and JSON response types are supported."
                        );
                    }

                    Object body = mapBody(bodyBytes, contentType);
                    if (body instanceof HttpGatewayError error) {
                        return error;
                    }

                    return new HttpGetResponse(
                        response.getStatusCode().value(),
                        extractHeaders(responseHeaders),
                        body
                    );
                });
        } catch (ResourceAccessException e) {
            return new HttpGatewayError(
                NETWORK_ERROR,
                "Failed to reach the upstream server."
            );
        } catch (RestClientException e) {
            return new HttpGatewayError(
                NETWORK_ERROR,
                "HTTP GET failed before a response was received."
            );
        } catch (Exception e) {
            return new HttpGatewayError(
                NETWORK_ERROR,
                "Unexpected gateway error while processing the response."
            );
        }
    }

    private URI validateUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || scheme == null) {
                return null;
            }
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void applyHeaders(HttpHeaders outgoingHeaders, Map<String, String> inputHeaders) {
        if (inputHeaders == null || inputHeaders.isEmpty()) {
            return;
        }

        inputHeaders.forEach((name, value) -> {
            if (name == null || value == null) {
                return;
            }
            String normalized = name.toLowerCase(Locale.ROOT);
            if ("authorization".equals(normalized)
                || "cookie".equals(normalized)
                || "host".equals(normalized)) {
                return;
            }
            outgoingHeaders.set(name, value);
        });
    }

    private boolean isSupportedContentType(MediaType contentType) {
        if (contentType == null) {
            return true;
        }
        return isJsonLike(contentType) || "text".equalsIgnoreCase(contentType.getType());
    }

    private Object mapBody(byte[] bodyBytes, MediaType contentType) {
        String bodyText = new String(bodyBytes, resolveCharset(contentType));

        if (contentType == null || !isJsonLike(contentType)) {
            return bodyText;
        }

        if (bodyText.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(bodyText, Object.class);
        } catch (JsonProcessingException e) {
            return new HttpGatewayError(
                INVALID_JSON_RESPONSE,
                "Upstream declared JSON but returned an unreadable body."
            );
        }
    }

    private Charset resolveCharset(MediaType contentType) {
        return contentType != null && contentType.getCharset() != null
            ? contentType.getCharset()
            : StandardCharsets.UTF_8;
    }

    private boolean isJsonLike(MediaType contentType) {
        return MediaType.APPLICATION_JSON.includes(contentType)
            || contentType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+json");
    }

    private Map<String, String> extractHeaders(HttpHeaders headers) {
        Map<String, String> result = new LinkedHashMap<>();
        copyHeader(headers, result, HttpHeaders.CONTENT_TYPE);
        copyHeader(headers, result, HttpHeaders.LOCATION);
        copyHeader(headers, result, HttpHeaders.WWW_AUTHENTICATE);
        return result;
    }

    private void copyHeader(HttpHeaders source, Map<String, String> target, String name) {
        String value = source.getFirst(name);
        if (value != null) {
            target.put(name.toLowerCase(Locale.ROOT), value);
        }
    }
}
