package uk.gov.moj.cpp.listing.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.Invocation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.yaml.snakeyaml.Yaml;

/**
 * Every command operation of the OpenAPI spec, with every request media type it declares, reaches
 * a mapped handler (MBD refactor P2.3.3: no command media type may answer 404 or fall through to
 * "unexpected media type"). For each (operation, media type) the controller method named by the
 * {@code operationId} is called with that Content-Type, its command service mocked: the answer must
 * be 202 and exactly one command-service call must be made, carrying a command named after the
 * media type or its RAML {@code (mapping)} name.
 */
class CommandOperationsCoverageTest {

    private static final String SPEC = "/openapi/listing-api.openapi.yml";
    private static final String COMMAND_ROOT = "/command/api/rest/listing/";

    /** The command controllers; each takes (its command service, the request). */
    private static final List<Class<?>> COMMAND_CONTROLLERS = List.of(
            CaseCommandsApi.class, HearingCommandsApi.class, CourtScheduleCommandsApi.class,
            CourtListCommandsApi.class, ListingNoteCommandsApi.class);

    /** RAML {@code (mapping)} names that differ from the media type. */
    private static final Map<String, String> RAML_MAPPING_NAMES = Map.of(
            "listing.duplicate-unallocated-hearing", "listing.mark-unallocated-hearing-as-duplicate",
            "listing.next-hearings-v2", "listing.list-next-hearings-v2",
            "listing.related-hearing", "listing.update-related-hearing");

    record CommandOperation(String path, String operationId, String mediaType, List<String> pathParameters) {
        @Override
        public String toString() {
            return operationId + " " + mediaType;
        }
    }

    @SuppressWarnings("unchecked")
    static List<CommandOperation> commandOperations() throws Exception {
        final Map<String, Object> spec;
        try (InputStream in = CommandOperationsCoverageTest.class.getResourceAsStream(SPEC)) {
            spec = new Yaml().load(in);
        }
        final List<CommandOperation> operations = new ArrayList<>();
        final Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        paths.forEach((path, item) -> {
            if (!path.startsWith(COMMAND_ROOT)) {
                return;
            }
            ((Map<String, Object>) item).forEach((method, op) -> {
                if (!"post".equals(method)) {
                    return;
                }
                final Map<String, Object> operation = (Map<String, Object>) op;
                final List<String> pathParameters = new ArrayList<>();
                final List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.getOrDefault("parameters", List.of());
                parameters.stream().filter(p -> "path".equals(p.get("in"))).forEach(p -> pathParameters.add((String) p.get("name")));
                final Map<String, Object> content = (Map<String, Object>) ((Map<String, Object>) operation.get("requestBody")).get("content");
                content.keySet().forEach(mediaType ->
                        operations.add(new CommandOperation(path, (String) operation.get("operationId"), mediaType, pathParameters)));
            });
        });
        return operations;
    }

    static Stream<Arguments> operations() throws Exception {
        return commandOperations().stream().map(Arguments::of);
    }

    @Test
    void theSpecDeclaresTheCommandOperations() throws Exception {
        assertThat(commandOperations()).hasSizeGreaterThanOrEqualTo(32);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("operations")
    void eachCommandMediaTypeIsDispatchedToACommandService(final CommandOperation operation) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/listing-service" + operation.path());
        request.setContentType(operation.mediaType());

        final Target target = controllerFor(operation.operationId(), request);
        final Object[] arguments = argumentsFor(target.method(), operation);

        final ResponseEntity<?> response;
        try {
            response = (ResponseEntity<?>) target.method().invoke(target.controller(), arguments);
        } catch (final InvocationTargetException e) {
            throw new AssertionError(operation + " was not dispatched: " + e.getCause(), e.getCause());
        }

        assertThat(response.getStatusCode()).as(operation.toString()).isEqualTo(HttpStatus.ACCEPTED);
        final List<Invocation> calls = new ArrayList<>(mockingDetails(target.service()).getInvocations());
        assertThat(calls).as(operation + ": command-service calls").hasSize(1);

        final String action = operation.mediaType().substring("application/vnd.".length(), operation.mediaType().length() - "+json".length());
        final String expectedName = RAML_MAPPING_NAMES.getOrDefault(action, action);
        assertThat(commandName(calls.get(0))).as(operation + ": command name").isEqualTo(expectedName);
    }

    private record Target(Object controller, Method method, Object service) {
    }

    private static Target controllerFor(final String operationId, final HttpServletRequest request) throws Exception {
        for (final Class<?> controllerClass : COMMAND_CONTROLLERS) {
            for (final Method method : controllerClass.getDeclaredMethods()) {
                if (method.getName().equals(operationId) && ResponseEntity.class.equals(method.getReturnType())) {
                    final Constructor<?> constructor = controllerClass.getConstructors()[0];
                    final Object service = mock(constructor.getParameterTypes()[0]);
                    return new Target(constructor.newInstance(service, request), method, service);
                }
            }
        }
        throw new AssertionError("No command controller implements operation " + operationId);
    }

    private static Object[] argumentsFor(final Method method, final CommandOperation operation) {
        final Parameter[] parameters = method.getParameters();
        final Object[] arguments = new Object[parameters.length];
        int pathIndex = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (Map.class.equals(parameters[i].getType())) {
                arguments[i] = new LinkedHashMap<String, Object>(Map.of("note", "x"));
            } else {
                final String name = operation.pathParameters().get(pathIndex++);
                arguments[i] = switch (name) {
                    case "publishCourtListType" -> "FINAL";
                    case "startDate" -> "2026-01-01";
                    default -> "11111111-1111-1111-1111-111111111111";
                };
            }
        }
        return arguments;
    }

    private static String commandName(final Invocation call) {
        for (final Object argument : call.getArguments()) {
            if (argument instanceof Envelope envelope) {
                return envelope.name();
            }
            if (argument instanceof Metadata metadata) {
                return metadata.name();
            }
        }
        throw new AssertionError("The command-service call carries no command: " + call);
    }
}
