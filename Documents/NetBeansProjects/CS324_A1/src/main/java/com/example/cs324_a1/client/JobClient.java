/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.client;

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.rmi.BootstrapService;
import com.example.cs324_a1.rmi.WorkerService;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// Client-side access to the distributed system through Java RMI.
// Finds the current coordinator via the Bootstrap Node and submits jobs to it.
// One instance is shared by all task threads of a client, so it only keeps
// immutable settings plus one atomic timestamp.
public class JobClient {

    // How long a job keeps retrying while there is no usable coordinator. Long enough
    // to cover a new election after five jobs and Bootstrap's 30 s heartbeat timeout.
    public static final long DEFAULT_RETRY_WINDOW_MS = 90000;

    // Wait this long with no coordinator anywhere before asking a worker to start an election,
    // so an election that is already running (e.g. after the five-job limit) can finish first.
    private static final long NO_COORDINATOR_GRACE_MS = 2500;

    // Minimum gap between election requests from this client
    private static final long ELECTION_REQUEST_GAP_MS = 8000;

    private final String bootstrapHost;
    private final int bootstrapPort;
    private final String clientId;
    private final long retryWindowMs;

    // Shared by all task threads of this client. compareAndSet lets exactly one
    // of them request an election when several notice there is no coordinator.
    private final AtomicLong lastElectionRequest = new AtomicLong(0);

    public JobClient(String bootstrapHost, int bootstrapPort, String clientId) {

        this(bootstrapHost, bootstrapPort, clientId, DEFAULT_RETRY_WINDOW_MS);
    }

    public JobClient(String bootstrapHost, int bootstrapPort, String clientId, long retryWindowMs) {

        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.clientId = clientId;
        this.retryWindowMs = retryWindowMs;
    }

    public String getClientId() {
        return clientId;
    }

    public String getBootstrapAddress() {
        return bootstrapHost + ":" + bootstrapPort;
    }

    // Ask Bootstrap for the active workers and ask the workers who the coordinator is.
    public ClusterStatus fetchStatus() throws JobFailedException {

        List<WorkerInfo> workers;

        try {
            BootstrapService bootstrap = (BootstrapService) Naming.lookup(
                    "rmi://" + bootstrapHost + ":" + bootstrapPort + "/BootstrapService");

            workers = new ArrayList<>(bootstrap.getActiveWorkers());

        } catch (Exception e) {
            throw new JobFailedException("Cannot reach the Bootstrap Node at " + getBootstrapAddress()
                    + " (" + rootMessage(e) + ")", e);
        }

        workers.sort(Comparator.comparingInt(WorkerInfo::getId));

        // Workers can briefly disagree while COORDINATOR / TERM-END messages propagate,
        // so trust the view with the newest term.
        int coordinatorId = -1;
        int term = -1;

        for (WorkerInfo worker : workers) {

            try {
                WorkerService remote = lookup(worker);
                int known = remote.getCoordinatorId();
                int knownTerm = remote.getCoordinatorTerm();

                if (known != -1 && knownTerm > term && findWorker(workers, known) != null) {
                    coordinatorId = known;
                    term = knownTerm;
                }

            } catch (Exception e) {
                // Worker crashed but Bootstrap has not timed it out yet; skip it
            }
        }

        return new ClusterStatus(workers, coordinatorId, term);
    }

