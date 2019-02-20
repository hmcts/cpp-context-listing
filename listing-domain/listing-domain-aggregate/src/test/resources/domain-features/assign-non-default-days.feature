Feature: Assign non default days to a hearing or change the start times

  Scenario: An unallocated hearing has been listed and assigning non default days results in the
            non default days being assigned to the hearing

    Given hearing listed
    When you assignNonDefaultDays to a Hearing using a non default days
    Then non default days assigned to hearing


  Scenario: An unallocated hearing has been listed and a non default days assigned. Changing the
            non default days to a different non default days results in the non default days being changed for hearing

    Given hearing listed
      And non default days assigned to hearing
    When you assignNonDefaultDays to a Hearing using a different non default days
    Then non default days changed for hearing


  Scenario: An unallocated hearing has been listed and non default days assigned. Requesting the original
            non default days to be changed to the same non default days does not result in any change

    Given hearing listed
      And non default days assigned to hearing
    When you assignNonDefaultDays to a Hearing using a the same non default days
    Then no events occured