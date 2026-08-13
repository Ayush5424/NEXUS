package com.NEXUS.NEXUS.worker;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkerManager {

    private static final int MAX_RESTARTS = 3;

    private final WorkerRepository workerRepository;

    public WorkerManager(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @PostConstruct
    public void initialize() {
        createIfMissing("worker-1");
        createIfMissing("worker-2");
        createIfMissing("worker-3");
    }

    private void createIfMissing(String workerId) {
        if (!workerRepository.existsById(workerId)) {
            WorkerEntity worker = new WorkerEntity(
                    workerId,
                    MAX_RESTARTS
            );
            workerRepository.save(worker);
        }
    }

    @Transactional
    public WorkerEntity createWorker(String workerId) {

        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException("Worker ID cannot be empty");
        }

        if (workerRepository.existsById(workerId)) {
            throw new IllegalArgumentException(
                    "Worker already exists: " + workerId
            );
        }

        WorkerEntity worker = new WorkerEntity(
                workerId,
                MAX_RESTARTS
        );

        return workerRepository.save(worker);
    }

    @Transactional
    public void deleteWorker(String workerId) {
        WorkerEntity worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found: " + workerId));

        // Delete managed entity and flush SQL DELETE immediately
        workerRepository.delete(worker);
        workerRepository.flush();
    }

    public List<WorkerEntity> getWorkers() {
        return workerRepository.findAll();
    }

    public WorkerEntity getWorker(String workerId) {
        return workerRepository.findById(workerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Worker not found: " + workerId
                        )
                );
    }

    @Transactional
    public void heartbeat(String workerId) {
        Optional<WorkerEntity> optionalWorker = workerRepository.findById(workerId);

        if (optionalWorker.isEmpty()) {
            return;
        }

        WorkerEntity worker = optionalWorker.get();

        if (worker.getStatus() == WorkerStatus.RUNNING) {
            worker.heartbeat();
            workerRepository.save(worker);
        }
    }

    @Transactional
    public void recordFailure(String workerId) {
        Optional<WorkerEntity> optionalWorker = workerRepository.findById(workerId);

        if (optionalWorker.isEmpty()) {
            return;
        }

        WorkerEntity worker = optionalWorker.get();

        worker.recordFailure();

        if (worker.getRestartCount() >= worker.getMaxRestarts()) {
            worker.markOutOfService();
        }

        workerRepository.save(worker);
    }

    @Transactional
    public void stopWorker(String workerId) {
        WorkerEntity worker = getWorker(workerId);

        worker.stop();

        workerRepository.save(worker);
    }

    @Transactional
    public void restartWorker(String workerId) {
        WorkerEntity worker = getWorker(workerId);

        if (worker.getRestartCount() >= worker.getMaxRestarts()) {
            worker.markOutOfService();
        } else {
            worker.markRestarting();
            worker.restart();
        }

        workerRepository.save(worker);
    }

    @Transactional
    public void checkWorkerHealth(String workerId) {
        Optional<WorkerEntity> optionalWorker = workerRepository.findById(workerId);

        if (optionalWorker.isEmpty()) {
            return;
        }

        WorkerEntity worker = optionalWorker.get();

        if (worker.getStatus() != WorkerStatus.RUNNING) {
            return;
        }

        LocalDateTime timeout = LocalDateTime.now().minusSeconds(10);

        if (worker.getLastHeartbeat() != null &&
                worker.getLastHeartbeat().isBefore(timeout)) {

            worker.markRestarting();

            if (worker.getRestartCount() < worker.getMaxRestarts()) {
                worker.restart();
            } else {
                worker.markOutOfService();
            }

            workerRepository.save(worker);
        }
    }
}