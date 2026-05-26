package uk.gov.moj.cpp.listing.command.api.mapper;

import uk.gov.justice.services.adapter.rest.application.DefaultCommonProviders;

import java.util.Set;

import javax.enterprise.inject.Specializes;

@Specializes
public class ListingCommandCommonProviders extends DefaultCommonProviders {

    @Override
    public Set<Class<?>> providers() {
        final Set<Class<?>> providers = super.providers();
        providers.add(CrownMultiDayExtensionExceptionMapper.class);
        return providers;
    }
}
