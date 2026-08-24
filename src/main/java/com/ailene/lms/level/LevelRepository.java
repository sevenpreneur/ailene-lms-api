package com.ailene.lms.level;

import com.ailene.lms.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevelRepository extends JpaRepository<Level, Integer> {

    List<Level> findByProjectIdAndStatusOrderByLevelNumberAsc(String projectId, Status status);
}
