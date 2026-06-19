package uk.gov.moj.cpp.listing.query.view.hearing;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.UUID.randomUUID;
import static org.apache.deltaspike.core.util.ArraysUtils.asSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.query.view.dto.ApplicationTypeTest;
import uk.gov.moj.cpp.listing.query.view.dto.Permission;
import uk.gov.moj.cpp.listing.query.view.service.UsersGroupsService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("squid:S2187")
@ExtendWith(MockitoExtension.class)
public class ApplicationTypeFilterTest {

    public static final String PERMITTED_TYPE_CODE1 = "PL84506";
    public static final String PERMITTED_TYPE_CODE2 = "PL84505";
    public static final String RANDOM_TYPE_CODE = "XX84500";
    public static final String UN_PERMITTED_TYPE_CODE1 = "PL84501";
    public static final String UN_PERMITTED_TYPE_CODE2 = "PL84502";
    @InjectMocks
    private ApplicationTypeFilter applicationTypeFilter;

    @Mock
    private UsersGroupsService usersGroupsService;

    @Mock
    private Metadata queryMetadata;

    final List<Permission> permissions = asList(
            new Permission(randomUUID(), PERMITTED_TYPE_CODE1, "Access to Standalone Application", true),
            new Permission(randomUUID(), UN_PERMITTED_TYPE_CODE1, "Access to Standalone Application", false),
            new Permission(randomUUID(), PERMITTED_TYPE_CODE2, "Access to Standalone Application", true),
            new Permission(randomUUID(), UN_PERMITTED_TYPE_CODE2, "Access to Standalone Application", false)
    );

    @Test
    public void doNotApplyFilteringWhenHearingListIsEmpty() {

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, emptyList());

        assertThat(result.size(), is(0));
    }

    @Test
    public void doNotApplyFilteringWhenHearingHasNoApplication() {
        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(emptySet());

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(emptySet());

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(2));
    }

    @Test
    public void doNotApplyFilteringWhenPermissionListReturnsEmpty() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(emptySet());

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(null)));


        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(emptyList());

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(2));
    }

    @Test
    public void doNotApplyFilteringWhenOneHearingHasNoApplicationAndOtherHasPermission() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(emptySet());

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(PERMITTED_TYPE_CODE1))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(2));
    }

    @Test
    public void doNotApplyFilteringWhenBothHearingHasPermission() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(PERMITTED_TYPE_CODE2))));

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(PERMITTED_TYPE_CODE1))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(2));
    }

    @Test
    public void doNotApplyFilteringWhenOneHearingHasPermissionOtherHasRandomApplicationType() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(PERMITTED_TYPE_CODE2))));

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(RANDOM_TYPE_CODE))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(2));
    }

    @Test
    public void doApplyFilteringWhenOneHearingDoNotHasPermissionOtherHasRandomApplicationType() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(UN_PERMITTED_TYPE_CODE1))));

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(RANDOM_TYPE_CODE))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(1));
    }

    @Test
    public void doApplyFilteringWhenOneHearingDoNotHasPermissionOtherHasPermission() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(UN_PERMITTED_TYPE_CODE1))));

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(PERMITTED_TYPE_CODE1))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(1));
    }

    @Test
    public void doApplyFilteringWhenBothHearingDoNotHavePermission() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(UN_PERMITTED_TYPE_CODE1))));

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(UN_PERMITTED_TYPE_CODE2))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(0));
    }

    @Test
    public void doApplyFilteringWhenOneHearingDoNotHavePermissionOtherHasNoApplication() {

        final Hearing hearingMock1 = mock(Hearing.class);
        when(hearingMock1.getCourtApplications()).thenReturn(emptySet());

        final Hearing hearingMock2 = mock(Hearing.class);
        when(hearingMock2.getCourtApplications()).thenReturn(asSet(getCourtApplication(ApplicationTypeTest.getApplicationTypeTitle(UN_PERMITTED_TYPE_CODE2))));

        when(usersGroupsService.getUserPermissionForApplicationTypes(queryMetadata)).thenReturn(permissions);

        List<Hearing> result = applicationTypeFilter.filter(queryMetadata, asList(hearingMock1, hearingMock2));

        assertThat(result.size(), is(1));
    }

    private static CourtApplications getCourtApplication(final String applicationTitle) {
        return new CourtApplications(randomUUID(), null, null, applicationTitle, null, null, null, null);
    }

}