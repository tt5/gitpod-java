Feature: Receive and publish messages

  @isolated
  Scenario: Receive and handle static analysis request as message
    Given I listen to messages in the exchange "staticEvaluation.created"

    When a message with the content from "StaticEvaluationRequest.json" was published to the exchange "staticEvaluationRequest.submitted"

    Then I will eventually get a message on "staticEvaluation.created"
    And the message has an element "evaluation" containing "Total | "

  @isolated
  Scenario: Receive and handle evaluation task
    Given I listen to messages in the exchange "evaluation.created"

    When a message with the content from "EvaluationRequestDefaultEngine.json" was published to the exchange "task.submitted" with the routing key "windows"

    Then I will eventually get a message on "evaluation.created"
    And the message has an element "status" equal to "NOT_ACTIVE"
    And the message has an element "taskName" equal to "nameOfDefaultGame"
    And the message has an element "uciEngineName" containing "Stockfish"
    And the message has an element "evaluation.variations.size()" equal to "4"
    And the message has an element "latestEvents.size()" equal to "4"

  @isolated
  Scenario: Receive invalid task and do not block
    Given I listen to messages in the exchange "evaluation.created"

    When a message with the content from "EvaluationRequestInvalidEngine.json" was published to the exchange "task.submitted" with the routing key "both"
    And I wait for 2 seconds

    When I submit a task for game "1. e4 e5 2. Nf3 Nc6 3. Bb5 a5" with 2 variations and the name "invalidMessageTest" and the duration "PT60S" to engine "stockfish"
    Then I receive a response with status 202
    And the json element status is equal to ACTIVE
    And the json element taskName is equal to invalidMessageTest

    When I stop this task
    Then I will eventually get a message on "evaluation.created"
    And the message has an element "taskName" equal to "invalidMessageTest"
