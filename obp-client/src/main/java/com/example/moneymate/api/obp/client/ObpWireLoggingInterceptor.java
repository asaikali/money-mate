package com.example.moneymate.api.obp.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Logs the raw DirectLogin HTTP exchange at DEBUG level while redacting credentials and tokens.
 * Other OBP requests are deliberately excluded because their bodies can contain banking data.
 */
final class ObpWireLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ObpWireLoggingInterceptor.class);
    private static final String DIRECT_LOGIN_PATH = "/my/logins/direct";
    private static final Pattern DIRECT_LOGIN_PASSWORD = Pattern.compile(
        "(?i)(password\\s*=\\s*)(?:\"[^\"]*\"|[^,\\s]+)"
    );
    private static final Pattern DIRECT_LOGIN_CONSUMER_KEY = Pattern.compile(
        "(?i)(consumer_key\\s*=\\s*)(?:\"[^\"]*\"|[^,\\s]+)"
    );
    private static final Pattern DIRECT_LOGIN_TOKEN = Pattern.compile(
        "(?i)(token\\s*=\\s*)(?:\"[^\"]*\"|[^,\\s]+)"
    );
    private static final Pattern JSON_SECRET = Pattern.compile(
        "(?i)(\"(?:password|consumer_key|token|access_token|refresh_token)\"\\s*:\\s*\")[^\"]*(\")"
    );

    @Override
    public ClientHttpResponse intercept(
        HttpRequest request,
        byte[] body,
        ClientHttpRequestExecution execution
    ) throws IOException {
        if (!log.isDebugEnabled() || !isDirectLogin(request.getURI())) {
            return execution.execute(request, body);
        }

        log.debug(
            "OBP wire request\n> {} {}\n{}\n\n{}",
            request.getMethod(),
            request.getURI(),
            formatHeaders(request.getHeaders(), ">"),
            redactBody(decode(body, request.getHeaders()))
        );

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException exception) {
            log.debug("OBP wire response\n< I/O failure: {}", exception.getMessage());
            throw exception;
        }

        byte[] responseBody = response.getBody().readAllBytes();
        log.debug(
            "OBP wire response\n< HTTP {} {}\n{}\n\n{}",
            response.getStatusCode().value(),
            response.getStatusText(),
            formatHeaders(response.getHeaders(), "<"),
            redactBody(decode(responseBody, response.getHeaders()))
        );

        return new ReplayableClientHttpResponse(response, responseBody);
    }

    static boolean isDirectLogin(URI uri) {
        return DIRECT_LOGIN_PATH.equals(uri.getPath());
    }

    static String redactHeaderValue(String headerName, String value) {
        String normalizedName = headerName.toLowerCase(Locale.ROOT);
        if (HttpHeaders.COOKIE.equalsIgnoreCase(headerName)
            || HttpHeaders.SET_COOKIE.equalsIgnoreCase(headerName)) {
            return "<redacted>";
        }
        if (!HttpHeaders.AUTHORIZATION.equalsIgnoreCase(headerName)) {
            return value;
        }
        if (value.regionMatches(true, 0, "DirectLogin", 0, "DirectLogin".length())) {
            String redacted = DIRECT_LOGIN_PASSWORD.matcher(value).replaceAll("$1<redacted>");
            redacted = DIRECT_LOGIN_CONSUMER_KEY.matcher(redacted).replaceAll("$1<redacted>");
            return DIRECT_LOGIN_TOKEN.matcher(redacted).replaceAll("$1<redacted>");
        }
        if (normalizedName.equals(HttpHeaders.AUTHORIZATION.toLowerCase(Locale.ROOT))) {
            return "<redacted>";
        }
        return value;
    }

    static String redactBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        return JSON_SECRET.matcher(body).replaceAll("$1<redacted>$2");
    }

    private static String formatHeaders(HttpHeaders headers, String prefix) {
        StringBuilder formatted = new StringBuilder();
        headers.forEach((name, values) -> {
            List<String> redactedValues = values.stream()
                .map(value -> redactHeaderValue(name, value))
                .toList();
            if (!formatted.isEmpty()) {
                formatted.append('\n');
            }
            formatted.append(prefix)
                .append(' ')
                .append(name)
                .append(": ")
                .append(String.join(", ", redactedValues));
        });
        return formatted.toString();
    }

    private static String decode(byte[] bytes, HttpHeaders headers) {
        if (bytes.length == 0) {
            return "";
        }
        MediaType contentType = headers.getContentType();
        Charset charset = contentType != null && contentType.getCharset() != null
            ? contentType.getCharset()
            : StandardCharsets.UTF_8;
        return new String(bytes, charset);
    }

    private static final class ReplayableClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] body;

        private ReplayableClientHttpResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override
        public org.springframework.http.HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
