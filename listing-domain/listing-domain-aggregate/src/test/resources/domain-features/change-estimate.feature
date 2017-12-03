Feature: Change the estimate for the hearing

  Scenario: An unallocated hearing has been listed and changing the original estimate to a
            different estimate results in the hearing estimate being changed

    Given there are previous events unallocated-hearing-listed
    When changeEstimate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-different-estimate
    Then the estimate-minutes-changed-for-hearing


  Scenario: An unallocated hearing has been listed and requesting the original estimate to
            be changed to the same estimate does not result in any change

    Given there are previous events unallocated-hearing-listed
    When changeEstimate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-estimate
    #Then the no events occurred


  Scenario: A request to change the estimate for a hearing that has not been listed does not result in any change

    Given no previous events
    When changeEstimate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-ten-minute-estimate
    #Then the no events occurred
