Feature: Assign a court-room to a hearing or change the court-room

  Scenario: An unallocated hearing has been listed and assigning a court-room results in the
            court-room being assigned to the hearing

    Given there are previous events unallocated-hearing-listed
    When assignCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-court-room
    Then the court-room-assigned-to-hearing


  Scenario: An unallocated hearing has been listed and a court-room assigned. Changing the court-room
            to a different court-room results in the court-room being changed for hearing

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing
    When assignCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-different-court-room
    Then the court-room-changed-for-hearing


  Scenario: An unallocated hearing has been listed and a court-room assigned. Requesting the original
            court-room to be changed to the same court-room does not result in any change

    Given there are previous events unallocated-hearing-listed,court-room-assigned-to-hearing
    When assignCourtRoom to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-court-room
    #Then the no events occurred