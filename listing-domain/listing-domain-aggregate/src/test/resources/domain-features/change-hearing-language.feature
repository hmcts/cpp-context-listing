Feature: Change the type of the hearing

  Scenario: An unallocated hearing has been listed and changing the original type to
            a different type results in the hearing type being changed

    Given hearing listed
    When you changeHearingLanguage to a Hearing using a different hearing language
    Then hearing language changed for hearing


  Scenario: An unallocated hearing has been listed and requesting the original type to
            be changed to the same type does not result in any change

    Given hearing listed
    When you changeHearingLanguage to a Hearing using a the same hearing language
    Then no events occurred


  Scenario: A request to change the type for a hearing that has not been listed does not result in any change

    Given no previous events
    When you changeHearingLanguage to a Hearing using a english hearing language
    Then no events occurred
