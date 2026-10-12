package com.ailene.lms.common.nativeimage;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.util.UUID;

// Reflection the native image cannot discover on its own; a no-op on the JVM.
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeImageHints.Registrar.class)
public class NativeImageHints {

    static class Registrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Hibernate's multi-id and batch loaders allocate an array of each entity's @Id type reflectively.
            for (Class<?> idArray : new Class<?>[] {UUID[].class, Integer[].class, Short[].class, String[].class}) {
                hints.reflection().registerType(idArray);
            }
        }
    }
}
