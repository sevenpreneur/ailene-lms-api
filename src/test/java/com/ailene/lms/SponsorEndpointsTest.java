package com.ailene.lms;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.AccessRole;
import com.ailene.lms.auth.AuthService;
import com.jayway.jsonpath.JsonPath;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Hits the real Neon database end to end -- only the JWT lookup and the sponsor access row are stubbed.
@SpringBootTest
class SponsorEndpointsTest {

    // LmsApplication's own static block never runs here -- @SpringBootTest only reads its bytecode metadata.
    static {
        LmsApplication.loadDotenv();
    }

    private static final String PROJECT_ID = "V7rdgcYkq9PHQZkwvoA-F";
    private static final String ACCESS_ID = "-2xph_GzlulJA0SlZy1GP";
    private static final UUID USER_ID = UUID.fromString("1df9f6b8-0911-4b65-acb6-3dc797bbe8e9");

    private static final String[] PROJECT_SCOPED_PATHS = {
            "/api/v1/sponsor/organization-stats",
            "/api/v1/sponsor/executive-view",
            "/api/v1/sponsor/headline",
            "/api/v1/sponsor/program-health",
            "/api/v1/sponsor/recent-activity",
            "/api/v1/sponsor/weekly-trends",
            "/api/v1/sponsor/proficiency-trends",
            "/api/v1/sponsor/level-distribution",
            "/api/v1/sponsor/workforce-members",
            "/api/v1/sponsor/organization-leaderboard",
            "/api/v1/sponsor/pre-assessment-organization",
            "/api/v1/sponsor/groups/departments",
            "/api/v1/sponsor/outcome/overview",
            "/api/v1/sponsor/outcome/level-distribution",
            "/api/v1/sponsor/outcome/roi-trend",
            "/api/v1/sponsor/outcome/department-roi",
            "/api/v1/sponsor/outcome/top-performers" };

    private static final String[] GROUP_SCOPED_PATHS = {
            "/api/v1/sponsor/groups/overview",
            "/api/v1/sponsor/groups/level-distribution",
            "/api/v1/sponsor/groups/top-use-cases",
            "/api/v1/sponsor/groups/attention-members" };

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
    void everySponsorEndpointReturnsDataFromTheDatabase() throws Exception {
        stubAccess(AccessRole.sponsor);

        for (String path : PROJECT_SCOPED_PATHS) {
            print(path, call(path, "{\"project_id\":\"" + PROJECT_ID + "\"}"));
        }
        int groupId = firstGroupId();
        for (String path : GROUP_SCOPED_PATHS) {
            print(path, call(path, "{\"project_id\":\"" + PROJECT_ID + "\",\"group_id\":" + groupId + "}"));
        }
    }

    @Test
    void organizationStatsReadsRealRowsFromTheDatabase() throws Exception {
        stubAccess(AccessRole.sponsor);

        mockMvc.perform(post("/api/v1/sponsor/organization-stats")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member_count").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.group_count").value(greaterThan(0)));
    }

    @Test
    void groupOverviewResolvesTheGroupAndItsChampion() throws Exception {
        stubAccess(AccessRole.sponsor);

        mockMvc.perform(post("/api/v1/sponsor/groups/overview")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"group_id\":" + firstGroupId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.group.name").isNotEmpty())
                .andExpect(jsonPath("$.data.metrics.total_members").value(greaterThan(0)));
    }

    @Test
    void nonSponsorAccessIsRejected() throws Exception {
        stubAccess(AccessRole.champion);

        mockMvc.perform(post("/api/v1/sponsor/headline")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only sponsors can access this resource"));
    }

    @Test
    void unknownGroupIsRejected() throws Exception {
        stubAccess(AccessRole.sponsor);

        mockMvc.perform(post("/api/v1/sponsor/groups/overview")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"project_id\":\"" + PROJECT_ID + "\",\"group_id\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Group not found in this project"));
    }

    @Test
    void missingProjectIdIsRejected() throws Exception {
        stubAccess(AccessRole.sponsor);

        mockMvc.perform(post("/api/v1/sponsor/headline")
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // Resolved from the API itself so the suite doesn't pin itself to a seeded group id.
    private int firstGroupId() throws Exception {
        String body = call("/api/v1/sponsor/groups/departments", "{\"project_id\":\"" + PROJECT_ID + "\"}")
                .getResponse().getContentAsString();
        // A freshly created, still-empty group can sort first, so pick one that actually has members.
        List<Integer> populated = JsonPath.read(body, "$.data.departments[?(@.member_count > 0)].id");
        return populated.get(0);
    }

    private void stubAccess(AccessRole role) {
        Access access = new Access();
        access.setId(ACCESS_ID);
        access.setProjectId(PROJECT_ID);
        access.setUserId(USER_ID);
        access.setRole(role);
        when(authService.resolveUserId(anyString())).thenReturn(USER_ID);
        when(accessRepository.findByUserIdAndProjectId(any(), anyString())).thenReturn(Optional.of(access));
    }

    private MvcResult call(String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer test-jwt")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andReturn();
    }

    private void print(String path, MvcResult result) throws Exception {
        System.out.println("[SPONSOR] POST " + path + " -> " + result.getResponse().getContentAsString());
    }
}
