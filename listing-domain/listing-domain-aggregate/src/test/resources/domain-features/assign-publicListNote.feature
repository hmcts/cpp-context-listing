Feature: Assign a publicListNote to a hearing or change the hasVideoLink

  Scenario: An unallocated hearing has been listed. Add a public list note to an unallocated hearing. Update a public list note on an unallocated hearing.

    Given hearing listed
    When you assignPublicListNote to a Hearing using a new publicListNote
    Then publicListNote assigned to hearing

  Scenario: An unallocated hearing has been listed and the publicListNote being assigned and then the publicListNote changed

    Given hearing listed
    And publicListNote assigned to hearing
    When you assignPublicListNote to a Hearing using a different publicListNote
    Then publicListNote changed for hearing

  Scenario: An unallocated hearing has been listed and the publicListNote being assigned and then the publicListNote removed

    Given hearing listed
    And publicListNote assigned to hearing
    When you assignPublicListNote from a Hearing using a remove publicListNote
    Then publicListNote removed from hearing

  Scenario: An unallocated hearing has been listed and the publicListNote being assigned and then same publicListNote received

    Given hearing listed
    And publicListNote assigned to hearing
    When you assignPublicListNote to a Hearing using a new publicListNote
    Then no events occurred