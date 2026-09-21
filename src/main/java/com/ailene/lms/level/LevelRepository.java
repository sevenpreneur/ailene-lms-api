package com.ailene.lms.level;

import com.ailene.lms.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LevelRepository extends JpaRepository<Level, Integer> {

    List<Level> findByStatusOrderByLevelNumberAsc(Status status);

    Optional<Level> findByLevelNumber(Short levelNumber);
}
