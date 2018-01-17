Feature: Remove a judge from a hearing

  Scenario: An unallocated hearing has been listed and a judge assigned. A request to remove the
            judge results in the judge being removed from the hearing

    Given unallocated-hearing-listed,judge-assigned-to-hearing
    When you removeJudge to a Hearing using a hearing-id
    Then judge-removed-from-hearing


  Scenario: An unallocated hearing has been listed and no judge has been assigned. A request to remove
            a judge from the hearing does not result in any change

    Given unallocated-hearing-listed
    When you removeJudge to a Hearing using a hearing-id
    Then no events occured