package com.ailene.lms.champion;

import com.ailene.lms.category.Category;
import com.ailene.lms.category.CategoryRepository;
import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChampionDraftServiceTest {

    private final ChampionAccessGuard championAccessGuard = mock(ChampionAccessGuard.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
    private final ChampionDraftService service = new ChampionDraftService(championAccessGuard, categoryRepository,
            deepSeekClient, new ObjectMapper());

    @BeforeEach
    void setUp() {
        when(championAccessGuard.requireChampion("jwt", "p1")).thenReturn(new ChampionContext("a1", "p1", 1, "HR"));
        when(categoryRepository.findAllByOrderByNameAsc()).thenReturn(List.of(category(87, "HR"), category(3, "Ops")));
    }

    @Test
    void generate_promptDraft_keepsOnlyKnownCategories() {
        modelReturns("""
                {"kind":"PROMPT","name":" Draft JD ","description":"Anda adalah HR","expected_output":"JD lengkap",
                 "category_ids":[999, "87", 87, 3, 3]}""");

        GenerateAssignmentResponse draft = service.generate("jwt", request(null));

        assertThat(draft.kind()).isEqualTo(AssignmentKind.PROMPT);
        assertThat(draft.name()).isEqualTo("Draft JD");
        assertThat(draft.expectedOutput()).isEqualTo("JD lengkap");
        assertThat(draft.categoryIds()).containsExactly((short) 87, (short) 3);
    }

    @Test
    void generate_requestedUseCase_overridesModelKindAndDropsExpectedOutput() {
        modelReturns("""
                {"kind":"PROMPT","name":"Rekap","description":"Tugas rutin","expected_output":"x","category_ids":[1]}""");

        GenerateAssignmentResponse draft = service.generate("jwt", request(AssignmentKind.USE_CASE));

        assertThat(draft.kind()).isEqualTo(AssignmentKind.USE_CASE);
        assertThat(draft.expectedOutput()).isNull();
        assertThat(draft.categoryIds()).isEmpty();
    }

    @Test
    void generate_promptWithoutExpectedOutput_isBadGateway() {
        modelReturns("""
                {"kind":"PROMPT","name":"Draft","description":"Skenario","expected_output":null,"category_ids":[87]}""");

        assertThatThrownBy(() -> service.generate("jwt", request(null))).isInstanceOf(BadGatewayException.class);
    }

    @Test
    void generate_unparsableJson_isBadGateway() {
        modelReturns("{\"kind\": \"PROMPT\", \"name\": ");

        assertThatThrownBy(() -> service.generate("jwt", request(null))).isInstanceOf(BadGatewayException.class);
    }

    private void modelReturns(String content) {
        when(deepSeekClient.createJsonCompletion(anyString(), anyString())).thenReturn(content);
    }

    private static GenerateAssignmentRequest request(AssignmentKind kind) {
        return new GenerateAssignmentRequest("p1", "bikin latihan prompt untuk tim HR", kind);
    }

    private static Category category(int id, String name) {
        Category category = new Category();
        category.setId((short) id);
        category.setName(name);
        return category;
    }
}
