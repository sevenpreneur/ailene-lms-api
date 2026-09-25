package com.ailene.lms.prompt;

public record PromptRubricScore(short specificity, short context, short constraints, short examples,
        short iteration, String feedback) {
}
