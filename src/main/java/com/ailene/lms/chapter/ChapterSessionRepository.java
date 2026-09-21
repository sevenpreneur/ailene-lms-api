package com.ailene.lms.chapter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterSessionRepository extends JpaRepository<ChapterSession, Integer> {

    List<ChapterSession> findByChapterId(Integer chapterId);
}
