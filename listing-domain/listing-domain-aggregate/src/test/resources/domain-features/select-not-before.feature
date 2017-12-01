Feature: Select 'not before' start time for the hearing

  Scenario: An unallocated hearing has been listed and the start time has been assigned.
            Selecting 'not before' results in 'not before' being selected for hearing

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing
    When selectNotBefore to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using selected-true
    Then the not-before-selected-for-hearing

  Scenario: An unallocated hearing has been listed, the start time has been assigned and 'not before' selected to true.
            Requesting 'not before' selected to be changed to false results in 'not before' being unselected for hearing

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing,not-before-selected-for-hearing
    When selectNotBefore to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using selected-false
    Then the not-before-unselected-for-hearing

  Scenario: An unallocated hearing has been listed, the start time has been assigned and 'not before' selected.
            Requesting 'not before' selected to be changed to the same value does not result in any change

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing,not-before-selected-for-hearing
    When selectNotBefore to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using selected-true
    #Then the no events occurred

  Scenario: An unallocated hearing has been listed and the start time has not been assigned.
            Selecting 'not before' does not result in any change

    Given there are previous events unallocated-hearing-listed
    When selectNotBefore to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using selected-true
    #Then the no events occurred