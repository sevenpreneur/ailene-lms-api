package com.ailene.lms.learnings;

public record LevelMaterialItem(String id, String title, Integer index, Boolean completed, Boolean locked,
        Boolean isCurrent) {
}
