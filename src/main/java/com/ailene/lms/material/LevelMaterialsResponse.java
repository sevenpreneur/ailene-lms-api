package com.ailene.lms.material;

import java.util.List;

public record LevelMaterialsResponse(Short levelNumber, List<LevelMaterialItem> materials) {
}
