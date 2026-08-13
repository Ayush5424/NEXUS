NEXUS
Fault-Tolerant Task Orchestration & System Health Platform

NEXUS is a fault-tolerant task orchestration platform designed to process tasks reliably while monitoring worker health, handling failures, retrying failed tasks, and maintaining an auditable event timeline.

It also provides an operator-facing dashboard for real-time visibility into workers, tasks, failures, retries, and system health.

Overview
NEXUS helps operators submit and track tasks, observe worker status, and understand how the system behaves during failures.
It is built to remain operational even when individual workers fail, while keeping a clear history of task execution.
imulate failures for testing.


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

Project Goals:
  NEXUS was designed around four major goals:
  - Reliability
  - Fault tolerance
  - Observability
  - Explainability

Author
- Ayush Abhinav
- B.Tech Computer Science & Engineering

License
This project is intended for educational, demonstration, and portfolio purposes.
