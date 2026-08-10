package com.NEXUS.NEXUS.worker;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerManager {

    private final WorkerRepository workerRepository;

    public WorkerManager(WorkerRepository workerRepository) {
        this.workerRepository = workerRepository;
    }

    @PostConstruct
    public void initialize() {

        if (!workerRepository.existsById("worker-1")) {
            workerRepository.save(
                    new WorkerEntity("worker-1", 3)
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
    public void stopWorker(String workerId) {

        WorkerEntity worker = getWorker(workerId);
        worker.stop();

        workerRepository.save(worker);
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
    public void heartbeat(String workerId) {

        WorkerEntity worker = getWorker(workerId);

        worker.heartbeat();

        workerRepository.save(worker);
    }
}
