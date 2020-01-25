Feature: Allocate a hearing

  Scenario: An unallocated hearing has been listed. However, a courtroom has not been assigned
            which is mandatory for allocation. Applying allocation rules does not result in any change.

    Given hearing listed
    When you applyAllocationRules to a Hearing
    Then no events occurred


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned and non default days
            have been assigned. Applying allocation rules results in hearing being allocated.

    Given hearing listed
      And court room assigned to hearing
      And non default days assigned to hearing
      And hearing days changed for hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing

  Scenario: An unallocated hearing has been listed and a courtroom has been assigned which is a mandatory
            requirement for allocation. However, the courtroom is then removed. Applying allocation
            rules results does not result in any change.

    Given hearing listed
      And court room assigned to hearing
      And court room removed from hearing
      And hearing days changed for hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then no events occurred


  Scenario: A hearing has been listed with the necessary fields for allocation

    Given hearing listed with data for allocation
    And hearing days changed for hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing
