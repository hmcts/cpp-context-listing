package uk.gov.moj.cpp.listing.common.client;

import java.util.Map;
import java.util.Set;

/**
 * One remote query the framework's generated {@code Remote*} clients exposed: the service's REST
 * root (relative to the CPP gateway), the path template (with {@code {param}} placeholders filled
 * from the request payload) and the query parameters it forwards. The {@code Accept} header is the
 * framework convention {@code application/vnd.<action name>+json}.
 */
public record CppEndpoint(String contextRoot, String pathTemplate, Set<String> queryParams) {

    // Context roots as deployed (<context>-service WARs), which is what the framework's REST client
    // called and what the legacy ITs stub (listing-integration-test ReferenceDataStub, ProgressionStub,
    // DefenceStub, UsersAndGroupsStub). The generated clients' compile-time BASE_URI
    // (http://localhost:8080/<context>-query-api/...) was rewritten by the framework at run time.
    static final String REFERENCEDATA = "/referencedata-service/query/api/rest/referencedata";
    static final String PROGRESSION = "/progression-service/query/api/rest/progression";
    static final String DEFENCE = "/defence-service/query/api/rest/defence";
    static final String USERSGROUPS = "/usersgroups-service/query/api/rest/usersgroups";

    /**
     * The endpoints the query side calls, copied from the generated clients in
     * listing-query-api/target/generated-sources (RemoteQueryApi2*QueryApi) and listing-common's
     * reference-data clients.
     */
    static final Map<String, CppEndpoint> ENDPOINTS = Map.ofEntries(
            Map.entry("defence.query.get-case-by-person-defendant", new CppEndpoint(DEFENCE, "/case/person-defendant",
                    Set.of("firstName", "lastName", "dateOfBirth", "isCivil", "isGroupMember"))),
            Map.entry("defence.query.get-case-by-organisation-defendant", new CppEndpoint(DEFENCE, "/case/organisation-defendant",
                    Set.of("organisationName", "isCivil", "isGroupMember"))),
            Map.entry("progression.query.application-notes", new CppEndpoint(PROGRESSION, "/applications/{applicationId}/notes", Set.of())),
            Map.entry("progression.query.application-only", new CppEndpoint(PROGRESSION, "/applications/{applicationId}", Set.of())),
            Map.entry("progression.query.case-exist-by-caseurn", new CppEndpoint(PROGRESSION, "/search", Set.of("q", "caseIds", "caseUrn"))),
            Map.entry("progression.query.case-notes", new CppEndpoint(PROGRESSION, "/cases/{caseId}/notes", Set.of())),
            Map.entry("progression.query.prosecutioncase", new CppEndpoint(PROGRESSION, "/prosecutioncases/{caseId}", Set.of("orderDate", "isAllRelatedCases"))),
            Map.entry("referencedata.query.all-hearing-types", new CppEndpoint(REFERENCEDATA, "/hearing-types", Set.of())),
            Map.entry("referencedata.query.hearing-types", new CppEndpoint(REFERENCEDATA, "/hearing-types", Set.of())),
            Map.entry("referencedata.query.courtroom", new CppEndpoint(REFERENCEDATA, "/courtrooms/{id}", Set.of())),
            Map.entry("referencedata.query.courtrooms", new CppEndpoint(REFERENCEDATA, "/courtrooms",
                    Set.of("oucodeL1Code", "oucode", "ouCourtRoomCode", "ouCourtRoomName", "courtId", "courtCentreId"))),
            Map.entry("referencedata.query.cp-xhibit-court-mappings", new CppEndpoint(REFERENCEDATA, "/cp-xhibit-court-mappings", Set.of("ouId", "crestCourtId"))),
            Map.entry("referencedata.query.cp-xhibit-courtroom-mappings", new CppEndpoint(REFERENCEDATA, "/cp-xhibit-courtroom-mappings", Set.of("oucode", "courtRoomId", "ouId"))),
            Map.entry("referencedata.query.cp-xhibit-mags-court-mapping", new CppEndpoint(REFERENCEDATA, "/cp-xhibit-mags-court-mapping", Set.of())),
            Map.entry("referencedata.query.judiciaries", new CppEndpoint(REFERENCEDATA, "/judiciaries",
                    Set.of("judiciaryType", "judiciaryGroup", "judiciaryGroups", "ids", "search", "limit", "personalCode", "judiciaryTypeId", "withSpecialism"))),
            Map.entry("referencedata.query.organisationunits", new CppEndpoint(REFERENCEDATA, "/organisationunits",
                    Set.of("oucodeL1Code", "oucodeL2Code", "oucodeL3Code", "oucode", "spiOucode"))),
            Map.entry("referencedata.query.prosecutor", new CppEndpoint(REFERENCEDATA, "/prosecutors/{id}", Set.of())),
            Map.entry("usersgroups.is-logged-in-user-has-permission-for-action", new CppEndpoint(USERSGROUPS, "/users/logged-in-user/permissions", Set.of("object", "action")))
    );
}
