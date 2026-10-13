package com.ailene.lms.chaptertest;

public record ChapterTestOptionItem(Integer id, String optionCode, String text) {

    public static ChapterTestOptionItem from(ChapterTestQuestion.Option option) {
        return new ChapterTestOptionItem(option.id(), option.optionCode(), option.text());
    }
}
