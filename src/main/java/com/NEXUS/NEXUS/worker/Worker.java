package com.NEXUS.NEXUS.worker;

import com.NEXUS.NEXUS.task.Task;

public interface Worker {

    void process(Task task);

    String getWorkerId();

    WorkerStatus getStatus();
}