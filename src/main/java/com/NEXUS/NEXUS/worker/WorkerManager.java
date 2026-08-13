package com.NEXUS.NEXUS.worker;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

            workerRepository.save(
                    new WorkerEntity(
                            workerId,
                            MAX_RESTARTS
                    )
            );
        }
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

        WorkerEntity worker = getWorker(workerId);

        if (worker.getStatus() == WorkerStatus.RUNNING) {
            worker.heartbeat();
            workerRepository.save(worker);
        }
    }

    @Transactional
    public void recordFailure(String workerId) {

        WorkerEntity worker = getWorker(workerId);

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

        WorkerEntity worker = getWorker(workerId);

        if (worker.getStatus() != WorkerStatus.RUNNING) {
            return;
        }

        LocalDateTime timeout =
                LocalDateTime.now().minusSeconds(10);

        if (worker.getLastHeartbeat().isBefore(timeout)) {

            worker.markRestarting();
            workerRepository.save(worker);

            if (worker.getRestartCount() < worker.getMaxRestarts()) {

                worker.restart();

            } else {

                worker.markOutOfService();
            }

            workerRepository.save(worker);
        }
    }
}