package com.ailene.lms.student;

import com.ailene.lms.access.Access;
import com.ailene.lms.access.AccessRepository;
import com.ailene.lms.access.GroupSummaryProjection;
import com.ailene.lms.access.LeaderboardRowProjection;
import com.ailene.lms.access.LevelProgressProjection;
import com.ailene.lms.auth.AuthService;
import com.ailene.lms.prompt.PromptRepository;
import com.ailene.lms.quiz.QuizRepository;
import com.ailene.lms.usecase.UseCaseAchievementProjection;
import com.ailene.lms.usecase.UseCaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private AccessRepository accessRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private PromptRepository promptRepository;
    @Mock
    private UseCaseRepository useCaseRepository;

    @InjectMocks
    private StudentService studentService;

    @Test
    void leaderboard_noGroup_returnsEmptyLeaderboard() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        GroupSummaryProjection summary = mock(GroupSummaryProjection.class);
        lenient().when(summary.getAccessId()).thenReturn("access-1");
        when(summary.getGroupId()).thenReturn(null);
        when(accessRepository.findGroupSummary(userId, "proj-1")).thenReturn(Optional.of(summary));

        LeaderboardResponse response = studentService.getLeaderboard("jwt", new LeaderboardRequest("proj-1"));

        assertThat(response.group()).isNull();
        assertThat(response.total()).isEqualTo(0);
        assertThat(response.leaderboard()).isEmpty();
    }

    @Test
    void leaderboard_ranksByXpDescendingAndFlagsCaller() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        GroupSummaryProjection summary = mock(GroupSummaryProjection.class);
        when(summary.getAccessId()).thenReturn("access-2");
        when(summary.getGroupId()).thenReturn(1);
        when(summary.getGroupName()).thenReturn("Human Capital");
        when(accessRepository.findGroupSummary(userId, "proj-1")).thenReturn(Optional.of(summary));

        LeaderboardRowProjection row1 = mock(LeaderboardRowProjection.class);
        when(row1.getAccessId()).thenReturn("access-1");
        when(row1.getFullName()).thenReturn("Top Scorer");
        when(row1.getAvatar()).thenReturn(null);
        when(row1.getTotalXp()).thenReturn(500L);

        LeaderboardRowProjection row2 = mock(LeaderboardRowProjection.class);
        when(row2.getAccessId()).thenReturn("access-2");
        when(row2.getFullName()).thenReturn("Caller");
        when(row2.getAvatar()).thenReturn(null);
        when(row2.getTotalXp()).thenReturn(200L);

        when(accessRepository.findGroupLeaderboard("proj-1", 1)).thenReturn(List.of(row1, row2));

        LeaderboardResponse response = studentService.getLeaderboard("jwt", new LeaderboardRequest("proj-1"));

        assertThat(response.group().name()).isEqualTo("Human Capital");
        assertThat(response.total()).isEqualTo(2);
        assertThat(response.myRank()).isEqualTo(2);
        assertThat(response.leaderboard().get(0).rank()).isEqualTo(1);
        assertThat(response.leaderboard().get(0).isMe()).isFalse();
        assertThat(response.leaderboard().get(1).isMe()).isTrue();
    }

    @Test
    void levelProgress_sumsApprovedHoursAndDedupsTools() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        LevelProgressProjection progress = mock(LevelProgressProjection.class);
        when(progress.getAccessId()).thenReturn("access-1");
        when(progress.getXpCount()).thenReturn(120L);
        when(progress.getCurrentLevelNumber()).thenReturn((short) 1);
        when(progress.getCurrentLevelName()).thenReturn("Foundation");
        when(progress.getTasksRequired()).thenReturn(8L);
        when(progress.getTasksDone()).thenReturn(8L);
        when(accessRepository.findLevelProgress(userId, "proj-1")).thenReturn(Optional.of(progress));

        when(promptRepository.countApprovedSubmissions("proj-1", "access-1")).thenReturn(2L);

        UseCaseAchievementProjection uc1 = mock(UseCaseAchievementProjection.class);
        when(uc1.getHoursWithAi()).thenReturn(1.0);
        when(uc1.getHoursWithoutAi()).thenReturn(4.0);
        when(uc1.getAiTool()).thenReturn("ChatGPT, Claude");

        UseCaseAchievementProjection uc2 = mock(UseCaseAchievementProjection.class);
        when(uc2.getHoursWithAi()).thenReturn(2.0);
        when(uc2.getHoursWithoutAi()).thenReturn(3.5);
        when(uc2.getAiTool()).thenReturn("claude");

        when(useCaseRepository.findApprovedAchievements("proj-1", "access-1")).thenReturn(List.of(uc1, uc2));

        LevelProgressResponse response = studentService.getLevelProgress("jwt", new LevelProgressRequest("proj-1"));

        assertThat(response.tasksDone()).isEqualTo(8);
        assertThat(response.tasksRequired()).isEqualTo(8);
        assertThat(response.nextLevelUnlockable()).isTrue();
        assertThat(response.useCaseApprovedCount()).isEqualTo(2);
        assertThat(response.promptApprovedCount()).isEqualTo(2);
        assertThat(response.hoursSavedTotal()).isEqualTo(4.5);
        assertThat(response.toolsMastered()).containsExactly("ChatGPT", "Claude");
    }

    @Test
    void competency_noData_defaultsToBeginnerTier() {
        UUID userId = UUID.randomUUID();
        when(authService.resolveUserId("jwt")).thenReturn(userId);

        Access access = new Access();
        access.setId("access-1");
        when(accessRepository.findByUserIdAndProjectId(userId, "proj-1")).thenReturn(Optional.of(access));
        when(accessRepository.findStudentStatus(userId, "proj-1")).thenReturn(Optional.empty());

        when(quizRepository.findAverageBestScore("proj-1", "access-1")).thenReturn(null);
        when(promptRepository.findCompetencySubmissions("proj-1", "access-1")).thenReturn(List.of());
        when(useCaseRepository.findCompetencySubmissions("proj-1", "access-1")).thenReturn(List.of());
        when(accessRepository.countActiveDays(org.mockito.ArgumentMatchers.eq("proj-1"),
                org.mockito.ArgumentMatchers.eq("access-1"), org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        CompetencyProfileResponse response = studentService.getCompetencyProfile("jwt",
                new CompetencyRequest("proj-1"));

        assertThat(response.avg()).isEqualTo(0.0);
        assertThat(response.tierNumber()).isEqualTo(0);
        assertThat(response.tierName()).isEqualTo("Beginner");
        assertThat(response.dimensions()).hasSize(6);
        assertThat(response.nextTier().number()).isEqualTo(1);
        assertThat(response.nextTier().name()).isEqualTo("Explorer");
    }
}
