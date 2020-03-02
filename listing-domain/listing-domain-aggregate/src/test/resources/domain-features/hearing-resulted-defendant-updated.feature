Feature: Case updated and hearing resulted

  Scenario: A hearing has been listed. When the case is updated and hearing resulted then listing.events.case-resulted-and-defendant-proceedings-concluded with be raised.

    Given hearing added to case
    When you updateDefendantCaseResultedAndUpdated on a Case using a prosecutionCase
    Then get hearingIds for prosecutionCase

  Scenario: An unallocated hearing has been listed. When the case is updated and hearing resulted and no hearingId passed then no event is raised

    Given no previous events
    When you updateDefendantCaseResultedAndUpdated on a Case using a prosecutionCase
    Then no events occurred

  Scenario: A hearing has been listed. When the case is updated and hearing resulted then listing.events.case-update-defendant-proceedings-updated will be raised.

    Given hearing added to case
    When you updateDefendantProceedingConcludedForHearing on a Hearing using a hearingId and prosecutionCase
    Then update defendant proceedings

  Scenario: A hearing has been listed in the past. When the case is updated and hearing resulted then no event is raised.
    Given hearing listed in the past
    When you updateDefendantProceedingConcludedForHearing on a Hearing with a hearing in past and prosecutionCase
    Then no events occurred