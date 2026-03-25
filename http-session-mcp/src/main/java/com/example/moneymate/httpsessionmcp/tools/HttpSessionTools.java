package com.example.moneymate.httpsessionmcp.tools;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpSessionTools {

    private final HttpRequestService httpRequestService;

    public HttpSessionTools(HttpRequestService httpRequestService) {
        this.httpRequestService = httpRequestService;
    }

    @McpTool(
        name = "http_get",
        description = "Perform an HTTP GET to an absolute URL and return a compact response payload."
    )
    public Object httpGet(
        @McpToolParam(
            description = "Absolute http or https URL to fetch.",
            required = true
        ) String url,
        @McpToolParam(
            description = "Optional safe request headers such as Accept.",
            required = false
        ) Map<String, String> headers
    ) {
        return httpRequestService.get(url, headers);
    }

    @McpTool(
        name = "http_post",
        description = "Perform an HTTP POST to an absolute URL and return a compact response payload."
    )
    public Object httpPost(
        @McpToolParam(
            description = "Absolute http or https URL to post to.",
            required = true
        ) String url,
        @McpToolParam(
            description = "Optional safe request headers such as Accept or Content-Type.",
            required = false
        ) Map<String, String> headers,
        @McpToolParam(
            description = "Request body as either a JSON object/array or a text string.",
            required = false
        ) Object body
    ) {
        return httpRequestService.post(url, headers, body);
    }
}
