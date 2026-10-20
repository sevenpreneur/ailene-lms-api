package com.ailene.lms.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuidedUseCaseService {
    private final GuidedUseCaseStore store;
    private final GuidedWorkflowGenerator generator;

    public GuidedSessionResponse generate(UUID userId, GuidedGenerateRequest request) {
        GuidedUseCaseStore.GenerationReservation reserved = store.reserve(userId, request);
        try {
            GuidedWorkflow workflow = generator.generate(reserved.session(), request.feedback());
            return store.finish(userId, reserved, request.feedback(), workflow);
        } catch (RuntimeException failure) {
            try {
                store.release(userId, reserved);
            } catch (RuntimeException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }
}
