Feature: Change the start date of the hearing

  Scenario: An unallocated hearing has been listed and changing the original start date to
            a different start date results in the hearing start date being changed

    Given there are previous events unallocated-hearing-listed
    When changeStartDate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-different-start-date
    Then the start-date-changed-for-hearing

  Scenario: An unallocated hearing has been listed and requesting the original start date to
            be changed to the same start date does not result in any change

    Given there are previous events unallocated-hearing-listed
    When changeStartDate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-start-date
    #Then the no events occurred

  Scenario: A request to change the start date for a hearing that has not been listed does not result in any change

    Given no previous events
    When changeStartDate to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-start-date
    #Then the no events occurred
