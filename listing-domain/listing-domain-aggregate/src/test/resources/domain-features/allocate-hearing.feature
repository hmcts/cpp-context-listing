Feature: Allocate a hearing

  Scenario: An unallocated hearing has been listed and a courtroom has been assigned. However, a judge
            has not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given hearing listed
      And court room assigned to hearing
    When you applyAllocationRules to a Hearing
    Then no events occured


  Scenario: An unallocated hearing has been listed and a judge has been assigned. However, a court
            room not been assigned which is mandatory for allocation. Applying allocation rules does
            not result in any change.

    Given hearing listed
      And judge assigned to hearing
    When you applyAllocationRules to a Hearing
    Then no events occured


  Scenario: An unallocated hearing has been listed, a courtroom has been assigned, a judge has been assigned and
             start times have been assigned. Applying allocation rules results in hearing being allocated

    Given hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
      And end date assigned to hearing
      And start times assigned to hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing

  Scenario: An unallocated hearing has been listed, a courtroom has been assigned and a judge has been assigned which
            are all the mandatory requirements for allocation. However, the judge is then removed. Applying allocation
            rules results does not result in any change.

    Given hearing listed
      And court room assigned to hearing
      And judge assigned to hearing
      And end date assigned to hearing
      And court room removed from hearing
    When you applyAllocationRules to a Hearing using a no-args
    Then no events occured


  Scenario: A hearing has been listed with the necessary fields for allocation

    Given hearing listed with data for allocation
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing




  Scenario: A hearing has been listed with the necessary fields for allocation

    Given hearing listed with data for allocation
    When you applyAllocationRules to a Hearing using a no-args
    Then hearing allocated for listing


