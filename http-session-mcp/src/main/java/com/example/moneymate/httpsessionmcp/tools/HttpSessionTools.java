package com.example.moneymate.httpsessionmcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpSessionTools {

    @McpTool(
        name = "http_get",
        description = "Perform an authenticated HTTP GET to an absolute URL. Phase 1 returns a placeholder response."
    )
    public HttpGetResponse httpGet(
        @McpToolParam(
            description = "Absolute http or https URL to fetch.",
            required = true
        ) String url,
        @McpToolParam(
            description = "Optional safe request headers such as Accept.",
            required = false
        ) Map<String, String> headers
    ) {
        return HttpGetResponse.notImplemented();
    }
}
