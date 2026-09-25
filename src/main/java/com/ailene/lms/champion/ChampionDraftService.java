package com.ailene.lms.champion;

import com.ailene.lms.category.Category;
import com.ailene.lms.category.CategoryRepository;
import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChampionDraftService {

    private static final Logger log = LoggerFactory.getLogger(ChampionDraftService.class);
    private static final int NAME_MAX_LENGTH = 255;
    private static final int MAX_CATEGORIES = 2;
    private static final String INVALID_DRAFT_MESSAGE = "The AI service returned an unusable draft, please try again";

    private static final String SYSTEM_PROMPT = """
            Kamu adalah perancang kurikulum untuk program adopsi AI di lingkungan kerja.
            Tugasmu: dari instruksi seorang champion (pembina tim), susun SATU draft item latihan untuk library.

            Ada dua jenis item:
            - PROMPT (Level 2): latihan menulis satu prompt. 'description' berisi skenario/peran dan konteks yang harus dijadikan prompt oleh peserta (mis. "Anda adalah HR Generalist di perusahaan ritel ..."), dan 'expected_output' menjelaskan hasil yang seharusnya dihasilkan AI dari prompt yang baik.
            - USE_CASE (Level 3): penerapan AI pada tugas atau alur kerja nyata yang berulang. 'description' menjelaskan tugasnya, cara AI dipakai, dan hasil yang diharapkan. 'expected_output' HARUS null.

            Aturan:
            - Gunakan Bahasa Indonesia yang profesional, ringkas, dan konkret.
            - 'name' berupa judul singkat (maks. 80 karakter), tanpa tanda kutip.
            - 'category_ids' berisi 1 atau 2 id yang diambil PERSIS dari daftar kategori yang diberikan.
            - Jika jenis sudah ditentukan, patuhi jenis itu. Jika belum, pilih jenis yang paling sesuai dengan instruksi.
            - Balas HANYA dengan satu objek json, tanpa teks lain, dengan bentuk persis seperti contoh berikut.

            Contoh json untuk PROMPT:
            {"kind": "PROMPT", "name": "Draft Job Description", "description": "Anda adalah HR Generalist di perusahaan distribusi. Tim sales membutuhkan Sales Executive baru ...", "expected_output": "Job description lengkap berisi ringkasan posisi, tanggung jawab, kualifikasi, dan benefit ...", "category_ids": [1]}

            Contoh json untuk USE_CASE:
            {"kind": "USE_CASE", "name": "Rekap Invoice Bulanan", "description": "Setiap akhir bulan tim finance merekap puluhan invoice secara manual. Gunakan AI untuk ...", "expected_output": null, "category_ids": [1, 2]}""";

    private final ChampionAccessGuard championAccessGuard;
    private final CategoryRepository categoryRepository;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public GenerateAssignmentResponse generate(String jwt, GenerateAssignmentRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        String categoryList = categories.stream()
                .map(c -> "- " + c.getId() + ": " + c.getName())
                .collect(Collectors.joining("\n"));

        String userPrompt = String.join("\n", "Daftar kategori (id: nama):", categoryList, "",
                "Tim champion: " + (champion.groupName() == null ? "-" : champion.groupName()),
                "Jenis item: " + (request.kind() == null ? "belum ditentukan, pilih PROMPT atau USE_CASE"
                        : request.kind().name()),
                "", "Instruksi champion:", request.instruction().trim());

        String content = deepSeekClient.createJsonCompletion(SYSTEM_PROMPT, userPrompt);
        return parseDraft(content, request.kind(), categories);
    }

    @SuppressWarnings("unchecked")
    private GenerateAssignmentResponse parseDraft(String content, AssignmentKind requestedKind,
            List<Category> categories) {
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(content, Map.class);
        } catch (JacksonException e) {
            log.error("DeepSeek draft is not valid JSON: {}", content);
            throw new BadGatewayException(INVALID_DRAFT_MESSAGE);
        }

        AssignmentKind kind = requestedKind != null ? requestedKind : parseKind(parsed.get("kind"));
        String name = text(parsed.get("name"));
        String description = text(parsed.get("description"));
        String expectedOutput = kind == AssignmentKind.PROMPT ? text(parsed.get("expected_output")) : null;

        if (kind == null || name == null || description == null
                || (kind == AssignmentKind.PROMPT && expectedOutput == null)) {
            log.error("DeepSeek draft is missing required fields: {}", content);
            throw new BadGatewayException(INVALID_DRAFT_MESSAGE);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            name = name.substring(0, NAME_MAX_LENGTH).trim();
        }

        Set<Short> validIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
        List<Short> categoryIds = new ArrayList<>();
        if (parsed.get("category_ids") instanceof List<?> rawIds) {
            for (Object rawId : rawIds) {
                Short id = toShort(rawId);
                if (id != null && validIds.contains(id) && !categoryIds.contains(id)
                        && categoryIds.size() < MAX_CATEGORIES) {
                    categoryIds.add(id);
                }
            }
        }

        return new GenerateAssignmentResponse(kind, name, description, expectedOutput, categoryIds);
    }

    private static AssignmentKind parseKind(Object raw) {
        String value = text(raw);
        if (value == null) {
            return null;
        }
        try {
            return AssignmentKind.valueOf(value.toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String text(Object raw) {
        if (!(raw instanceof String value) || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Short toShort(Object raw) {
        try {
            if (raw instanceof Number number) {
                return number.shortValue() == number.doubleValue() ? number.shortValue() : null;
            }
            if (raw instanceof String value) {
                return Short.valueOf(value.trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }
}
