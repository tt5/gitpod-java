Feature: Post analysis tasks

  @isolated
  Scenario: Post invalid structured task
    When I post the content from "EvaluationRequestInvalid.json" to "/api/tasks"
    Then I receive a response with status 400
    And the response contains the text "Either Depth or Duration must be given"

  @isolated
  Scenario: Post incorrect task
    When I post the content from "EvaluationRequestInvalidEngine.json" to "/api/tasks"
    Then I receive a response with status 400
    And the response contains the text "Engine not found"

  @isolated
  Scenario: Run task for Dragon with given duration
    Given I listen to messages in the exchange "evaluation.created"

    When I submit a task for game "1. e4 e5 2. Nf3 Nc6 3. Bb5" with 3 variations and the name "for dragon" and the duration "PT5S" to engine "dragon"
    Then I will find this task in the list of tasks

    Then I will eventually get a message on "evaluation.created"
    And the message has an element "status" equal to "NOT_ACTIVE"
    And the message has an element "taskName" equal to "for dragon"


  @isolated
  Scenario: Run task for Stockfish, and stop
    Given I listen to messages in the exchange "evaluation.created"
    When I start an evaluation for "EvaluationRequestStockfish.json"
    Then I will find this task in the list of tasks
    Then I can get details for this task
    And the task has the status "ACTIVE"

    When I stop this task
    And I can get details for this task
    Then the task has the status "NOT_ACTIVE"
    And the json element hostname is equal to localtest
    And I will eventually get a message on "evaluation.created"
    And the message has an element "taskName" equal to "nameOfStockfishGame"


  @isolated
  Scenario: Post task with expected outcome and check json content
    When I post the content from "EvaluationRequestStockfishMateIn2.json" to "/api/tasks"
    Then I receive a response with status 202

    When I wait for 2 seconds
    And I stop all tasks
    Then I receive a response with status 200

    When I get "/api/tasks/current" from service
    Then I receive a response with status 200
    And the response body has the json content of "EvaluationMessageStockfishMateIn2.json" with regex in fields
      | evaluation.created                     |
      | evaluation.uciState.kiloNodes          |
      | evaluation.uciState.kiloNodesPerSecond |
      | latestEvents[depth=30].occurredOn      |
      | taskStarted                            |
      | lastEvaluation                         |
      | lastAlive                              |
    And the json element history[0] is equal to /^d=.*/

  @isolated
  Scenario: Post task for FEN and check json content
    When I post the content from "EvaluationRequestByFen.json" to "/api/tasks"
    Then I receive a response with status 202

    When I wait for 2 seconds
    And I stop all tasks
    When I get "/api/tasks/current" from service

    Then I receive a response with status 200
    And the response body has the json content of "EvaluationMessageByFen.json" with regex in fields
      | evaluation.created |

  @isolated
  Scenario: Reduce PV
    Given I listen to messages in the exchange "evaluation.created"

    When I post the content from "EvaluationRequestMultiPv.json" to "/api/tasks"
    Then I receive a response with status 202

    Then I will eventually get a message on "evaluation.created"
    When I get "/api/tasks/current" from service
    Then I receive a response with status 200
    And the json element evaluation.variations[0].depth is equal to 25
    And the json element evaluation.variations[4].depth is equal to 22
