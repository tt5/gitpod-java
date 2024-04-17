# Dockfish
A restful wrapper around the UCI chess engine stockfish. Run stockfish (and other chess engines) in docker container.

[![pipeline status](https://gitlab.com/ce72/dockfish/badges/master/pipeline.svg)](https://gitlab.com/ce72/dockfish/-/commits/master)
[![coverage report](https://gitlab.com/ce72/dockfish/badges/master/coverage.svg)](https://gitlab.com/ce72/dockfish/commits/master)
[![Release](https://gitlab.com/ce72/dockfish/-/badges/release.svg)](https://gitlab.com/ce72/dockfish/-/releases)

[View it on Gitlab][https://gitlab.com/ce72/dockfish]

The purpose of this project is to provide a docker image which runs a single stockfish process analyzing a
chess position. Dockfish doesn't provide any queuing. Everything but starting and stopping a chess engine and providing
analysis results is up to the client application.

This project includes source code from https://github.com/Raptor-Fics-Interface/Raptor.

This project includes a binary copy of the linux Stockfish engine (https://stockfishchess.org/).

This project includes a binary copy of the linux Komodo Dragon engine (https://komodochess.com/).


## Build with maven
```shell
mvn clean install
```
### Optional: perform integration-test
```shell
mvn clean install -Pintegration-test
```

## Run local docker container

```shell
docker run -d -p8080:8080 ce72/dockfish
```
Hint: you can add e.g. "-e TZ=Europe/Berlin" to the docker run statement to set the timezone of the java process.

### Alternative: simply pull the image from gitlab registry
No need to build:
```shell
docker run --name dockfish -d -p 8080:8080 registry.gitlab.com/ce72/dockfish:latest
```

# Perform Analysis
## Submit task
```shell
curl -H "Content-type: application/json" -X POST -d '{"name" : "test", "pgn": "1. e4 e5 2. Nf3", "initialPv": 3, "maxDuration": "PT0H10M"}' localhost:8080/api/tasks
```

## Get results
```
curl -v localhost:8080/api/tasks
Response: {id:"f2e...", ..., status:"ACTIVE"}
```

```
curl localhost:8080/api/tasks/f2e
```

## Stop task (before it finishes automatically)
```
curl localhost:8080/api/tasks/f2e/stop
```

## Description of Submit Task Format
When posting a new Task the following parameters must be provided:
See [example](/util/game.json)
```json
{
  "name": "xy",
  "pgn": "1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 4.Ba4 Nf6 5.O-O b5 6.Bb3 Bc5 7.a4 Bb7 8.c3 d6 9.d4 Bb6 10.Bg5 h6 11.Bxf6 Qxf6 12.Bd5 O-O 13.Na3 exd4 14.cxd4 Ra7 15.Nc2 bxa4 16.Rxa4 a5 *",
  "initialPv": 3,
  "maxDuration": "PT4H30M",
  "useSyzygyPath" : true,
  "options": [
    {
      "name": "Threads",
      "value": "1"
    },
    {
      "name": "Hash",
      "value": "800"
    }
  ],
  "dynamicPv": {
    "requiredDepth": 30,
    "cutOffCentiPawns": 20,
    "keepMinPv":2
  }
}
```

| Attribute | Description |
| --------- | ---------- |
| name      | arbitrary  |
| pgn       | chess game notation. dockfish will evaluate the position after the last move |
| initialPV | number of principal variations to start with |
| maxDepth  | calculate until this analysis depth is reached. Exactly one of maxDepth and maxDuration must be given |
| maxDuration | calculate for the given duration (e.g. PT2H10S). Exactly one of maxDepth and maxDuration must be given |
| useSyzygyPath | If true then the value of the environment variable "uci_option_SyzygyPath" will be passed as SyzygyPath to the engine. (If false the environment variable will be ignored.) Note: if running in docker, the SyzygyPath must be mounted as volume (see docker-compose.yml)|
| options     | (optional) any UCI options (name, value) which the engine will understand |
| dynamicPv  | (optional) when 'requiredDepth' is reached, the engine will only continue variations that are not worse than 'cutOffCentipawns', but will keep at least 'keepMinPv' variations. |

See also [SubmitTaskCommand.java](application/src/main/java/ce/chess/dockfish/adapter/in/dto/SubmitTaskCommand.java)


# Messaging with Rabbit MQ
Dockfish listens to an exchange for tasks, The message body must be of the same JSON format as described above.

|  |  |
| --------------- | --------- |
| rabbitmq.host   | rabbitmq  |
| rabbitmq.port   | 5267  |
| Exchange name   | dockfish.tasks |
| Topic           | "" |
| (default queue) | submitted |
| body content    | SubmitTaskCommand (json) |

When a task is finished, Dockfish sends an AMQP message: The message body contains an evaluation like as in the Rest API described above. This message will also be send, if the task was triggered by a Rest call.

|  |  |
| --------------- | --------- |
| rabbitmq.host   | rabbitmq  |
| rabbitmq.port   | 5267  |
| Exchange name   | dockfish.evaluations |
| Topic           | "" |
| (default queue) | finished |
| body content    | Evaluation (json) |

The parameters of the Rabbit connection can be configured with the environment variables "rabbitmq.host" and
"rabbitmq.port".

# Simple Web form
You can point your browser to
```shell
http://localhost:8080/dockfish.html
```
and obtain a simple webform to submit an analysis task.



[https://gitlab.com/ce72/dockfish]: https://gitlab.com/ce72/dockfish
