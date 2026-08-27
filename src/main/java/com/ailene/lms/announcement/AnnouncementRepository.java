package com.ailene.lms.announcement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    Optional<Announcement> findByProjectId(String projectId);
}
