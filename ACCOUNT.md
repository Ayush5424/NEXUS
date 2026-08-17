# ACCOUNT

## Scope

Built:

- Local persisted task intake and dispatch.
- Idempotency-key based duplicate detection.
- Bounded retry with exponential backoff.
- Dead-letter terminal state.
- Startup recovery for stranded `PROCESSING` tasks.
- Logical worker state with restart budget and out-of-service status.
- Durable events with details.
- Release deploy and one-action rollback to a known target.
- Operator diagnostics, task list, worker list, event history, cache age/disagreement view, and failure triggers.

Deliberately left out:

- Real OS-level worker processes. Workers are logical in-process components.
- Distributed queues, cloud services, hosted databases, auth, or external monitoring.
- Full production migration management. Hibernate `update` is used for the challenge-sized local database.

## Decisions

Chosen:

- Spring Boot + JPA because the existing project already used it.
- File-backed H2 by default so a reviewer can run offline with one backend command.
- Idempotency keys retained indefinitely in the local database; duplicate detection lasts as long as the database file is kept.
- Bounded dispatch batches to avoid recovery storms.
- Durable event rows for platform actions and operator actions.

Rejected:

- PostgreSQL as the required default, because it adds setup and can violate the one-machine/offline expectation if treated as hosted infrastructure.
- External worker processes, because the existing architecture was in-process and a rewrite would add risk.
- Kafka/RabbitMQ/SQS, because the challenge favors a small local system.

What would change these decisions:

- A requirement for multi-host operation would justify an external durable queue and separately supervised workers.
- A production setting would require schema migrations, authentication, and structured log export.

## Failure Behaviour

- Worker failure: set `FAIL`; tasks retry with backoff and then become `DEAD_LETTER`.
- Worker slow: set `SLOW`; processing continues slowly and backlog age becomes visible.
- Logical worker killed: call `/operator/simulate/kill-worker`; worker state is persisted as `STOPPED`.
- Dependency unavailable: set `DEPENDENCY_DOWN`; diagnostics report degraded state and cache responses are explicitly stale-marked.
- Cache disagreement: set `CACHE_DISAGREE`; diagnostics report warning and events record the disagreement.
- Platform restart: `StartupRecovery` resets stranded `PROCESSING` work to `ACCEPTED` and records `TASK_RECOVERED`.
- Bad release: deploy a bad version and call `/operator/releases/undo`; rollback is recorded.

## Limits

- Single JVM and one local database file.
- Logical workers share one Spring async executor.
- Default executor is 3 core threads, 6 max threads, queue capacity 100.
- Default dispatch batch is 25 items per scheduler pass.
- Backlogs around thousands of items are expected to be visible and processed gradually; 10,000-item testing is reasoned and supported by throttling but not fully load-profiled here.
- If all workers are stopped or out of service, accepted work remains persisted and visible but will not process until a worker is restored.

## Confidence

Actually tested:

- Maven test suite passed.
- Duplicate work returns the original task.
- Retry attempts increment once per failure, backoff is scheduled, and terminal failure is visible.
- Startup recovery requeues stranded work and records history.
- Release rollback restores the previous version.
- Cache disagreement and dependency failure are visible in diagnostics.
- Worker restart budget creates an out-of-service state visible to operators.
- Frontend production build passed.

Reasoned but not exhaustively tested:

- Very large 10,000-item backlog behavior.
- Actual manual process kill during the exact sleep window.
- Phone usability beyond responsive layout checks in CSS.

Assumptions:

- Reviewers run the backend from the project root so the H2 file appears at `.\data\nexus.mv.db`.
- Idempotency-key retention equals database retention.

## Next

1. Add a scripted manual verification harness for the eight handbook scenarios.
2. Add a true process-per-worker mode if OS-level kill semantics are required.
3. Add schema migrations instead of Hibernate `update`.
4. Add load-test scripts for 10,000 item backlog recovery.
5. Add authentication only if the project leaves local challenge scope.