    // Submit a job to the coordinator and wait for the combined result.
    // Retries while there is no coordinator (e.g. during an election) or the
    // coordinator's term is full, and asks a worker to start an election when
    // the system has no coordinator at all.
    public JobResult submit(JobRequest request, Consumer<String> progress) throws JobFailedException, InterruptedException {

        long deadline = System.currentTimeMillis() + retryWindowMs;
        long noCoordinatorSince = -1;
        int attempt = 0;

        while (true) {

            attempt++;
            String reason;

            try {
                ClusterStatus status = fetchStatus();

                if (status.getWorkers().isEmpty()) {

                    reason = "no workers are registered with Bootstrap";

                } else if (!status.hasCoordinator()) {

                    long now = System.currentTimeMillis();

                    if (noCoordinatorSince < 0) {
                        noCoordinatorSince = now;
                    }

                    reason = "no coordinator yet";

                    if (now - noCoordinatorSince >= NO_COORDINATOR_GRACE_MS) {
                        requestElection(status, progress);
                    }

                } else {

                    noCoordinatorSince = -1;
                    WorkerInfo coordinator = status.getCoordinator();

                    progress.accept("Running on coordinator Worker " + coordinator.getId()
                            + " (term " + status.getTerm() + ")");

                    return lookup(coordinator).submitJob(request);
                }

            } catch (JobFailedException e) {
                // Bootstrap unreachable: the system is down, so retrying will not help
                throw e;

            } catch (Exception e) {

                if (isInputError(e)) {
                    throw new JobFailedException(rootMessage(e), e);
                }

                reason = rootMessage(e);
            }

            if (System.currentTimeMillis() >= deadline) {
                throw new JobFailedException("Gave up after " + attempt + " attempts: " + reason, null);
            }

            progress.accept("Waiting - " + reason + " (attempt " + attempt + ")");

            // 250 ms, 500 ms, 1 s, then every 2 s
            Thread.sleep(Math.min(2000, 250L << Math.min(attempt - 1, 3)));
        }
    }

    private void requestElection(ClusterStatus status, Consumer<String> progress) {

        long now = System.currentTimeMillis();
        long last = lastElectionRequest.get();

        if (now - last < ELECTION_REQUEST_GAP_MS || !lastElectionRequest.compareAndSet(last, now)) {
            return;
        }

        // Every client asks the same (lowest-ID) worker, which makes overlapping
        // elections from different clients unlikely.
        WorkerInfo target = status.getWorkers().get(0);

        try {
            WorkerService worker = lookup(target);

            if (!worker.hasCoordinator()) {
                progress.accept("No coordinator - asking Worker " + target.getId() + " to start an election");
                worker.startElection();
            }

        } catch (Exception e) {
            progress.accept("Could not ask Worker " + target.getId() + " to start an election: " + rootMessage(e));
        }
    }

    private static WorkerService lookup(WorkerInfo worker) throws Exception {

        return (WorkerService) Naming.lookup(worker.getRmiUrl());
    }

    private static WorkerInfo findWorker(List<WorkerInfo> workers, int workerId) {

        for (WorkerInfo worker : workers) {
            if (worker.getId() == workerId) {
                return worker;
            }
        }

        return null;
    }

    // Invalid input fails the same way on every attempt, so it is not retried
    private static boolean isInputError(Throwable error) {

        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof IllegalArgumentException) {
                return true;
            }
        }

        return false;
    }

    // RMI wraps server errors several times; the innermost message is the useful one
    public static String rootMessage(Throwable error) {

        String message = null;

        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t.getMessage() != null && !t.getMessage().isBlank()) {
                message = t.getMessage();
            } else if (message == null) {
                message = t.getClass().getSimpleName();
            }
        }

        return message == null ? "unknown error" : message;
    }

    // Snapshot of the system as seen by the client
    public static class ClusterStatus {

        private final List<WorkerInfo> workers;
        private final int coordinatorId;
        private final int term;

        public ClusterStatus(List<WorkerInfo> workers, int coordinatorId, int term) {

            this.workers = new ArrayList<>(workers);
            this.coordinatorId = coordinatorId;
            this.term = term;
        }

        public List<WorkerInfo> getWorkers() {
            return Collections.unmodifiableList(workers);
        }

        public List<Integer> getWorkerIds() {

            List<Integer> ids = new ArrayList<>();

            for (WorkerInfo worker : workers) {
                ids.add(worker.getId());
            }

            return ids;
        }

        public boolean hasCoordinator() {
            return coordinatorId != -1;
        }

        public int getCoordinatorId() {
            return coordinatorId;
        }

        public WorkerInfo getCoordinator() {
            return findWorker(workers, coordinatorId);
        }

        public int getTerm() {
            return term;
        }

        @Override
        public String toString() {

            String coordinator = hasCoordinator()
                    ? "Coordinator: Worker " + coordinatorId + " (term " + term + ")"
                    : "Coordinator: none";

            return workers.size() + " active worker(s) " + getWorkerIds() + "  |  " + coordinator;
        }
    }

    public static class JobFailedException extends Exception {

        private static final long serialVersionUID = 1L;

        public JobFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
