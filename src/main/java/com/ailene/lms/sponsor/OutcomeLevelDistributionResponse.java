package com.ailene.lms.sponsor;

import java.util.List;

public record OutcomeLevelDistributionResponse(int total, List<OutcomeLevelItem> distribution) {
}
