Feature: Change the type of the hearing

  Scenario: An unallocated hearing has been listed and changing the original type to
            a different type results in the hearing type being changed

    Given there are previous events unallocated-hearing-listed
    When changeType to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-different-type
    Then the type-changed-for-hearing

  Scenario: An unallocated hearing has been listed and requesting the original type to
            be changed to the same type does not result in any change

    Given there are previous events unallocated-hearing-listed
    When changeType to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using the-same-type
    #Then the no events occurred

  Scenario: A request to change the type for a hearing that has not been listed does not result in any change

    Given no previous events
    When changeType to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using a-trial-type
    #Then the no events occurred
