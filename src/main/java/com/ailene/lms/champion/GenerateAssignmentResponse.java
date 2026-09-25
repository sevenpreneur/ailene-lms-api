package com.ailene.lms.champion;

import java.util.List;

public record GenerateAssignmentResponse(AssignmentKind kind, String name, String description, String expectedOutput,
        List<Short> categoryIds) {
}
