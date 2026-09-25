package com.ailene.lms.champion;

import com.ailene.lms.category.Category;
import com.ailene.lms.category.CategoryRepository;
import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChampionDraftServiceTest {

    private final ChampionAccessGuard championAccessGuard = mock(ChampionAccessGuard.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final AssignmentDraftRepository draftRepository = mock(AssignmentDraftRepository.class);
    private final DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
    private final ChampionDraftService service = new ChampionDraftService(championAccessGuard, categoryRepository,
            draftRepository, deepSeekClient, new ObjectMapper());
    private final List<AssignmentDraft> saved = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(championAccessGuard.requireChampion("jwt", "p1")).thenReturn(new ChampionContext("a1", "p1", 1, "HR"));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category(87, "HR"), category(3, "Ops")));
        when(draftRepository.saveAll(anyList())).thenAnswer(inv -> {
            saved.addAll(inv.getArgument(0, List.class));
            return saved;
        });
        when(draftRepository.findByChampionAccessIdAndBatchIdInOrderByIdAsc(eq("a1"), anyList()))
                .thenAnswer(inv -> saved);
    }

    @Test
    void generate_savesUsableVariantsUnderOneBatch() {
        modelReturns("""
                {"drafts":[
                 {"kind":"PROMPT","angle":"Untuk pemula","name":" Draft JD ","description":"Anda adalah HR",
                  "expected_output":"JD lengkap","category_ids":[999, "87", 87, 3, 3]},
                 {"kind":"PROMPT","angle":"Rusak","name":"Tanpa output","description":"x","expected_output":null},
                 {"kind":"USE_CASE","name":"Rekap","description":"Tugas rutin","expected_output":"dibuang"}
                ]}""");

        AssignmentDraftBatch batch = service.generate("jwt", request(null, 3));

        assertThat(batch.drafts()).hasSize(2);
        assertThat(saved).extracting(AssignmentDraft::getBatchId).containsOnly(saved.get(0).getBatchId());
        assertThat(saved.get(0).getBatchId()).hasSize(21);
        assertThat(saved).extracting(AssignmentDraft::getChampionAccessId).containsOnly("a1");

        AssignmentDraftDto first = batch.drafts().get(0);
        assertThat(first.name()).isEqualTo("Draft JD");
        assertThat(first.categoryIds()).containsExactly((short) 87, (short) 3);

        AssignmentDraftDto second = batch.drafts().get(1);
        assertThat(second.kind()).isEqualTo(AssignmentKind.USE_CASE);
        assertThat(second.angle()).isEqualTo("Varian 2");
        assertThat(second.expectedOutput()).isNull();
        assertThat(second.categoryIds()).isEmpty();
    }

    @Test
    void generate_requestedKindAppliesToEveryVariantAndCapsAtCount() {
        modelReturns("""
                {"drafts":[
                 {"kind":"PROMPT","angle":"A","name":"Satu","description":"d","expected_output":"x"},
                 {"kind":"PROMPT","angle":"B","name":"Dua","description":"d","expected_output":"x"},
                 {"kind":"PROMPT","angle":"C","name":"Tiga","description":"d","expected_output":"x"}
                ]}""");

        AssignmentDraftBatch batch = service.generate("jwt", request(AssignmentKind.USE_CASE, 2));

        assertThat(batch.requestedKind()).isEqualTo(AssignmentKind.USE_CASE);
        assertThat(batch.drafts()).extracting(AssignmentDraftDto::kind).containsOnly(AssignmentKind.USE_CASE);
        assertThat(batch.drafts()).extracting(AssignmentDraftDto::expectedOutput).containsOnlyNulls();
        assertThat(batch.drafts()).extracting(AssignmentDraftDto::name).containsExactly("Satu", "Dua");
    }

    @Test
    void generate_countOmitted_letsModelChooseButCapsAtFive() {
        StringBuilder drafts = new StringBuilder();
        for (int i = 1; i <= 6; i++) {
            drafts.append(i > 1 ? "," : "").append("{\"kind\":\"USE_CASE\",\"angle\":\"A").append(i)
                    .append("\",\"name\":\"N").append(i).append("\",\"description\":\"d\"}");
        }
        modelReturns("{\"plan\":\"banyak sudut\",\"drafts\":[" + drafts + "]}");

        AssignmentDraftBatch batch = service.generate("jwt", request(null, null));

        assertThat(batch.drafts()).hasSize(5);
    }

    @Test
    void generate_noUsableVariant_isBadGatewayAndSavesNothing() {
        modelReturns("""
                {"drafts":[{"kind":"PROMPT","name":"Draft","description":"Skenario","expected_output":null}]}""");

        assertThatThrownBy(() -> service.generate("jwt", request(null, 3))).isInstanceOf(BadGatewayException.class);
        verify(draftRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_unparsableJson_isBadGateway() {
        modelReturns("{\"drafts\": [");

        assertThatThrownBy(() -> service.generate("jwt", request(null, 3))).isInstanceOf(BadGatewayException.class);
    }

    @Test
    void listBatches_keepsNewestFirstAndGroupsDrafts() {
        when(draftRepository.findRecentBatchIds("a1", 30)).thenReturn(List.of("new", "old"));
        when(draftRepository.findByChampionAccessIdAndBatchIdInOrderByIdAsc("a1", List.of("new", "old")))
                .thenReturn(List.of(draft(1, "old"), draft(2, "new"), draft(3, "old")));

        AssignmentDraftBatchesResponse response = service.listBatches("jwt", new ChampionProjectRequest("p1"));

        assertThat(response.batches()).extracting(AssignmentDraftBatch::batchId).containsExactly("new", "old");
        assertThat(response.batches().get(1).drafts()).extracting(AssignmentDraftDto::id).containsExactly(1, 3);
    }

    @Test
    void deleteDraft_someoneElsesDraft_isNotFound() {
        when(draftRepository.findByIdAndChampionAccessId(anyInt(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDraft("jwt", new DeleteDraftRequest("p1", 9)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void modelReturns(String content) {
        when(deepSeekClient.createJsonCompletion(anyString(), anyString(), anyInt(), anyDouble())).thenReturn(content);
    }

    private static GenerateAssignmentRequest request(AssignmentKind kind, Integer count) {
        return new GenerateAssignmentRequest("p1", "bikin latihan prompt untuk tim HR", kind, count);
    }

    private static AssignmentDraft draft(int id, String batchId) {
        AssignmentDraft draft = new AssignmentDraft();
        draft.setId(id);
        draft.setBatchId(batchId);
        draft.setKind(AssignmentKind.PROMPT);
        draft.setCategoryIds(new Short[0]);
        return draft;
    }

    private static Category category(int id, String name) {
        Category category = new Category();
        category.setId((short) id);
        category.setName(name);
        return category;
    }
}
