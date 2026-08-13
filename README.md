NEXUS
Fault-Tolerant Task Orchestration & System Health Platform

NEXUS is a fault-tolerant task orchestration platform designed to process tasks reliably while monitoring worker health, handling failures, retrying failed tasks, and maintaining an auditable event timeline.

It also provides an operator-facing dashboard for real-time visibility into workers, tasks, failures, retries, and system health.

Overview
NEXUS helps operators submit and track tasks, observe worker status, and understand how the system behaves during failures.
It is built to remain operational even when individual workers fail, while keeping a clear history of task execution.

Features
Task Orchestration
Submit tasks through REST APIs.

Generate unique task IDs using UUIDs.

Support idempotency keys.

Track the full task lifecycle.

Process tasks concurrently.

Monitor task status in real time.

Fault Tolerance
Detect failures automatically.

Retry failed tasks with configurable attempts.

Schedule retries.

Handle worker restarts.

Enforce maximum restart limits.

Move exhausted tasks to a dead-letter state.

Simulate failures for testing.

Detect workers that go out of service.

Multiple Workers
Support multiple workers running concurrently.

Maintain worker-specific health data:

Worker ID

Maximum restarts

Failure count

Restart count

Last heartbeat

Current status

Worker Health Monitoring
Track periodic heartbeats from workers.

Identify unhealthy workers.

Restart workers according to policy.

Expose worker states such as:

RUNNING

RESTARTING

STOPPED

OUT_OF_SERVICE

System Health Monitoring
The operator dashboard exposes:

Total tasks

Accepted tasks

Processing tasks

Completed tasks

Retrying tasks

Dead-letter tasks

Failure mode

Active workers

Worker health

Event Timeline
NEXUS records an auditable event history for each task.
Events include:

TASK_ACCEPTED

TASK_STARTED

TASK_COMPLETED

TASK_FAILED

TASK_RETRY_SCHEDULED

TASK_DEAD_LETTERED

Failure Simulation
NEXUS supports failure simulation so operators can test:

Retry behavior

Worker failure handling

Worker restart logic

Dead-letter processing

Recovery behavior

Worker health monitoring

Failure mode options:

NORMAL

FAIL

Architecture
text

                    ┌──────────────────────┐
                    │      Operator UI     │
                    │   React + TypeScript  │
                    └──────────┬───────────┘
                               │
                               │ REST API
                               ▼
                    ┌──────────────────────┐
                    │    Spring Boot API   │
                    │      NEXUS Core      │
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
        ┌──────────┐      ┌──────────┐    ┌──────────┐
        │ Worker 1 │      │ Worker 2 │    │ Worker N │
        └──────────┘      └──────────┘    └──────────┘
              │                │                │
              └────────────────┼────────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      PostgreSQL      │
                    │ Tasks                │
                    │ Workers              │
                    │ Events               │
                    └──────────────────────┘
Technology Stack
Backend
Java 21

Spring Boot

Spring Web

Spring Data JPA

Hibernate

Maven

PostgreSQL

Frontend
React

TypeScript

Vite

CSS

Development Tools
Git

GitHub

IntelliJ IDEA / VS Code

PowerShell

Project Structure
text

NEXUS/
│
├── frontend/
│   ├── src/
│   │   ├── App.tsx
│   │   ├── App.css
│   │   ├── index.css
│   │   └── main.tsx
│   │
│   ├── package.json
│   └── vite.config.ts
│
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── NEXUS/
│       │           └── NEXUS/
│       │               ├── task/
│       │               ├── worker/
│       │               ├── event/
│       │               └── operator/
│       │
│       └── resources/
│           └── application.properties
│
├── pom.xml
└── README.md
Requirements
Before running NEXUS, install:

Java 21

Maven

Node.js

npm

PostgreSQL

Verify installations:

java -version

mvn -version

node -v

npm -v

PostgreSQL Setup
Create a database for NEXUS:

sql

CREATE DATABASE nexus;
Configure the database connection in src/main/resources/application.properties:

text

spring.datasource.url=jdbc:postgresql://localhost:5432/nexus
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
Replace YOUR_PASSWORD with your PostgreSQL password.

Running the Backend
From the project root:

bash

cd NEXUS
mvn clean spring-boot:run
The backend runs at:

text

http://localhost:8080
Running the Frontend
Open another terminal:

bash

cd NEXUS/frontend
npm install
npm run dev
The frontend typically runs at:

text

http://localhost:5173
Make sure the Spring Boot backend is running before using the dashboard.

REST API
Tasks
Create Task
POST /tasks

Example PowerShell request:

powershell

Invoke-RestMethod `
  -Uri "http://localhost:8080/tasks" `
-Method POST `
  -Headers @{"Idempotency-Key"="test-123"} `
-ContentType "application/json" `
-Body '{"type":"EMAIL","payload":"Hello NEXUS"}'
Get Tasks
GET /operator/tasks

powershell

Invoke-RestMethod "http://localhost:8080/operator/tasks"
Worker APIs
Get Workers
GET /workers

powershell

Invoke-RestMethod "http://localhost:8080/workers"
Create Worker
POST /workers?workerId=worker-test

powershell

Invoke-RestMethod `
  "http://localhost:8080/workers?workerId=worker-test" `
-Method POST
Delete Worker
DELETE /workers/{workerId}

powershell

Invoke-RestMethod `
  "http://localhost:8080/workers/worker-test" `
-Method DELETE
Stop Worker
POST /workers/{workerId}/stop

Restart Worker
POST /workers/{workerId}/restart

Operator APIs
System Status
GET /operator/status

powershell

Invoke-RestMethod "http://localhost:8080/operator/status"
Example response:

text

totalTasks      : 23
acceptedTasks   : 0
processingTasks : 0
completedTasks  : 20
retryingTasks   : 0
deadLetterTasks : 3
failureMode     : NORMAL
workers         : ...
Failure Mode
Enable failure simulation:

POST /workers/failure/enable

Disable failure simulation:

POST /workers/failure/disable

Event Timeline
NEXUS keeps a full event history for task execution.

Example successful flow:

TASK_ACCEPTED

TASK_STARTED

TASK_COMPLETED

Example failure flow:

TASK_ACCEPTED

TASK_STARTED

TASK_FAILED

TASK_RETRY_SCHEDULED

TASK_STARTED

TASK_FAILED

TASK_DEAD_LETTERED

This makes task execution explainable and auditable.

Example Load Test
Submit multiple tasks:

powershell

1..10 | ForEach-Object {
Invoke-RestMethod `
      -Uri "http://localhost:8080/tasks" `
-Method POST `
      -Headers @{"Idempotency-Key"="load-test-$_"} `
-ContentType "application/json" `
      -Body "{`"type`":`"EMAIL`",`"payload`":`"Load Test $_`"}"
}
Then inspect system status:

powershell

Invoke-RestMethod "http://localhost:8080/operator/status"
Fault-Tolerance Model
NEXUS follows this flow:

Task acceptance

Worker pickup

Processing

Success or failure

Retry if attempts remain

Dead-letter if attempts are exhausted

This ensures tasks do not disappear silently when failures occur.

Observability
NEXUS provides visibility into:

Task lifecycle

Worker state

Worker failures

Worker restart counts

Heartbeats

Retry attempts

Dead-letter tasks

System-level task statistics

Historical task events

Project Goals
NEXUS was designed around four major goals:

Reliability

Fault tolerance

Observability

Explainability

Author
Ayush Abhinav
B.Tech Computer Science & Engineering

License
This project is intended for educational, demonstration, and portfolio purposes.
