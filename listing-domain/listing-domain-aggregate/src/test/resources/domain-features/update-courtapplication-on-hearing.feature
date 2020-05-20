Feature: Update court application on future hearing following public event from Progression

  Scenario: Court application is updated on future hearing following a public event from the Progression context

    Given hearing listed with application
    When you updateCourtApplication to a Hearing with a changed court application
    Then court application details updated


  Scenario: When a hearing is listed, application is added to hearing

    Given hearing listed with application
    When you addToHearing to a Application with a court application hearing
    Then court application added to hearing

  Scenario: When an update is received for a court application following a public event from the Progression context

    Given court application added to hearing
    When you update to a Application with a court application
    Then court application to be updated

  Scenario: When an update is received for a court application which has no hearing associated

    Given hearing listed with application
    When you update to a Application with a court application
    Then raises no hearing found for court application
