package uk.gov.moj.cpp.listing.command.api.mapper;

import uk.gov.justice.services.adapter.rest.application.DefaultCommonProviders;

import java.util.Set;

import javax.enterprise.inject.Specializes;

/**
 * Registers the listing command-api's custom JAX-RS providers with the generated REST
 * {@code Application}. The framework-generated Application injects {@code CommonProviders} and
 * builds its {@code getClasses()} set solely from {@link #providers()} — providers are NOT
 * classpath-scanned, so a {@code @Provider} annotation alone does nothing. {@code @Specializes}
 * makes this bean replace the framework's {@code @Default DefaultCommonProviders} at that
 * injection point while inheriting its qualifiers.
 *
 * <p><strong>This is the single specialization point for command-api JAX-RS providers.</strong>
 * CDI allows only ONE bean to specialize {@link DefaultCommonProviders} per deployment — adding a
 * second {@code @Specializes DefaultCommonProviders} subclass anywhere on the classpath makes the
 * whole WAR fail to deploy (inconsistent specialization). Register any future command-api
 * provider (exception mappers, filters, features) by adding its class here; do NOT create another
 * specializing subclass.
 */
@Specializes
public class ListingCommandCommonProviders extends DefaultCommonProviders {

    @Override
    public Set<Class<?>> providers() {
        final Set<Class<?>> providers = super.providers();
        providers.add(MoveHearingToPastDateExceptionMapper.class);
        return providers;
    }
}
