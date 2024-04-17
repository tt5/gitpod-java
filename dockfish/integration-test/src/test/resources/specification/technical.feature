@Technical
Feature: Test technical features

  Scenario: It can be pinged
    When I get "/api/ping" from service
    Then I receive a response with status 200

  Scenario: It provides a favicon
    When I get "/favicon.ico" from service
    Then I receive a response with status 200

  Scenario: Expose prometheus endpoint
    When I get "/q/metrics" from service
    Then I receive a response with status 200
    And the response contains the text "system_cpu_count{application=\"dockfish\""
    And the response contains the text "jvm_buffer_memory_used_bytes{application=\"dockfish\""
    And the response contains the text "log_total{application=\"dockfish\",level=\"INFO\""
    And the response contains the text "guava_cache_size{application=\"dockfish\""
    And the response contains the text "task_time_remaining_seconds{application=\"dockfish\",scope=\"application\""

  Scenario: Expose health endpoint
    When I get "/q/health" from service
    Then I receive a response with status 200

