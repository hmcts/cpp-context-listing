Feature: Remove a judiciary from a hearing

  Scenario: An unallocated hearing has been listed and a judiciary assigned. A request to remove the
            judiciary results in the judiciary being removed from the hearing

    Given hearing listed
      And judiciary assigned to hearing
    When you removeJudiciary from a Hearing using a hearing-id
    Then judiciary removed from hearing


  Scenario: An unallocated hearing has been listed and no judge has been assigned. A request to remove
            a judge from the hearing does not result in any change

    Given hearing listed
    When you removeJudiciary from a Hearing using a hearing id
    Then no events occured