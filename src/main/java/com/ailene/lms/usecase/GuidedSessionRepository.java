package com.ailene.lms.usecase;

import com.ailene.lms.access.AccessRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface GuidedSessionRepository extends JpaRepository<GuidedSession, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from GuidedSession s where s.id = :id")
    Optional<GuidedSession> lockById(Integer id);

    @Query("""
            select s from GuidedSession s, Access a
            where s.id = :id and a.id = s.studentAccessId and a.userId = :userId and a.role = :role
            """)
    Optional<GuidedSession> findOwned(Integer id, UUID userId, AccessRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from GuidedSession s where s.id = :id and exists (
                select 1 from Access a where a.id = s.studentAccessId and a.userId = :userId and a.role = :role
            )
            """)
    Optional<GuidedSession> lockOwned(Integer id, UUID userId, AccessRole role);

    Optional<GuidedSession> findFirstByStudentAccessIdAndUseCaseIdOrderByIdDesc(String accessId, Integer useCaseId);

    Optional<GuidedSession> findFirstByStudentAccessIdAndProjectIdAndUseCaseIdIsNullAndStatusNotOrderByIdDesc(
            String accessId, String projectId, GuidedStatus status);
}
