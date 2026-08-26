package com.ailene.lms.learnings;

import com.ailene.lms.level.Level;

public record LevelDto(Integer id, Short levelNumber, String name) {

    public static LevelDto from(Level level) {
        return new LevelDto(level.getId(), level.getLevelNumber(), level.getName());
    }
}
