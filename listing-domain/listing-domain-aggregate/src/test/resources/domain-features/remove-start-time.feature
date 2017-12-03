Feature: Remove a start time from a hearing

  Scenario: An unallocated hearing has been listed and a start time assigned. A request to remove the
            start time results in the start time being removed from the hearing

    Given there are previous events unallocated-hearing-listed,start-time-assigned-to-hearing
    When removeStartTime to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using hearing-id
    Then the start-time-removed-from-hearing


  Scenario: An unallocated hearing has been listed and no start time has been assigned. A request to remove
            a start time from the hearing does not result in any change

    Given there are previous events unallocated-hearing-listed
    When removeStartTime to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using hearing-id
    #Then the no events occurred