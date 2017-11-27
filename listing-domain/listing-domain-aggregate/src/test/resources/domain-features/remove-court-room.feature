Feature: Remove a court-room from a hearing

  Scenario: An unallocated hearing has been listed and a court-room assigned. A request to remove the
            court-room results in the court-room being removed from the hearing

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing
    When removeCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using hearing-id
    Then the court-room-removed-from-hearing

  Scenario: An unallocated hearing has been listed and no court-room has been assigned. A request to remove
            a court-room from the hearing does not result in any change

    Given there are previous events unallocated-hearing-listed
    When removeCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using hearing-id
    #Then the no events occurred