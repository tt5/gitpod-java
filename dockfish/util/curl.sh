#!/bin/bash
HOST=localhost:8080

curl -v -H "Content-Type: application/json" -d @game.json -X POST http://$HOST/api/tasks

curl -v -H "Content-Type: application/json" http://$HOST/api/tasks
