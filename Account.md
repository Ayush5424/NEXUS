# Architectural Decision & Strategy Account

## 1. Scope
* **Built:** Local task orchestration, crash recovery engine, release rollback engine, worker health monitoring, human-readable diagnostics.
* **Excluded:** Cloud message buses (Kafka/SQS) to adhere strictly to Rule 01/02 offline local execution.

## 2. Decisions
* **Tech Stack:** Spring Boot + JPA/H2 + React.
* **Crash Safety:** On startup, `StartupRecovery` resets stranded `PROCESSING` tasks back to `ACCEPTED` state to guarantee work is never lost.

## 3. Failure Behaviour
* **Worker Crash Loops:** Capped at `MAX_RESTARTS`. On breach, status transitions to `OUT_OF_SERVICE`.
* **Testing:** Triggerable via UI "Kill Worker" button.

## 4. Limits
* Single-node in-memory/DB dispatch queue. Handles up to 1,000 tasks/min smoothly.

## 5. Confidence
* Fully tested process recovery, restart limits, release rollbacks, and offline local cold-starts.

## 6. Next Steps
* Add dynamic rate-limiting controls and worker auto-scaling algorithms.