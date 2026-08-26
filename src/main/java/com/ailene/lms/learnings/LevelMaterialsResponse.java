package com.ailene.lms.learnings;

import java.util.List;

public record LevelMaterialsResponse(Short levelNumber, List<LevelMaterialItem> materials) {
}
