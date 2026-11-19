package uk.gov.moj.cpp.listing.domain.json;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * The behaviour of {@code jackson-module-parameter-names} (registered by the framework's
 * ObjectMapperProducer as {@code new ParameterNamesModule(PROPERTIES)}): constructor and factory
 * parameters are named from the class file ({@code -parameters}), so unannotated constructors
 * such as {@code ListedCase(caseIdentifier, defendants, ...)} work as property-based creators.
 * Inlined because the module's 2.x artifact for Spring Boot 4's Jackson 2 line is not available.
 */
public class ParameterNamesModule extends SimpleModule {

    public ParameterNamesModule() {
        super("ListingParameterNamesModule");
    }

    @Override
    public void setupModule(final SetupContext context) {
        super.setupModule(context);
        context.appendAnnotationIntrospector(new ParameterNamesIntrospector());
    }

    private static final class ParameterNamesIntrospector extends NopAnnotationIntrospector {

        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(final AnnotatedMember member) {
            if (member instanceof AnnotatedParameter parameter) {
                final Object owner = parameter.getOwner().getMember();
                if (owner instanceof Executable executable) {
                    final Parameter reflected = executable.getParameters()[parameter.getIndex()];
                    return reflected.isNamePresent() ? reflected.getName() : null;
                }
            }
            return null;
        }

        @Override
        public JsonCreator.Mode findCreatorAnnotation(final MapperConfig<?> config, final Annotated a) {
            final JsonCreator ann = _findAnnotation(a, JsonCreator.class);
            if (ann != null) {
                final JsonCreator.Mode mode = ann.mode();
                return mode == JsonCreator.Mode.DEFAULT ? JsonCreator.Mode.PROPERTIES : mode;
            }
            return null;
        }
    }
}
