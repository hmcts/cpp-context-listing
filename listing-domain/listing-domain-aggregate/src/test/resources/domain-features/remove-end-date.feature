Feature: Remove an end-date from a hearing

  Scenario: An unallocated hearing has been listed. A request to remove the
            end-date results in the end-date being removed from the hearing

    Given hearing listed
    When you removeEndDate from a Hearing using a hearing id
    Then end date removed from hearing
