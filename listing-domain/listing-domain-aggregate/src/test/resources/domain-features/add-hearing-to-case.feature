Feature: Send Case for Listing

  Scenario: A hearing is added to a case so that the hearing can be managed in the future

    Given no previous events
    When you addHearing to a Case using a hearing to be added to case
    Then hearing added to case

Scenario: A further hearing is added to a case

  Given hearing added to case
  When you addHearing to a Case using a hearing to be added to case
  Then hearing added to case
