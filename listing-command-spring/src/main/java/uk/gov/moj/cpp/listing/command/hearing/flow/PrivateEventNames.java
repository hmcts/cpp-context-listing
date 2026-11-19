package uk.gov.moj.cpp.listing.command.hearing.flow;

import static java.util.Map.entry;

import java.util.Map;

/**
 * The names the JEE build gave the listing domain events ({@code @Event("…")} on the generated
 * classes, stripped from the vendored copies, ADR 015). The Spring command services never store or
 * publish these events; the name only becomes the in-memory {@link RaisedEvent}'s name, from
 * which the public messages take their metadata (as the JEE processor's
 * {@code metadataFrom(event.metadata())} did). Generated from
 * {@code listing-domain/listing-domain-event/target/generated-sources} (MBD refactor P2.3.2).
 */
public final class PrivateEventNames {

    private static final Map<Class<?>, String> NAMES = Map.ofEntries(
            entry(uk.gov.justice.listing.event.CourtApplicationHearingDeleted.class, "listing.events.court-application-hearing-deleted"),
            entry(uk.gov.justice.listing.event.CourtListExportRequested.class, "listing.event.court-list-export-requested"),
            entry(uk.gov.justice.listing.event.HearingCounselModified.class, "listing.event.hearing-counsel-modified"),
            entry(uk.gov.justice.listing.event.ListingNumberUpdated.class, "listing.events.listing-number-updated"),
            entry(uk.gov.justice.listing.event.PublishCourtListExportFailed.class, "listing.event.publish-court-list-export-failed"),
            entry(uk.gov.justice.listing.event.PublishCourtListExportSuccessful.class, "listing.event.publish-court-list-export-successful"),
            entry(uk.gov.justice.listing.event.PublishCourtListProduced.class, "listing.event.publish-court-list-produced"),
            entry(uk.gov.justice.listing.event.PublishCourtListRequested.class, "listing.event.publish-court-list-requested"),
            entry(uk.gov.justice.listing.event.PublishedCourtListStored.class, "listing.event.published-court-list-stored"),
            entry(uk.gov.justice.listing.events.AddedCasesForHearing.class, "listing.event.added-cases-for-hearing"),
            entry(uk.gov.justice.listing.events.AllocatedHearingDeleted.class, "listing.events.allocated-hearing-deleted"),
            entry(uk.gov.justice.listing.events.AllocatedHearingExtendedForListing.class, "listing.events.allocated-hearing-extended-for-listing"),
            entry(uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2.class, "listing.events.allocated-hearing-extended-for-listing-v2"),
            entry(uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing.class, "listing.events.allocated-hearing-updated-for-listing"),
            entry(uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2.class, "listing.events.allocated-hearing-updated-for-listing-v2"),
            entry(uk.gov.justice.listing.events.ApplicationEjected.class, "listing.events.application-ejected"),
            entry(uk.gov.justice.listing.events.ApplicationEjectedForHearings.class, "listing.events.application-ejected-for-hearings"),
            entry(uk.gov.justice.listing.events.AvailableSlotsForHearingFreed.class, "listing.events.available-slots-for-hearing-freed"),
            entry(uk.gov.justice.listing.events.CaseEjected.class, "listing.events.case-ejected"),
            entry(uk.gov.justice.listing.events.CaseEjectedForHearings.class, "listing.events.case-ejected-for-hearings"),
            entry(uk.gov.justice.listing.events.CaseIdentifierUpdated.class, "listing.events.case-identifier-updated"),
            entry(uk.gov.justice.listing.events.CaseMarkersToBeUpdated.class, "listing.events.case-markers-to-be-updated"),
            entry(uk.gov.justice.listing.events.CaseRemovedFromGroupCases.class, "listing.events.case-removed-from-group-cases"),
            entry(uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded.class, "listing.events.case-resulted-defendant-proceedings-updated"),
            entry(uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated.class, "listing.events.case-update-defendant-proceedings-updated"),
            entry(uk.gov.justice.listing.events.CasesAddedToHearing.class, "listing.event.cases-added-to-hearing"),
            entry(uk.gov.justice.listing.events.CourtApplicationAddedForHearing.class, "listing.events.court-application-added-for-hearing"),
            entry(uk.gov.justice.listing.events.CourtApplicationAddedToHearing.class, "listing.events.court-application-added-to-hearing"),
            entry(uk.gov.justice.listing.events.CourtApplicationToBeUpdated.class, "listing.events.court-application-to-be-updated"),
            entry(uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing.class, "listing.events.court-application-updated-for-hearing"),
            entry(uk.gov.justice.listing.events.CourtCentreChangedForHearing.class, "listing.events.court-centre-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.CourtListRestricted.class, "listing.events.court-list-restricted"),
            entry(uk.gov.justice.listing.events.CourtRoomAssignedToHearing.class, "listing.events.court-room-assigned-to-hearing"),
            entry(uk.gov.justice.listing.events.CourtRoomChangedForHearing.class, "listing.events.court-room-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.CourtRoomRemovedFromHearing.class, "listing.events.court-room-removed-from-hearing"),
            entry(uk.gov.justice.listing.events.CreateNextHearingRequested.class, "listing.events.create-next-hearing-requested"),
            entry(uk.gov.justice.listing.events.CreatedListingNote.class, "listing.events.created-listing-note"),
            entry(uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule.class, "listing.events.crown-hearing-migrated-to-courtschedule"),
            entry(uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated.class, "listing.events.defendant-court-proceedings-updated"),
            entry(uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated2.class, "listing.events.defendant-court-proceedings-updated2"),
            entry(uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2.class, "listing.events.defendant-court-proceedings-updated-v2"),
            entry(uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated.class, "listing.events.defendant-legalaid-status-updated"),
            entry(uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing.class, "listing.events.defendant-legalaid-status-updated-for-hearing"),
            entry(uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings.class, "listing.events.defendants-to-be-added-for-court-proceedings"),
            entry(uk.gov.justice.listing.events.DefendantsToBeUpdated.class, "listing.events.defendants-to-be-updated"),
            entry(uk.gov.justice.listing.events.DefendantsToBeUpdatedLater.class, "listing.events.defendants-to-be-updated-later"),
            entry(uk.gov.justice.listing.events.DeleteNextHearingRequested.class, "listing.events.delete-next-hearing-requested"),
            entry(uk.gov.justice.listing.events.DeletedListingNote.class, "listing.events.deleted-listing-note"),
            entry(uk.gov.justice.listing.events.EndDateChangedForHearing.class, "listing.events.end-date-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.EndDateRemovedForHearing.class, "listing.events.end-date-removed-for-hearing"),
            entry(uk.gov.justice.listing.events.EndDateRemovedFromHearing.class, "listing.events.end-date-removed-from-hearing"),
            entry(uk.gov.justice.listing.events.HearingAddedToCase.class, "listing.events.hearing-added-to-case"),
            entry(uk.gov.justice.listing.events.HearingAllocatedForListing.class, "listing.events.hearing-allocated-for-listing"),
            entry(uk.gov.justice.listing.events.HearingAllocatedForListingV2.class, "listing.events.hearing-allocated-for-listing-v2"),
            entry(uk.gov.justice.listing.events.HearingChangesSaved.class, "listing.events.hearing-changes-saved"),
            entry(uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated.class, "listing.events.hearing-day-court-schedule-updated"),
            entry(uk.gov.justice.listing.events.HearingDaysCancelled.class, "listing.events.hearing-days-cancelled"),
            entry(uk.gov.justice.listing.events.HearingDaysChangedForHearing.class, "listing.events.hearing-days-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.HearingDaysSequenced.class, "listing.events.hearing-days-sequenced"),
            entry(uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected.class, "listing.events.hearing-days-without-court-centre-corrected"),
            entry(uk.gov.justice.listing.events.HearingDeleted.class, "listing.events.hearing-deleted"),
            entry(uk.gov.justice.listing.events.HearingLanguageChangedForHearing.class, "listing.events.hearing-language-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.HearingListed.class, "listing.events.hearing-listed"),
            entry(uk.gov.justice.listing.events.HearingListedCaseUpdated.class, "listing.events.hearing-listed-case-updated"),
            entry(uk.gov.justice.listing.events.HearingMarkedAsDeleted.class, "listing.events.hearing-marked-as-deleted"),
            entry(uk.gov.justice.listing.events.HearingMarkedAsDuplicate.class, "listing.events.hearing-marked-as-duplicate"),
            entry(uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase.class, "listing.events.hearing-marked-as-duplicate-for-case"),
            entry(uk.gov.justice.listing.events.HearingMarkedForPartialUpdate.class, "listing.events.hearing-marked-for-partial-update"),
            entry(uk.gov.justice.listing.events.HearingPartiallyUpdated.class, "listing.events.hearing-partially-updated"),
            entry(uk.gov.justice.listing.events.HearingRequestedForListing.class, "listing.events.hearing-requested-for-listing"),
            entry(uk.gov.justice.listing.events.HearingRescheduled.class, "listing.events.hearing-rescheduled"),
            entry(uk.gov.justice.listing.events.HearingResultStatusUpdated.class, "listing.events.hearing-result-status-updated"),
            entry(uk.gov.justice.listing.events.HearingTrialVacated.class, "listing.events.hearing-trial-vacated"),
            entry(uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved.class, "listing.events.hearing-unallocated-courtroom-removed"),
            entry(uk.gov.justice.listing.events.HearingUnallocatedForListing.class, "listing.events.hearing-unallocated-for-listing"),
            entry(uk.gov.justice.listing.events.HearingUpdatedToCase.class, "listing.events.hearing-updated-to-case"),
            entry(uk.gov.justice.listing.events.HearingsUpdateCompleted.class, "listing.events.hearings-update-completed"),
            entry(uk.gov.justice.listing.events.JudiciaryAssignedToHearing.class, "listing.events.judiciary-assigned-to-hearing"),
            entry(uk.gov.justice.listing.events.JudiciaryChangedForHearing.class, "listing.events.judiciary-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.JudiciaryChangedForHearingsStatus.class, "listing.events.judiciary-changed-for-hearings-status"),
            entry(uk.gov.justice.listing.events.JudiciaryRemovedFromHearing.class, "listing.events.judiciary-removed-from-hearing"),
            entry(uk.gov.justice.listing.events.JurisdictionChangedForHearing.class, "listing.events.jurisdiction-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated.class, "listing.events.laa-reference-for-application-updated"),
            entry(uk.gov.justice.listing.events.LinkedCasesToBeUpdated.class, "listing.events.linked-cases-to-be-updated"),
            entry(uk.gov.justice.listing.events.LinkedCasesUpdated.class, "listing.events.linked-cases-updated"),
            entry(uk.gov.justice.listing.events.ListingNoteEdited.class, "listing.events.listing-note-edited"),
            entry(uk.gov.justice.listing.events.MasterCaseUpdatedForGroup.class, "listing.events.master-case-updated-for-group"),
            entry(uk.gov.justice.listing.events.NewCaseMarkerUpdated.class, "listing.events.new-case-marker-updated"),
            entry(uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings.class, "listing.events.new-defendant-added-for-court-proceedings"),
            entry(uk.gov.justice.listing.events.NewDefendantDetailsUpdated.class, "listing.events.new-defendant-details-updated"),
            entry(uk.gov.justice.listing.events.NextHearingDayChanged.class, "listing.events.next-hearing-day-changed"),
            entry(uk.gov.justice.listing.events.NextHearingReplaced.class, "listing.events.next-hearing-replaced"),
            entry(uk.gov.justice.listing.events.NextHearingRequested.class, "listing.events.next-hearing-requested"),
            entry(uk.gov.justice.listing.events.NoHearingFoundForCourtApplication.class, "listing.events.no-hearing-found-for-court-application"),
            entry(uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing.class, "listing.events.non-default-days-assigned-to-hearing"),
            entry(uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing.class, "listing.events.non-default-days-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.class, "listing.events.non-sitting-days-assigned-to-hearing"),
            entry(uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.class, "listing.events.non-sitting-days-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.OffenceAdded.class, "listing.events.offence-added"),
            entry(uk.gov.justice.listing.events.OffenceDeleted.class, "listing.events.offence-deleted"),
            entry(uk.gov.justice.listing.events.OffenceUpdated.class, "listing.events.offence-updated"),
            entry(uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing.class, "listing.events.offences-removed-from-existing-allocated-hearing"),
            entry(uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing.class, "listing.events.offences-removed-from-existing-unallocated-hearing"),
            entry(uk.gov.justice.listing.events.OffencesRemovedFromHearing.class, "listing.events.offences-removed-from-hearing"),
            entry(uk.gov.justice.listing.events.OffencesToBeAdded.class, "listing.events.offences-to-be-added"),
            entry(uk.gov.justice.listing.events.OffencesToBeDeleted.class, "listing.events.offences-to-be-deleted"),
            entry(uk.gov.justice.listing.events.OffencesToBeUpdated.class, "listing.events.offences-to-be-updated"),
            entry(uk.gov.justice.listing.events.PublicListNoteChangedForHearing.class, "listing.events.public-list-note-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.PublicListNoteRemovedFromHearing.class, "listing.events.public-list-note-removed-from-hearing"),
            entry(uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested.class, "listing.events.remove-offences-from-existing-hearing-requested"),
            entry(uk.gov.justice.listing.events.ReportingRestrictionReasonChangedForHearing.class, "listing.events.reporting-restriction-reason-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.SequencesResetOnHearingDays.class, "listing.events.sequences-reset-on-hearing-days"),
            entry(uk.gov.justice.listing.events.StartDateChangedForHearing.class, "listing.events.start-date-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.StartDateRemovedForHearing.class, "listing.events.start-date-removed-for-hearing"),
            entry(uk.gov.justice.listing.events.TrialVacated.class, "listing.events.trial-vacated"),
            entry(uk.gov.justice.listing.events.TypeChangedForHearing.class, "listing.events.type-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.UnallocatedHearingDeleted.class, "listing.events.unallocated-hearing-deleted"),
            entry(uk.gov.justice.listing.events.UnscheduledNextHearingRequested.class, "listing.events.unscheduled-next-hearing-requested"),
            entry(uk.gov.justice.listing.events.UpdateExistingHearingRequested.class, "listing.events.update-existing-hearing-requested"),
            entry(uk.gov.justice.listing.events.VideoLinkChangedForHearing.class, "listing.events.video-link-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.VideoLinkDetailsAssignedForHearing.class, "listing.events.video-link-details-assigned-for-hearing"),
            entry(uk.gov.justice.listing.events.VideoLinkDetailsChangedForHearing.class, "listing.events.video-link-details-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.VideoLinkDetailsRemovedForHearing.class, "listing.events.video-link-details-removed-for-hearing"),
            entry(uk.gov.justice.listing.events.WeekCommencingDateChangedForHearing.class, "listing.events.week-commencing-date-changed-for-hearing"),
            entry(uk.gov.justice.listing.events.WeekCommencingDateRemovedForHearing.class, "listing.events.week-commencing-date-removed-for-hearing"));

    private PrivateEventNames() {
    }

    public static String nameOf(final Object event) {
        final String name = NAMES.get(event.getClass());
        if (name == null) {
            throw new IllegalArgumentException("Not a listing domain event: " + event.getClass().getName());
        }
        return name;
    }
}
