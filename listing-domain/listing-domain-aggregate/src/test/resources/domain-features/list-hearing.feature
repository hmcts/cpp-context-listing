Feature: List a hearing

  Scenario: A request to list a hearing results in an unallocated hearing being listed

    Given no previous events
    When list to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-new-hearing
    Then the unallocated-hearing-listed


  Scenario: A request to list a hearing that has already been listed does not result in a new listing

    Given there are previous events unallocated-hearing-listed
    When list to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-hearing
    #Then the no events occurred