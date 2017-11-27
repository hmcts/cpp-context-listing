Feature: Assign a judge to a hearing or change the judge

  Scenario: An unallocated hearing has been listed and assigning a judge results in the
            judge being assigned to the hearing

    Given there are previous events unallocated-hearing-listed
    When assignJudge to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using a-judge
    Then the judge-assigned-to-hearing

  Scenario: An unallocated hearing has been listed and a judge assigned. Changing the judge
            to a different judge results in the judge being changed for hearing

    Given there are previous events unallocated-hearing-listed,judge-assigned-to-hearing
    When assignJudge to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using a-different-judge
    Then the judge-changed-for-hearing

  Scenario: An unallocated hearing has been listed and a judge assigned. Requesting the original
            judge to be changed to the same judge does not result in any change

    Given there are previous events unallocated-hearing-listed,judge-assigned-to-hearing
    When assignJudge to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using the-same-judge
    #Then the no events occurred

  Scenario: A request to assign a judge to a hearing that has not been listed does not result in any change

    Given no previous events
    When assignJudge to a uk.gov.moj.cpp.listing.domain.aggregate.HearingAggregate using a-judge
    #Then the no events occurred