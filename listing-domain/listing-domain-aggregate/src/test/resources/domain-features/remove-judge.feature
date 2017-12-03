Feature: Remove a judge from a hearing

  Scenario: An unallocated hearing has been listed and a judge assigned. A request to remove the
            judge results in the judge being removed from the hearing

    Given there are previous events unallocated-hearing-listed,judge-assigned-to-hearing
    When removeJudge to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using hearing-id
    Then the judge-removed-from-hearing


  Scenario: An unallocated hearing has been listed and no judge has been assigned. A request to remove
            a judge from the hearing does not result in any change

    Given there are previous events unallocated-hearing-listed
    When removeJudge to a uk.gov.moj.cpp.listing.domain.aggregate.Hearing using hearing-id
    #Then the no events occurred