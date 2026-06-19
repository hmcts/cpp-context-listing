Feature: A listing officer should be able to add, edit, and delete notes per hearing date per court room id
  on allocated and hearing search pages

  Scenario: A listing officer should be able to delete a previously created note
    # GIVEN : listing-note-created.json
    Given listing note created
    # WHEN : ListingNote.deleteNote(..) aggregate method is called using delete-listing-note-requested.json input
    When you deleteNote to a ListingNote using a delete listing note requested
    # THEN : deleted-listing-note.json event is raised
    Then deleted-listing-note

  Scenario: A listing officer should not be able to delete a note which was not created
    Given no previous events
    When you deleteNote to a ListingNote using a delete listing note requested
    Then no events occurred