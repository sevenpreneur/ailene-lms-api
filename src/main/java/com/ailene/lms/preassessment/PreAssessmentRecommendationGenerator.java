package com.ailene.lms.preassessment;

import com.ailene.lms.common.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PreAssessmentRecommendationGenerator {

    private static final String MODEL = "gpt-5-mini";
    private static final String SCHEMA_NAME = "pre_assessment_recommendations";

    private static final String SYSTEM_PROMPT = """
            Kamu adalah AI learning advisor untuk program adopsi AI di lingkungan kerja.
            Tugasmu: dari hasil pre-assessment seorang karyawan, susun 3-5 rekomendasi use case AI yang paling berdampak untuk pekerjaan sehari-harinya.
            Aturan:
            - Bahasa Indonesia, ringkas, dan langsung bisa ditindaklanjuti.
            - Setiap rekomendasi harus berakar pada tugas/rutinitas nyata yang user sebutkan (field 'source').
            - Prioritaskan use case yang menutup pilar terlemah dan sesuai use case yang user minati.
            - Field 'lessons' WAJIB diambil persis dari daftar chapter yang tersedia, jangan mengarang nama lain.
            - 'time_saved_label' adalah perkiraan realistis total waktu hemat per minggu dari seluruh rekomendasi.""";

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    public Map<String, Object> generate(PreAssessment pa, List<String> availableLessons) {
        PreAssessmentReportSummary report = PreAssessmentReportBuilder.build(pa);

        String pillarLines = report.pillars().stream()
                .map(p -> "- " + p.label() + ": " + p.score() + "/5")
                .collect(Collectors.joining("\n"));

        boolean hasConcreteExample = pa.getConcreteExample() != null && !pa.getConcreteExample().isBlank();
        String userContext = String.join("\n", "Skor pilar (skala 1-5):\n" + pillarLines,
                "Rata-rata: " + report.avg() + "/5", "Pilar terkuat: " + report.strongest().label(),
                "Pilar terlemah: " + report.weakest().label(),
                "Use case yang diminati: " + joinOrDash(pa.getUseCases()),
                "Contoh penggunaan konkret: " + (hasConcreteExample ? pa.getConcreteExample().trim() : "-"),
                "Tantangan terbesar: " + orDash(pa.getBiggestChallenge()),
                "Ekspektasi pelatihan: " + orDash(pa.getTrainingExpectation()));

        String lessonList = availableLessons.stream().map(l -> "- " + l).collect(Collectors.joining("\n"));

        String userPrompt = String.join("\n",
                "Daftar chapter kurikulum yang tersedia (pilih nama persis dari sini untuk 'lessons'):", lessonList,
                "", "Profil pre-assessment user:", userContext);

        String content = openAiClient.createStructuredCompletion(MODEL, SYSTEM_PROMPT, userPrompt, SCHEMA_NAME,
                buildJsonSchema());

        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException("OpenAI returned an unparseable recommendations response.", e);
        }

        Object items = parsed.get("items");
        if (!(items instanceof List<?> itemList) || itemList.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no recommendation items.");
        }

        return parsed;
    }

    private static String joinOrDash(String[] values) {
        return values == null || values.length == 0 ? "-" : String.join(", ", values);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static Map<String, Object> buildJsonSchema() {
        Map<String, Object> itemProperties = Map.of("source", Map.of("type", "string"), "title",
                Map.of("type", "string"), "impact", Map.of("type", "string", "enum",
                        List.of("Tinggi", "Sedang", "Rendah")),
                "speed", Map.of("type", "string"), "description", Map.of("type", "string"), "lessons",
                Map.of("type", "array", "items", Map.of("type", "string")));

        Map<String, Object> itemSchema = Map.of("type", "object", "properties", itemProperties, "required",
                List.of("source", "title", "impact", "speed", "description", "lessons"), "additionalProperties",
                false);

        Map<String, Object> properties = Map.of("time_saved_label", Map.of("type", "string"), "items",
                Map.of("type", "array", "items", itemSchema));

        return Map.of("type", "object", "properties", properties, "required",
                List.of("time_saved_label", "items"), "additionalProperties", false);
    }
}
