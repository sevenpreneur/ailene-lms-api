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
        mockMvc.perform(post("/api/v1/pre-assessment/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("\"NEVER\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(
                        "ai_use_frequency: 'NEVER' is not one of [never, tried, weekly, daily, intensive]"));
    }

    @Test
    void lowercaseEnumValueGetsPastTheBindingLayer() throws Exception {
        mockMvc.perform(post("/api/v1/pre-assessment/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("\"never\"")))
                .andExpect(result -> {
                    int code = result.getResponse().getStatus();
                    if (code == 400 && result.getResponse().getContentAsString().contains("is not one of")) {
                        throw new AssertionError("lowercase value was rejected by the enum binder");
                    }
                });
    }

    @Test
    void wrongJsonTypeIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/pre-assessment/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"ai_tools_used\":\"not-a-list\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ai_tools_used: wrong type or missing"));
    }

    @Test
    void brokenJsonIsRejectedWithoutLeakingInternals() throws Exception {
        mockMvc.perform(post("/api/v1/pre-assessment/create")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    private static String body(String aiUseFrequency) {
        return """
                {
                  "project_id": "%s",
                  "ai_use_frequency": %s,
                  "ai_tools_used": ["chatgpt"],
                  "ai_limitations": ["hallucination"],
                  "output_review": "no_check",
                  "use_cases": ["research"],
                  "team_adoption": "none",
                  "concrete_example": "example",
                  "model_selection": "never",
                  "multimodal_use": "never",
                  "workflow_reuse": "never",
                  "prompt_comfort": "none",
                  "prompt_iteration": "never",
                  "refine_scenario": "targeted",
                  "professional_attitude": "neutral",
                  "data_safety_check": "never",
                  "publish_unchecked": "never",
                  "biggest_challenge": "challenge",
                  "training_expectation": "expectation",
                  "motivation": "curious"
                }
                """.formatted(PROJECT_ID, aiUseFrequency);
    }
}
