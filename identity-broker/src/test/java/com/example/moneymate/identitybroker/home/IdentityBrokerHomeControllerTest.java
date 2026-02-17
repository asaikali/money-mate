package com.example.moneymate.identitybroker.home;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Identity Broker Home")
class IdentityBrokerHomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / redirects to login when unauthenticated")
    void rootRedirectsToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", "http://localhost/login"));
    }

    @Test
    @DisplayName("GET / renders Identity broker page with user details")
    void rootShowsAuthenticatedUserDetails() throws Exception {
        mockMvc.perform(get("/").with(user("demo").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>Identity broker</title>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<h1>Identity broker</h1>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Username")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("demo")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("ROLE_USER")));
    }
}
