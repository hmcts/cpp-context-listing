Feature: Add court application on existing hearing following public event from Progression

  Scenario: Court application is added on existing hearing following a public event from the Progression context

    Given hearing listed
    When you addCourtApplication to a Hearing with an add court application for hearing
    Then court application added for hearing
