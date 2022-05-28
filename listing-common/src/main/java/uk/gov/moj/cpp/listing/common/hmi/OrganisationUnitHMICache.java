package uk.gov.moj.cpp.listing.common.hmi;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.staginghmi.OrganisationUnitHmiStatus;
import uk.gov.moj.cpp.staginghmi.common.azure.OrganisationUnitHMIStatusService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class OrganisationUnitHMICache {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationUnitHMICache.class);

    @Inject
    private OrganisationUnitHMIStatusService organisationUnitHMIStatusService;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    private Set<String> notHmiEnabledCourtCentreIdSet = new HashSet();

    @PostConstruct
    public void initNotHmiEnabledCourtCentreIdData() {

        final Response response = organisationUnitHMIStatusService.getAllOrganisationUnitHMIStatus();
        final JsonObject resultJson = objectToJsonObjectConverter.convert(response.getEntity());

        final JsonArray organisationUnits = resultJson.getJsonArray("organisationUnitHMIStatus");
        if(nonNull(organisationUnits)){
            LOGGER.info("OrganisationUnitHMICache organisationUnits size {} ", organisationUnits.size());

            for (int i = 0; i < organisationUnits.size(); i++) {
                OrganisationUnitHmiStatus organisationUnitHmiStatus = jsonObjectConverter.convert(organisationUnits.getJsonObject(i), OrganisationUnitHmiStatus.class);
                if(!organisationUnitHmiStatus.getIsHMIListingEnabled()){
                    LOGGER.info("OrganisationUnitHMICache court centre id is added {} ", organisationUnitHmiStatus.getCourtCentreId());
                    this.notHmiEnabledCourtCentreIdSet.add(organisationUnitHmiStatus.getCourtCentreId());
                }
            }
        }else {
            LOGGER.info("organisationUnits is null");
        }
    }

    public Set<String> getNotHmiEnabledCourtCentreIdSet() {
        if (isNull(this.notHmiEnabledCourtCentreIdSet) ||
                this.notHmiEnabledCourtCentreIdSet.isEmpty() ||
                this.notHmiEnabledCourtCentreIdSet.stream().map(Optional::ofNullable).findFirst().flatMap(Function.identity()).orElse(null) == null) {
            LOGGER.info("OrganisationUnitHMICache.initNotHmiEnabledCourtCentreIdData is called again");
            initNotHmiEnabledCourtCentreIdData();
        }
        return this.notHmiEnabledCourtCentreIdSet;
    }

}
