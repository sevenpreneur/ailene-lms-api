package com.ailene.lms.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@RegisterReflectionForBinding({GuidedStep.class, GuidedStep[].class, GuidedWorkflow.class})
public class GuidedJson {
    private final ObjectMapper mapper;

    public String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    public List<String> strings(String json) {
        return Arrays.asList(mapper.readValue(json, String[].class));
    }

    public List<GuidedStep> steps(String json) {
        return Arrays.asList(mapper.readValue(json, GuidedStep[].class));
    }
}
