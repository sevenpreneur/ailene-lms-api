package com.ailene.lms.admin;

import java.util.Collection;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Stream;

// Pure helpers so every insight averages and rounds the same way.
final class AdminInsightsMath {

    private AdminInsightsMath() {
    }

    static Double average(Stream<? extends Number> values) {
        OptionalDouble avg = values.filter(Objects::nonNull).mapToDouble(Number::doubleValue).average();
        return avg.isPresent() ? round1(avg.getAsDouble()) : null;
    }

    static Double average(Collection<? extends Number> values) {
        return average(values.stream());
    }

    static Double sum(Stream<? extends Number> values) {
        return round1(values.filter(Objects::nonNull).mapToDouble(Number::doubleValue).sum());
    }

    static Double trainer(AdminReviewInsights.Ratings r) {
        return average(Stream.of(r.trainerClarity(), r.trainerMastery(), r.trainerResponsiveness(),
                r.trainerEngagement()));
    }

    static Double material(AdminReviewInsights.Ratings r) {
        return average(Stream.of(r.materialRelevance(), r.materialFlow(), r.materialDepth()));
    }

    // All eight ratings weigh the same, matching how the survey asks them.
    static Double overall(AdminReviewInsights.Ratings r) {
        return average(Stream.of(r.trainerClarity(), r.trainerMastery(), r.trainerResponsiveness(),
                r.trainerEngagement(), r.materialRelevance(), r.materialFlow(), r.materialDepth(),
                r.learningValue()));
    }

    static Double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
