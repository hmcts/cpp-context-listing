Feature: Assign a start time to a hearing or change the start time

  Scenario: An unallocated hearing has been listed and assigning a start time results in the
            start time being assigned to the hearing

    Given there are previous events unallocated-hearing-listed
    When assignStartTime to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-start-time
    Then the start-time-assigned-to-hearing

  Scenario: An unallocated hearing has been listed and a start time assigned. Changing the start time
            to a different start time results in the start time being changed for hearing

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing
    When assignStartTime to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-different-start-time
    Then the start-time-changed-for-hearing

  Scenario: An unallocated hearing has been listed and a start time assigned. Requesting the original
            start time to be changed to the same start time does not result in any change

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing
    When assignStartTime to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-start-time
    #Then the no events occurred