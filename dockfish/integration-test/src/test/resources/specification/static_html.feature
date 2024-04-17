Feature: Test static html

  Scenario: It provides a html form for entering a position
    When I get "dockfish.html" from service
    Then I receive a response with status 200
    And the content type is text/html
    And the response contains the text "Enter Position"
