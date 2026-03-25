package com.example.moneymate.httpsessionmcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpSessionTools {

    private final HttpGetService httpGetService;

    public HttpSessionTools(HttpGetService httpGetService) {
        this.httpGetService = httpGetService;
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
        return httpGetService.get(url, headers);
    }
}
