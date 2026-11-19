package uk.gov.moj.cpp.listing.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A controller overrides the generated {@code *OpenApi} method by position, so its parameter names
 * must follow the generated order, where the binding annotations are. Most parameters are
 * {@code String}s, so a mismatch compiles and silently swaps values (P2.3.1: D33 made the
 * hearing-slots {@code sessionEndDate} optional, the generator moved it after the required
 * parameters, and the controller kept the old order).
 */
class ControllerParameterOrderTest {

    @Test
    void everyControllerParameterHasTheNameOfTheGeneratedParameterItOverrides() throws Exception {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        final List<String> mismatches = new ArrayList<>();
        int checked = 0;
        for (final var candidate : scanner.findCandidateComponents("uk.gov.moj.cpp.listing.controllers")) {
            final Class<?> controller = Class.forName(candidate.getBeanClassName());
            for (final Class<?> api : controller.getInterfaces()) {
                if (!api.getSimpleName().endsWith("OpenApi")) {
                    continue;
                }
                for (final Method generated : api.getMethods()) {
                    final Method implementation = controller.getMethod(generated.getName(), generated.getParameterTypes());
                    final Parameter[] expected = generated.getParameters();
                    final Parameter[] actual = implementation.getParameters();
                    for (int i = 0; i < expected.length; i++) {
                        final String boundName = boundName(expected[i]);
                        if (boundName != null) {
                            checked++;
                            if (!boundName.equals(actual[i].getName())) {
                                mismatches.add(controller.getSimpleName() + "." + implementation.getName() + " parameter " + i
                                        + ": generated '" + boundName + "', controller '" + actual[i].getName() + "'");
                            }
                        }
                    }
                }
            }
        }
        assertThat(checked).isPositive();
        assertThat(mismatches).isEmpty();
    }

    private static String boundName(final Parameter parameter) {
        final RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null) {
            return requestParam.value();
        }
        final PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        return pathVariable != null ? pathVariable.value() : null;
    }
}
