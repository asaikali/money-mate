package com.example.moneymate.httpsessionmcp;

import com.example.moneymate.httpsessionmcp.tools.HttpGetResponse;
import com.example.moneymate.httpsessionmcp.tools.HttpSessionTools;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class HttpSessionMcpApplicationTests {

    @Autowired
    private HttpSessionTools httpSessionTools;

    @Test
    void contextLoads() {
        assertThat(httpSessionTools).isNotNull();
    }

    @Test
    void httpGetReturnsPlaceholderResponse() {
        HttpGetResponse response = httpSessionTools.httpGet(
            "http://localhost:8080/api/test",
            Map.of("Accept", "application/json")
        );

        assertThat(response.status()).isEqualTo(501);
        assertThat(response.headers()).containsEntry("content-type", "text/plain");
        assertThat(response.body()).isEqualTo("Phase 1 scaffold only. http_get is not implemented yet.");
    }
}
