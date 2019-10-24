Feature: Change the week commencing start/end date of the hearing

  Scenario: An unallocated hearing has been listed and changing the start/end date to week commencing date
            results in the hearing week commencing date being added.

    Given hearing listed
    When you changeWeekCommencingDate to a Hearing using a week commencing date
    Then week commencing date added for hearing


  Scenario: An unallocated hearing has been listed and requesting the original week commencing date to
            be changed to the same week commencing date does not result in any change.

    Given week commencing date added for hearing
    When you changeWeekCommencingDate to a Hearing using a the same week commencing date
    Then no events occurred


  Scenario: An unallocated hearing has been listed and requesting the original week commencing date to
            be changed to different week commencing date does result to week commencing date changed.

    Given week commencing date added for hearing
    When you changeWeekCommencingDate to a Hearing using a different week commencing date
    Then week commencing date changed for hearing

  Scenario: An unallocated hearing has been listed and a week commencing added. A request to remove the
            week commencing results in the week commencing being removed from the hearing

    Given hearing listed
    And week commencing date added for hearing
    When you removeWeekCommencingDates from a Hearing using a hearing id
    Then week commencing date removed from hearing

