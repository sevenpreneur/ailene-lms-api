package com.ailene.lms;

import com.ailene.lms.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// A body Jackson cannot bind is the client's mistake, so it must read as 400 and name the field.
@SpringBootTest
class MalformedBodyTest {

    // LmsApplication's own static block never runs here -- @SpringBootTest only reads its bytecode metadata.
    static {
        LmsApplication.loadDotenv();
    }

    private static final String PROJECT_ID = "V7rdgcYkq9PHQZkwvoA-F";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private AuthService authService;

    private MockMvc mockMvc;

    // Built by hand: Spring Boot 4 no longer ships @AutoConfigureMockMvc in starter-test.
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        when(authService.resolveUserId(anyString())).thenReturn(UUID.randomUUID());
    }

    @Test
    void uppercaseEnumValueIsRejectedAsBadRequestNamingTheField() throws Exception {
        mockMvc.perform(post("/api/v1/use-cases/self-create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(useCaseBody("\"DAILY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "frequency: 'DAILY' is not one of [daily, weekly, monthly, occasionally]"));
    }

    @Test
    void lowercaseEnumValueGetsPastTheBindingLayer() throws Exception {
        mockMvc.perform(post("/api/v1/use-cases/self-create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(useCaseBody("\"daily\"")))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code == 400 && result.getResponse().getContentAsString().contains("is not one of")) {
                        throw new AssertionError("lowercase value was rejected by the enum binder");
                    }
                });
    }

    @Test
    void wrongJsonTypeIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/pre-assessment/submit")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"form_id\":1,\"answers\":\"not-a-list\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("answers: wrong type or missing"));
    }

    @Test
    void brokenJsonIsRejectedWithoutLeakingInternals() throws Exception {
        mockMvc.perform(post("/api/v1/pre-assessment/submit")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    private static String useCaseBody(String frequency) {
        return """
                {
                  "project_id": "%s",
                  "name": "Rekap laporan mingguan",
                  "category_ids": [1],
                  "outcome_proof": "https://example.com/bukti",
                  "hours_with_ai": 1,
                  "hours_without_ai": 3,
                  "description": "Meringkas laporan penjualan mingguan",
                  "ai_tool": "ChatGPT",
                  "frequency": %s,
                  "type": "data_analysis"
                }
                """.formatted(PROJECT_ID, frequency);
    }
}
