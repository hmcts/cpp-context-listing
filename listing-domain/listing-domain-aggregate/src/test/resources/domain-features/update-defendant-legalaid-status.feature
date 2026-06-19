Feature: Update Defendant Legal Aid Status

  Scenario: An  hearing has been listed. When the defendant legal Aid Status is Updated then event is raised

    Given hearing added to case
    When you updateDefendantLegalAidStatus on a Case using a defendant and LegalAidStatus
    Then defendant legalAidStatus is updated