package com.ailene.lms;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hits the real Neon database end to end -- only the JWT lookup and the champion access row are stubbed.
@SpringBootTest
class ChampionEndpointsTest {

    // LmsApplication's own static block never runs here -- @SpringBootTest only reads its bytecode metadata.
    static {
        LmsApplication.loadDotenv();
    }

    private static final String PROJECT_ID = "V7rdgcYkq9PHQZkwvoA-F";
    private static final String ACCESS_ID = "-2xph_GzlulJA0SlZy1GP";
    private static final int GROUP_ID = 1;
    private static final UUID USER_ID = UUID.fromString("1df9f6b8-0911-4b65-acb6-3dc797bbe8e9");

    private static final String[] READ_PATHS = {
            "/api/v1/champion/members",
            "/api/v1/champion/pre-assessment-team",
            "/api/v1/champion/report",
            "/api/v1/champion/prompts/submissions",
            "/api/v1/champion/use-cases/submissions" };

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private AccessRepository accessRepository;

    private MockMvc mockMvc;

    // Built by hand: Spring Boot 4 no longer ships @AutoConfigureMockMvc in starter-test.
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void everyChampionReadEndpointReturnsDataFromTheDatabase() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        for (String path : READ_PATHS) {
            MvcResult result = mockMvc.perform(post(path)
                    .header("Authorization", "Bearer test-jwt")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"project_id\":\"" + PROJECT_ID + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn();
            System.out.println("[CHAMPION] POST " + path + " -> " + result.getResponse().getContentAsString());
        }
    }

    @Test
    void monthlyReportUsesTheRequestedPeriod() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/report")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"period\":\"monthly\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.period").value("monthly"))
                .andExpect(jsonPath("$.data.sent_reports").isArray());
    }

    @Test
    void nonChampionAccessIsRejected() throws Exception {
        stubAccess(AccessRole.student, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/members")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only champions can access this resource"));
    }

    @Test
    void championWithoutAGroupIsRejected() throws Exception {
        stubAccess(AccessRole.champion, null);

        mockMvc.perform(post("/api/v1/champion/members")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You don't lead a group in this project yet"));
    }

    @Test
    void memberOutsideTheLedGroupIsRejected() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/member-details")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"member_access_id\":\"not-a-real-access\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignmentRejectsAPastDeadline() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/prompts/assign")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"project_id":"%s","library_id":1,"target_type":"MEMBER",
                         "target_access_ids":["%s"],"deadline":"2020-01-01T00:00:00Z"}
                        """.formatted(PROJECT_ID, ACCESS_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Deadline must be in the future"));
    }

    @Test
    void reviewRejectsAnUnknownSubmission() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/prompts/review")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID
                        + "\",\"submission_id\":999999,\"is_accepted\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Submission not found"));
    }

    @Test
    void submissionDetailsRejectsAnUnknownSubmission() throws Exception {
        stubAccess(AccessRole.champion, GROUP_ID);

        mockMvc.perform(post("/api/v1/champion/use-cases/submission-details")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"submission_id\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Submission not found"));
    }

    private void stubAccess(AccessRole role, Integer groupId) {
        Access access = new Access();
        access.setId(ACCESS_ID);
        access.setProjectId(PROJECT_ID);
        access.setUserId(USER_ID);
        access.setRole(role);

        when(authService.resolveUserId(anyString())).thenReturn(USER_ID);
        when(accessRepository.findByUserIdAndProjectId(any(), anyString())).thenReturn(Optional.of(access));
        when(accessRepository.findGroupSummary(any(), anyString()))
                .thenReturn(Optional.of(groupSummary(groupId)));
    }

    private GroupSummaryProjection groupSummary(Integer groupId) {
        return new GroupSummaryProjection() {
            @Override
            public String getAccessId() {
                return ACCESS_ID;
            }

            @Override
            public Integer getGroupId() {
                return groupId;
            }

            @Override
            public String getGroupName() {
                return "Human Capital";
            }
        };
    }
}
