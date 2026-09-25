package com.ailene.lms.champion;

import com.ailene.lms.category.Category;
import com.ailene.lms.category.CategoryRepository;
import com.ailene.lms.common.NanoId;
import com.ailene.lms.common.deepseek.DeepSeekClient;
import com.ailene.lms.common.exception.BadGatewayException;
import com.ailene.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChampionDraftService {

    private static final Logger log = LoggerFactory.getLogger(ChampionDraftService.class);
    private static final int MAX_COUNT = 5;
    private static final double TEMPERATURE = 0.9;
    private static final int PLAN_TOKENS = 400;
    private static final int MAX_BATCHES = 30;
    private static final int NAME_MAX_LENGTH = 255;
    private static final int ANGLE_MAX_LENGTH = 100;
    private static final int MAX_CATEGORIES = 2;
    private static final int TOKENS_PER_DRAFT = 1000;
    private static final String INVALID_DRAFT_MESSAGE = "The AI service returned an unusable draft, please try again";

    private static final String SYSTEM_PROMPT = """
            Kamu adalah perancang kurikulum untuk program adopsi AI di lingkungan kerja.
            Tugasmu: dari instruksi seorang champion (pembina tim), susun draft item latihan untuk library. Champion tidak memilih jenis maupun jumlah; kamu yang memutuskan keduanya.

            Ada dua jenis item:
            - PROMPT (Level 2): latihan menulis satu prompt. 'description' berisi skenario/peran dan konteks yang harus dijadikan prompt oleh peserta (mis. "Anda adalah HR Generalist di perusahaan ritel ..."), dan 'expected_output' menjelaskan hasil yang seharusnya dihasilkan AI dari prompt yang baik.
            - USE_CASE (Level 3): penerapan AI pada tugas atau alur kerja nyata yang berulang. 'description' menjelaskan tugasnya, cara AI dipakai, dan hasil yang diharapkan. 'expected_output' HARUS null.

            Sebelum menulis draft, pikirkan dulu di field 'plan' (2-4 kalimat, tidak ditampilkan ke champion):
            1. Apa yang sebenarnya diminta instruksi ini, dan untuk siapa di tim.
            2. Jenis mana yang cocok: PROMPT bila intinya melatih menulis satu prompt, USE_CASE bila intinya menerapkan AI pada alur kerja nyata yang berulang. Boleh mencampur keduanya dalam satu batch bila instruksinya memang mencakup keduanya.
            3. Berapa varian yang benar-benar layak (1 sampai 5): instruksi yang sempit dan spesifik cukup 1-2 varian, instruksi yang luas boleh sampai 5. Jangan menambah varian hanya untuk memenuhi jumlah.
            4. Sudut pandang apa yang membedakan tiap varian.
            Baru setelah itu tulis 'drafts' sesuai rencana tersebut.

            Aturan varian:
            - Setiap varian harus BENAR-BENAR berbeda, bukan parafrase. Bedakan lewat tingkat kesulitan, skenario atau sub-peran di dalam departemen tim, atau bentuk output (mis. email, laporan, analisis).
            - 'angle' adalah label singkat (maks. 40 karakter) yang menjelaskan pembeda varian itu, mis. "Untuk pemula", "Studi kasus nyata", "Tantangan lanjutan".
            - Jika pesan user menyebut jenis atau jumlah varian yang WAJIB, patuhi itu dan abaikan keputusanmu sendiri untuk hal tersebut.

            Aturan umum:
            - Gunakan Bahasa Indonesia yang profesional, ringkas, dan konkret.
            - 'name' berupa judul singkat (maks. 80 karakter), tanpa tanda kutip.
            - 'category_ids' berisi 1 atau 2 id yang diambil PERSIS dari daftar kategori yang diberikan.
            - Balas HANYA dengan satu objek json berisi 'plan' lalu array 'drafts', tanpa teks lain, dengan bentuk persis seperti contoh berikut.

            Contoh json:
            {"plan": "Instruksi meminta latihan untuk tim HR menulis job description. Ini inti latihan menulis prompt, jadi PROMPT; satu USE_CASE untuk alur rekrutmen rutin juga relevan. Cukup 2 varian: satu dasar, satu alur kerja.",
             "drafts": [
              {"kind": "PROMPT", "angle": "Untuk pemula", "name": "Draft Job Description", "description": "Anda adalah HR Generalist di perusahaan distribusi. Tim sales membutuhkan Sales Executive baru ...", "expected_output": "Job description lengkap berisi ringkasan posisi, tanggung jawab, kualifikasi, dan benefit ...", "category_ids": [1]},
              {"kind": "USE_CASE", "angle": "Alur kerja rutin", "name": "Rekap Kandidat Mingguan", "description": "Setiap minggu tim HR merangkum puluhan CV pelamar secara manual. Gunakan AI untuk ...", "expected_output": null, "category_ids": [1, 2]}
            ]}""";

    private final ChampionAccessGuard championAccessGuard;
    private final CategoryRepository categoryRepository;
    private final AssignmentDraftRepository assignmentDraftRepository;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public AssignmentDraftBatch generate(String jwt, GenerateAssignmentRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        int count = request.count() == null ? MAX_COUNT : request.count();
        String instruction = request.instruction().trim();

        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        String categoryList = categories.stream()
                .map(c -> "- " + c.getId() + ": " + c.getName())
                .collect(Collectors.joining("\n"));

        String userPrompt = String.join("\n", "Daftar kategori (id: nama):", categoryList, "",
                "Tim champion: " + (champion.groupName() == null ? "-" : champion.groupName()),
                "Jenis item: " + (request.kind() == null ? "kamu yang memutuskan per varian"
                        : "WAJIB " + request.kind().name() + " untuk semua varian"),
                "Jumlah varian: " + (request.count() == null ? "kamu yang memutuskan, 1 sampai " + MAX_COUNT
                        : "WAJIB tepat " + request.count()),
                "", "Instruksi champion:", instruction);

        String content = deepSeekClient.createJsonCompletion(SYSTEM_PROMPT, userPrompt,
                PLAN_TOKENS + TOKENS_PER_DRAFT * count, TEMPERATURE);
        List<AssignmentDraft> drafts = parseDrafts(content, request.kind(), request.count(), count, categories);

        String batchId = NanoId.generate();
        for (AssignmentDraft draft : drafts) {
            draft.setBatchId(batchId);
            draft.setChampionAccessId(champion.accessId());
            draft.setProjectId(champion.projectId());
            draft.setInstruction(instruction);
            draft.setRequestedKind(request.kind());
        }
        assignmentDraftRepository.saveAll(drafts);

        // Re-read so created_at comes back from the DB default rather than staying null on the saved entities.
        return AssignmentDraftBatch.from(assignmentDraftRepository
                .findByChampionAccessIdAndBatchIdInOrderByIdAsc(champion.accessId(), List.of(batchId)));
    }

    public AssignmentDraftBatchesResponse listBatches(String jwt, ChampionProjectRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());

        List<String> batchIds = assignmentDraftRepository.findRecentBatchIds(champion.accessId(), MAX_BATCHES);
        if (batchIds.isEmpty()) {
            return new AssignmentDraftBatchesResponse(List.of());
        }

        Map<String, List<AssignmentDraft>> byBatch = new LinkedHashMap<>();
        batchIds.forEach(batchId -> byBatch.put(batchId, new ArrayList<>()));
        assignmentDraftRepository.findByChampionAccessIdAndBatchIdInOrderByIdAsc(champion.accessId(), batchIds)
                .forEach(draft -> byBatch.get(draft.getBatchId()).add(draft));

        return new AssignmentDraftBatchesResponse(byBatch.values().stream()
                .filter(drafts -> !drafts.isEmpty())
                .map(AssignmentDraftBatch::from)
                .toList());
    }

    public DeleteDraftResponse deleteDraft(String jwt, DeleteDraftRequest request) {
        ChampionContext champion = championAccessGuard.requireChampion(jwt, request.projectId());
        AssignmentDraft draft = assignmentDraftRepository.findByIdAndChampionAccessId(request.draftId(),
                champion.accessId()).orElseThrow(() -> new ResourceNotFoundException("Draft not found"));
        assignmentDraftRepository.delete(draft);
        return new DeleteDraftResponse(true);
    }

    // One bad variant is dropped rather than failing the batch; only an empty result is the model's fault.
    @SuppressWarnings("unchecked")
    private List<AssignmentDraft> parseDrafts(String content, AssignmentKind requestedKind, Integer requestedCount,
            int count, List<Category> categories) {
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(content, Map.class);
        } catch (JacksonException e) {
            log.error("DeepSeek drafts are not valid JSON: {}", content);
            throw new BadGatewayException(INVALID_DRAFT_MESSAGE);
        }

        Set<Short> validIds = categories.stream().map(Category::getId).collect(Collectors.toSet());
        List<AssignmentDraft> drafts = new ArrayList<>();
        if (parsed.get("drafts") instanceof List<?> rawDrafts) {
            for (Object rawDraft : rawDrafts) {
                if (drafts.size() == count) {
                    break;
                }
                if (rawDraft instanceof Map<?, ?> fields) {
                    AssignmentDraft draft = toDraft((Map<String, Object>) fields, requestedKind, validIds,
                            drafts.size() + 1);
                    if (draft != null) {
                        drafts.add(draft);
                    }
                }
            }
        }

        if (drafts.isEmpty()) {
            log.error("DeepSeek returned no usable drafts: {}", content);
            throw new BadGatewayException(INVALID_DRAFT_MESSAGE);
        }
        if (requestedCount != null && drafts.size() < requestedCount) {
            log.warn("DeepSeek returned {} usable drafts out of {} requested", drafts.size(), requestedCount);
        }
        log.info("DeepSeek draft plan: {}", parsed.get("plan"));
        return drafts;
    }

    private static AssignmentDraft toDraft(Map<String, Object> fields, AssignmentKind requestedKind,
            Set<Short> validIds, int position) {
        AssignmentKind kind = requestedKind != null ? requestedKind : parseKind(fields.get("kind"));
        String name = text(fields.get("name"));
        String description = text(fields.get("description"));
        String expectedOutput = kind == AssignmentKind.PROMPT ? text(fields.get("expected_output")) : null;
        if (kind == null || name == null || description == null
                || (kind == AssignmentKind.PROMPT && expectedOutput == null)) {
            return null;
        }

        String angle = text(fields.get("angle"));
        List<Short> categoryIds = new ArrayList<>();
        if (fields.get("category_ids") instanceof List<?> rawIds) {
            for (Object rawId : rawIds) {
                Short id = toShort(rawId);
                if (id != null && validIds.contains(id) && !categoryIds.contains(id)
                        && categoryIds.size() < MAX_CATEGORIES) {
                    categoryIds.add(id);
                }
            }
        }

        AssignmentDraft draft = new AssignmentDraft();
        draft.setKind(kind);
        draft.setAngle(truncate(angle == null ? "Varian " + position : angle, ANGLE_MAX_LENGTH));
        draft.setName(truncate(name, NAME_MAX_LENGTH));
        draft.setDescription(description);
        draft.setExpectedOutput(expectedOutput);
        draft.setCategoryIds(categoryIds.toArray(Short[]::new));
        return draft;
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

    private static String truncate(String value, int maxLength) {
        return value.length() > maxLength ? value.substring(0, maxLength).trim() : value;
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
