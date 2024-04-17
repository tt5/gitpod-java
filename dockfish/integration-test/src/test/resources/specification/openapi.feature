Feature: Test openapi documentation

  Scenario: It provides openapi documentation
    When I get "/q/openapi" from service
    Then I receive a response with status 200
    And the response contains the text "summary: Get information about all tasks"
    And the response contains the text "summary: Post an analysis"
    And the response contains the text "UciStateDto:"
    And the response contains the text "VariationDto:"
    And the response contains the text "kiloNodes:"
    And the response contains the text "moves:"
    And the response contains the text "example: 1.d4 d5 2.c4 *"
