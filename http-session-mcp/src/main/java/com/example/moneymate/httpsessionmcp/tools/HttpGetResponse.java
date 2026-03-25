package com.example.moneymate.httpsessionmcp.tools;

import java.util.Map;

public record HttpGetResponse(
    int status,
    Map<String, String> headers,
    Object body
) {
    public static HttpGetResponse notImplemented() {
        return new HttpGetResponse(
            501,
            Map.of("content-type", "text/plain"),
            "Phase 1 scaffold only. http_get is not implemented yet."
        );
    }
}
