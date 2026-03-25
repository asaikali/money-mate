package com.example.moneymate.httpsessionmcp.tools;

import java.util.Map;

public record HttpResponse(
    int status,
    Map<String, String> headers,
    Object body
) {}
