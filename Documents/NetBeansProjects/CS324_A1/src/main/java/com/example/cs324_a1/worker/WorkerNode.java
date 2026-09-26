/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.worker;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.coordinator.CoordinatorManager;
import com.example.cs324_a1.coordinator.TermEndMessage;
import com.example.cs324_a1.election.CoordinatorMessage;
import com.example.cs324_a1.election.ElectionManager;
import com.example.cs324_a1.election.ElectionMessage;
import com.example.cs324_a1.election.ElectionReply;
import com.example.cs324_a1.jobtype.JobProcessor;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.jobtype.JobType;
import com.example.cs324_a1.rmi.BootstrapService;
import com.example.cs324_a1.rmi.WorkerService;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerNode
        extends UnicastRemoteObject
        implements WorkerService {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String host;
    private final int port;
    private final String rmiName;

    private final String leaderman = "cs324";

    private final WorkerInfo info;

    private final ElectionManager electionManager;
    private final CoordinatorManager coordinatorManager;

    private final List<WorkerInfo> neighbours =
            Collections.synchronizedList(
                    new ArrayList<>()
            );

    private BootstrapService bootstrap;

    private final String bootstrapHost;
    private final int bootstrapPort;

    // Sends heartbeat to Bootstrap.
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    // Executes computational jobs.
    private final ExecutorService jobExecutor =
            Executors.newCachedThreadPool();

    private final JobProcessor jobProcessor =
            new JobProcessor();

    public WorkerNode(
            int id,
            String host,
            int port,
            String bootstrapHost,
            int bootstrapPort,
            int initialJac)
            throws RemoteException {

        super();

        this.id = id;
        this.host = host;
        this.port = port;

        this.rmiName =
                "WorkerService-" + id;

        this.bootstrapHost =
                bootstrapHost;

        this.bootstrapPort =
                bootstrapPort;

        this.info =
                new WorkerInfo(
                        id,
                        host,
                        port,
                        rmiName,
                        initialJac
                );

        this.electionManager =
                new ElectionManager();

        this.coordinatorManager =
                new CoordinatorManager();
    }

    public WorkerNode(
            int id,
            String host,
            int port,
            String bootstrapHost,
            int bootstrapPort)
            throws RemoteException {

        this(
                id,
                host,
                port,
                bootstrapHost,
                bootstrapPort,
                0
        );
    }

    public void start() throws Exception {

        Registry registry;

        try {

            registry =
                    LocateRegistry.createRegistry(port);

            System.out.println(
                    "[Worker " + id
                    + "] Created RMI registry on port "
                    + port
            );

        } catch (RemoteException e) {

            registry =
                    LocateRegistry.getRegistry(port);

            System.out.println(
                    "[Worker " + id
                    + "] Using existing RMI registry on port "
                    + port
            );
        }

        registry.rebind(
                rmiName,
                this
        );

        System.out.println(
                "[Worker " + id
                + "] Bound as "
                + rmiName
        );

        String bootstrapUrl =
                "rmi://"
                + bootstrapHost
                + ":"
                + bootstrapPort
                + "/BootstrapService";

        bootstrap =
                (BootstrapService)
                        Naming.lookup(
                                bootstrapUrl
                        );

        System.out.println(
                "[Worker " + id
                + "] Connected to Bootstrap"
        );

        WorkerInfo randomWorker =
                bootstrap.getRandomWorker();

        bootstrap.registerWorker(
                getInfo()
        );

        if (randomWorker != null
                && randomWorker.getId() != id) {

            try {

                addNeighbour(
                        randomWorker
                );

                WorkerService remote =
                        getRemoteWorker(
                                randomWorker
                        );

                remote.addNeighbour(
                        getInfo()
                );

                System.out.println(
                        "[Worker " + id
                        + "] Randomly connected to Worker "
                        + randomWorker.getId()
                );

            } catch (Exception e) {

                System.err.println(
                        "[Worker " + id
                        + "] Could not connect to Worker "
                        + randomWorker.getId()
                        + ": "
                        + e.getMessage()
                );
            }

        } else {

            System.out.println(
                    "[Worker " + id
                    + "] First worker - no neighbour yet."
            );
        }

        scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                5,
                10,
                TimeUnit.SECONDS
        );

        System.out.println();
        System.out.println("==============================");
        System.out.println(" WORKER " + id + " READY");
        System.out.println("==============================");

        System.out.println(
                "RMI Name   : " + rmiName
        );

        System.out.println(
                "Port       : " + port
        );

        System.out.println(
                "JAC        : " + info.getJac()
        );

        System.out.println(
                "leaderman  : " + leaderman
        );

        System.out.println(
                "Neighbours : " + neighbours.size()
        );
    }

    private void sendHeartbeat() {

        try {

            if (bootstrap != null) {

                bootstrap.heartbeat(id);
            }

        } catch (Exception e) {

            System.err.println(
                    "[Worker " + id
                    + "] Heartbeat failed: "
                    + e.getMessage()
            );
        }
    }

    private WorkerService getRemoteWorker(
            WorkerInfo worker)
            throws Exception {

        return (WorkerService)
                Naming.lookup(
                        worker.getRmiUrl()
                );
    }

    @Override
    public int getId()
            throws RemoteException {

        return id;
    }

    @Override
    public int getJac()
            throws RemoteException {

        return info.getJac();
    }

    @Override
    public WorkerInfo getInfo()
            throws RemoteException {

        return new WorkerInfo(
                id,
                host,
                port,
                rmiName,
                info.getJac()
        );
    }

    @Override
    public String getStatus()
            throws RemoteException {

        String coordinator;

        if (coordinatorManager.hasCoordinator()) {

            coordinator =
                    "Worker "
                    + coordinatorManager.getCoordinatorId();

        } else {

            coordinator = "NONE";
        }

        return "Worker "
                + id
                + " | JAC="
                + info.getJac()
                + " | neighbours="
                + neighbours.size()
                + " | coordinator="
                + coordinator
                + " | leaderman="
                + leaderman;
    }

    public int getWorkerId() {

        return id;
    }

    public String getLeaderman() {

        return leaderman;
    }

    @Override
    public void addNeighbour(
            WorkerInfo neighbour)
            throws RemoteException {

        if (neighbour == null
                || neighbour.getId() == id) {

            return;
        }

        synchronized (neighbours) {

            if (!neighbours.contains(neighbour)) {

                neighbours.add(
                        neighbour
                );

                System.out.println(
                        "[Worker " + id
                        + "] Added neighbour Worker "
                        + neighbour.getId()
                );
            }
        }
    }

    @Override
    public List<WorkerInfo> getNeighbours()
            throws RemoteException {

        synchronized (neighbours) {

            return new ArrayList<>(
                    neighbours
            );
        }
    }

    @Override
    public String ping(
            String message)
            throws RemoteException {

        System.out.println(
                "[Worker " + id
                + "] Received ping: "
                + message
        );

        return "Pong from Worker "
                + id;
    }

    public ElectionManager getElectionManager() {

        return electionManager;
    }

    public CoordinatorManager getCoordinatorManager() {

        return coordinatorManager;
    }

    @Override
    public boolean hasCoordinator()
            throws RemoteException {

        return coordinatorManager
                .hasCoordinator();
    }

    @Override
    public boolean isCoordinator()
            throws RemoteException {

        return coordinatorManager.hasCoordinator()
                && coordinatorManager.getCoordinatorId()
                == id;
    }
    
    @Override
    public void startElection()
            throws RemoteException {

        if (coordinatorManager.hasCoordinator()) {

            System.out.println(
                    "[ELECTION] Worker "
                    + id
                    + " cannot start election."
            );

            System.out.println(
                    "[ELECTION] Coordinator Worker "
                    + coordinatorManager.getCoordinatorId()
                    + " is already active."
            );

            return;
        }

        System.out.println();
        System.out.println("==============================");
        System.out.println(" LEADER ELECTION STARTED");
        System.out.println("==============================");

        System.out.println(
                "Worker "
                + id
                + " is initiating the election."
        );

        ElectionMessage message =
                electionManager.createElection(
                        id
                );

        String electionId =
                message.getElectionId();

        electionManager.shouldProcess(
                electionId
        );

        message.addCandidate(
                getInfo()
        );

        List<WorkerInfo> neighbourCopy =
                getNeighbours();

        List<Integer> expectedReplies =
                new ArrayList<>();

        for (WorkerInfo neighbour
                : neighbourCopy) {

            expectedReplies.add(
                    neighbour.getId()
            );
        }

        electionManager.initializeElectionState(
                electionId,
                getInfo(),
                expectedReplies
        );

        System.out.println(
                "[ELECTION] Worker "
                + id
                + " joined election."
        );

        System.out.println(
                "[ELECTION] Worker "
                + id
                + " JAC = "
                + info.getJac()
        );

        if (expectedReplies.isEmpty()) {

            finishElectionAsInitiator(
                    electionId
            );

            return;
        }

        for (WorkerInfo neighbour
                : neighbourCopy) {

            try {

                System.out.println(
                        "[ELECTION] Worker "
                        + id
                        + " sending election to Worker "
                        + neighbour.getId()
                );

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveElection(
                        message,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[ELECTION] Worker "
                        + neighbour.getId()
                        + " unreachable: "
                        + e.getMessage()
                );

                electionManager.processReply(
                        new ElectionReply(
                                electionId,
                                neighbour.getId()
                        )
                );
            }
        }

        checkElectionComplete(
                electionId
        );
    }

    @Override
    public void receiveElection(
            ElectionMessage message,
            int senderId)
            throws RemoteException {

        if (message == null) {
            return;
        }

        String electionId =
                message.getElectionId();

        System.out.println(
                "[ELECTION] Worker "
                + id
                + " received election from Worker "
                + senderId
        );

        if (!electionManager.shouldProcess(
                electionId)) {

            System.out.println(
                    "[ELECTION] Worker "
                    + id
                    + " ignored duplicate election."
            );

            sendEmptyReply(
                    electionId,
                    senderId
            );

            return;
        }

        electionManager.recordParent(
                electionId,
                senderId
        );

        message.addCandidate(
                getInfo()
        );

        List<WorkerInfo> neighbourCopy =
                getNeighbours();

        List<Integer> expectedReplies =
                new ArrayList<>();

        for (WorkerInfo neighbour
                : neighbourCopy) {

            if (neighbour.getId()
                    == senderId) {

                continue;
            }

            expectedReplies.add(
                    neighbour.getId()
            );
        }

        electionManager.initializeElectionState(
                electionId,
                getInfo(),
                expectedReplies
        );

        if (expectedReplies.isEmpty()) {

            sendCompletedReplyToParent(
                    electionId
            );

            return;
        }

        for (WorkerInfo neighbour
                : neighbourCopy) {

            if (neighbour.getId()
                    == senderId) {

                continue;
            }

            try {

                System.out.println(
                        "[ELECTION] Worker "
                        + id
                        + " forwarding election to Worker "
                        + neighbour.getId()
                );

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveElection(
                        message,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[ELECTION] Could not contact Worker "
                        + neighbour.getId()
                );

                electionManager.processReply(
                        new ElectionReply(
                                electionId,
                                neighbour.getId()
                        )
                );
            }
        }

        checkElectionComplete(
                electionId
        );
    }

    @Override
    public void receiveElectionReply(
            ElectionReply reply)
            throws RemoteException {

        if (reply == null) {
            return;
        }

        String electionId =
                reply.getElectionId();

        System.out.println(
                "[ELECTION-REPLY] Worker "
                + id
                + " received reply from Worker "
                + reply.getSenderId()
        );

        electionManager.processReply(
                reply
        );

        checkElectionComplete(
                electionId
        );
    }

    private void checkElectionComplete(
            String electionId)
            throws RemoteException {

        if (electionManager.hasPendingReplies(
                electionId)) {

            return;
        }

        if (electionManager.hasParent(
                electionId)) {

            sendCompletedReplyToParent(
                    electionId
            );

        } else {

            finishElectionAsInitiator(
                    electionId
            );
        }
    }

    private void sendEmptyReply(
            String electionId,
            int receiverId)
            throws RemoteException {

        WorkerInfo receiver =
                findNeighbour(
                        receiverId
                );

        if (receiver == null) {

            System.out.println(
                    "[ELECTION-REPLY] Cannot find Worker "
                    + receiverId
            );

            return;
        }

        ElectionReply reply =
                new ElectionReply(
                        electionId,
                        id
                );

        try {

            WorkerService remote =
                    getRemoteWorker(
                            receiver
                    );

            remote.receiveElectionReply(
                    reply
            );

        } catch (Exception e) {

            System.err.println(
                    "[ELECTION-REPLY] Could not send ACK to Worker "
                    + receiverId
            );
        }
    }

    private void sendCompletedReplyToParent(
            String electionId)
            throws RemoteException {

        if (!electionManager.markReplySent(
                electionId)) {

            return;
        }

        int parentId =
                electionManager.getParent(
                        electionId
                );

        if (parentId == -1) {
            return;
        }

        WorkerInfo parent =
                findNeighbour(
                        parentId
                );

        if (parent == null) {

            System.err.println(
                    "[ELECTION-REPLY] Parent Worker "
                    + parentId
                    + " could not be found."
            );

            return;
        }

        ElectionReply reply =
                electionManager.createReply(
                        electionId,
                        id
                );

        try {

            WorkerService remote =
                    getRemoteWorker(
                            parent
                    );

            remote.receiveElectionReply(
                    reply
            );

        } catch (Exception e) {

            System.err.println(
                    "[ELECTION-REPLY] Failed to reply to parent Worker "
                    + parentId
            );
        }
    }

    private void finishElectionAsInitiator(
            String electionId)
            throws RemoteException {

        if (!electionManager.markReplySent(
                electionId)) {

            return;
        }

        List<WorkerInfo> candidates =
                electionManager
                        .getCollectedCandidates(
                                electionId
                        );

        System.out.println();
        System.out.println("==============================");
        System.out.println(" ELECTION COMPLETE");
        System.out.println("==============================");

        System.out.println(
                "Workers found: "
                + candidates.size()
        );

        WorkerInfo winner =
                electionManager.selectCoordinator(
                        candidates
                );

        if (winner == null) {

            System.out.println(
                    "[ELECTION] No coordinator selected."
            );

            return;
        }

        System.out.println(
                "[ELECTION] Worker "
                + winner.getWorkerId()
                + " elected coordinator."
        );

        int newTerm =
                coordinatorManager.nextTerm();

        CoordinatorMessage coordinatorMessage =
                new CoordinatorMessage(
                        electionId,
                        winner.getWorkerId(),
                        newTerm
                );

        coordinatorManager
                .processCoordinatorMessage(
                        coordinatorMessage
                );

        for (WorkerInfo neighbour
                : getNeighbours()) {

            try {

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveCoordinator(
                        coordinatorMessage,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[COORDINATOR] Could not inform Worker "
                        + neighbour.getId()
                );
            }
        }
    }

    @Override
    public void receiveCoordinator(
            CoordinatorMessage message,
            int senderId)
            throws RemoteException {

        if (message == null) {
            return;
        }

        boolean accepted =
                coordinatorManager
                        .processCoordinatorMessage(
                                message
                        );

        if (!accepted) {

            System.out.println(
                    "[COORDINATOR] Worker "
                    + id
                    + " ignored duplicate/old message."
            );

            return;
        }

        System.out.println(
                "[COORDINATOR] Worker "
                + id
                + " agrees Coordinator = Worker "
                + message.getCoordinatorId()
                + " | Term = "
                + message.getTerm()
        );

        for (WorkerInfo neighbour
                : getNeighbours()) {

            if (neighbour.getId()
                    == senderId) {

                continue;
            }

            try {

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveCoordinator(
                        message,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[COORDINATOR] Could not forward to Worker "
                        + neighbour.getId()
                );
            }
        }
    }

    @Override
    public JobResult submitJob(
            JobRequest request)
            throws RemoteException {

        if (request == null) {

            throw new RemoteException(
                    "Job request cannot be null."
            );
        }

        if (!coordinatorManager.hasCoordinator()) {

            throw new RemoteException(
                    "No coordinator is currently active."
            );
        }

        if (!isCoordinator()) {

            throw new RemoteException(
                    "Worker "
                    + id
                    + " is not the coordinator. "
                    + "Current coordinator is Worker "
                    + coordinatorManager.getCoordinatorId()
            );
        }

        System.out.println();
        System.out.println("==============================");
        System.out.println(" NEW DISTRIBUTED JOB");
        System.out.println("==============================");

        System.out.println(
                "Job ID   : "
                + request.getJobId()
        );

        System.out.println(
                "Job Type : "
                + request.getJobType()
        );

        try {

            JobResult result =
                    distributeJob(
                            request
                    );

            boolean termFinished =
                    coordinatorManager
                            .recordJobAssignment();

            if (termFinished) {

                finishCoordinatorTerm();
            }

            return result;

        } catch (Exception e) {

            throw new RemoteException(
                    "Distributed job failed.",
                    e
            );
        }
    }

    private JobResult distributeJob(
            JobRequest request)
            throws Exception {

        List<WorkerInfo> workers =
                bootstrap.getActiveWorkers();

        if (workers == null) {

            workers =
                    new ArrayList<>();
        }

        boolean coordinatorFound = false;

        for (WorkerInfo worker : workers) {

            if (worker.getId() == id) {

                coordinatorFound = true;
                break;
            }
        }

        if (!coordinatorFound) {

            workers.add(
                    getInfo()
            );
        }

        if (workers.isEmpty()) {

            throw new RemoteException(
                    "No active workers available."
            );
        }

        List<JobAssignment> assignments =
                createAssignments(
                        request,
                        workers
                );

        List<CompletableFuture<JobResult>> futures =
                new ArrayList<>();

        for (JobAssignment assignment
                : assignments) {

            CompletableFuture<JobResult> future =
                    CompletableFuture.supplyAsync(
                            () -> {

                                try {

                                    return executeAssignment(
                                            assignment
                                    );

                                } catch (Exception e) {

                                    throw new RuntimeException(
                                            e
                                    );
                                }
                            },
                            jobExecutor
                    );

            futures.add(
                    future
            );
        }

        List<JobResult> partialResults =
                new ArrayList<>();

        for (CompletableFuture<JobResult> future
                : futures) {

            partialResults.add(
                    future.get()
            );
        }

        long finalValue =
                combineResults(
                        request.getJobType(),
                        partialResults
                );

        System.out.println(
                "[JOB] Final result = "
                + finalValue
        );

        return new JobResult(
                request.getJobId(),
                request.getJobType(),
                finalValue,
                id
        );
    }

    private List<JobAssignment> createAssignments(
            JobRequest request,
            List<WorkerInfo> workers) {

        List<JobAssignment> assignments =
                new ArrayList<>();

        if (request.getJobType()
                == JobType.PRIMESUM) {

            int start =
                    request.getStart();

            int end =
                    request.getEnd();

            if (start > end) {

                throw new IllegalArgumentException(
                        "Start cannot be greater than end."
                );
            }

            long totalValues =
                    (long) end - start + 1;

            int workerCount =
                    (int) Math.min(
                            workers.size(),
                            totalValues
                    );

            long baseSize =
                    totalValues / workerCount;

            long remainder =
                    totalValues % workerCount;

            long current =
                    start;

            for (int i = 0;
                    i < workerCount;
                    i++) {

                long size =
                        baseSize
                        + (i < remainder ? 1 : 0);

                int partStart =
                        (int) current;

                int partEnd =
                        (int) (
                                current
                                + size
                                - 1
                        );

                JobRequest part =
                        new JobRequest(
                                JobType.PRIMESUM,
                                partStart,
                                partEnd
                        );

                assignments.add(
                        new JobAssignment(
                                workers.get(i),
                                part
                        )
                );

                current =
                        (long) partEnd + 1;
            }

            return assignments;
        }

        List<Integer> numbers =
                request.getNumbers();

        if (numbers == null
                || numbers.isEmpty()) {

            throw new IllegalArgumentException(
                    request.getJobType()
                    + " requires numbers."
            );
        }

        int workerCount =
                Math.min(
                        workers.size(),
                        numbers.size()
                );

        int baseSize =
                numbers.size()
                / workerCount;

        int remainder =
                numbers.size()
                % workerCount;

        int index = 0;

        for (int i = 0;
                i < workerCount;
                i++) {

            int size =
                    baseSize
                    + (i < remainder ? 1 : 0);

            List<Integer> partNumbers =
                    new ArrayList<>(
                            numbers.subList(
                                    index,
                                    index + size
                            )
                    );

            JobRequest part =
                    new JobRequest(
                            request.getJobType(),
                            partNumbers
                    );

            assignments.add(
                    new JobAssignment(
                            workers.get(i),
                            part
                    )
            );

            index += size;
        }

        return assignments;
    }

    private JobResult executeAssignment(
            JobAssignment assignment)
            throws Exception {

        WorkerInfo worker =
                assignment.worker;

        JobRequest request =
                assignment.request;

        if (worker.getId() == id) {

            return executeSubJob(
                    request
            );
        }

        System.out.println(
                "[JOB] Coordinator Worker "
                + id
                + " assigning "
                + request.getJobType()
                + " to Worker "
                + worker.getId()
        );

        info.incrementJac();

        WorkerService remote =
                getRemoteWorker(
                        worker
                );

        return remote.executeSubJob(
                request
        );
    }

    @Override
    public JobResult executeSubJob(
            JobRequest request)
            throws RemoteException {

        if (request == null) {

            throw new RemoteException(
                    "Sub-job cannot be null."
            );
        }

        try {

            return CompletableFuture.supplyAsync(
                    () -> {

                        long result =
                                jobProcessor.process(
                                        request
                                );

                        System.out.println(
                                "[JOB] Worker "
                                + id
                                + " completed "
                                + request.getJobType()
                                + " | result="
                                + result
                        );

                        return new JobResult(
                                request.getJobId(),
                                request.getJobType(),
                                result,
                                id
                        );
                    },
                    jobExecutor
            ).get();

        } catch (Exception e) {

            throw new RemoteException(
                    "Worker "
                    + id
                    + " failed to execute job.",
                    e
            );
        }
    }

    private long combineResults(
            JobType type,
            List<JobResult> results) {

        if (results == null
                || results.isEmpty()) {

            throw new IllegalArgumentException(
                    "No partial results."
            );
        }

        if (type == JobType.MAX) {

            long maximum =
                    Long.MIN_VALUE;

            for (JobResult result
                    : results) {

                maximum =
                        Math.max(
                                maximum,
                                result.getResult()
                        );
            }

            return maximum;
        }

        long total = 0;

        for (JobResult result
                : results) {

            total +=
                    result.getResult();
        }

        return total;
    }

    public void recordCoordinatorJob(
            boolean assignedToAnotherWorker)
            throws RemoteException {

        if (!coordinatorManager.hasCoordinator()) {

            System.out.println(
                    "[JOB] No active coordinator."
            );

            return;
        }

        if (!isCoordinator()) {

            System.out.println(
                    "[JOB] Worker "
                    + id
                    + " is not coordinator."
            );

            return;
        }

        if (assignedToAnotherWorker) {

            info.incrementJac();
        }

        boolean termFinished =
                coordinatorManager
                        .recordJobAssignment();

        if (termFinished) {

            finishCoordinatorTerm();
        }
    }

    private void finishCoordinatorTerm()
            throws RemoteException {

        int oldCoordinator =
                coordinatorManager
                        .getCoordinatorId();

        int finishedTerm =
                coordinatorManager
                        .getCurrentTerm();

        System.out.println();
        System.out.println("==============================");
        System.out.println(" FIVE JOB LIMIT REACHED");
        System.out.println("==============================");

        System.out.println(
                "Worker "
                + oldCoordinator
                + " completed Term "
                + finishedTerm
        );

        TermEndMessage termEndMessage =
                new TermEndMessage(
                        oldCoordinator,
                        finishedTerm
                );

        coordinatorManager
                .processTermEndMessage(
                        termEndMessage
                );

        for (WorkerInfo neighbour
                : getNeighbours()) {

            try {

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveTermEnd(
                        termEndMessage,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[TERM] Could not inform Worker "
                        + neighbour.getId()
                );
            }
        }

        System.out.println();
        System.out.println(
                "[TERM] No coordinator is active."
        );

        System.out.println(
                "[TERM] Worker "
                + id
                + " initiating new election."
        );

        startElection();
    }

    @Override
    public void receiveTermEnd(
            TermEndMessage message,
            int senderId)
            throws RemoteException {

        if (message == null) {
            return;
        }

        boolean accepted =
                coordinatorManager
                        .processTermEndMessage(
                                message
                        );

        if (!accepted) {

            System.out.println(
                    "[TERM] Worker "
                    + id
                    + " ignored duplicate/old TERM-END."
            );

            return;
        }

        System.out.println(
                "[TERM] Worker "
                + id
                + " now has no active coordinator."
        );

        for (WorkerInfo neighbour
                : getNeighbours()) {

            if (neighbour.getId()
                    == senderId) {

                continue;
            }

            try {

                WorkerService remote =
                        getRemoteWorker(
                                neighbour
                        );

                remote.receiveTermEnd(
                        message,
                        id
                );

            } catch (Exception e) {

                System.err.println(
                        "[TERM] Could not forward to Worker "
                        + neighbour.getId()
                );
            }
        }
    }

    private WorkerInfo findNeighbour(
            int workerId) {

        synchronized (neighbours) {

            for (WorkerInfo neighbour
                    : neighbours) {

                if (neighbour.getId()
                        == workerId) {

                    return neighbour;
                }
            }
        }

        return null;
    }

    public void shutdown() {

        try {

            if (bootstrap != null) {

                bootstrap.unregisterWorker(
                        id
                );
            }

        } catch (Exception ignored) {
        }

        scheduler.shutdownNow();

        jobExecutor.shutdownNow();

        System.out.println(
                "[Worker "
                + id
                + "] Shutdown complete."
        );
    }

    private static class JobAssignment {

        private final WorkerInfo worker;
        private final JobRequest request;

        private JobAssignment(
                WorkerInfo worker,
                JobRequest request) {

            this.worker = worker;
            this.request = request;
        }
    }

    public static void main(
            String[] args) {

        if (args.length < 1) {

            System.err.println(
                    "Usage: WorkerNode "
                    + "<id> [host] [port] "
                    + "[bootstrapHost] [bootstrapPort]"
            );

            System.exit(1);
        }

        int workerId =
                Integer.parseInt(
                        args[0]
                );

        String host =
                args.length > 1
                ? args[1]
                : "localhost";

        int port =
                args.length > 2
                ? Integer.parseInt(
                        args[2]
                )
                : 1100 + workerId;

        String bootstrapHost =
                args.length > 3
                ? args[3]
                : "localhost";

        int bootstrapPort =
                args.length > 4
                ? Integer.parseInt(
                        args[4]
                )
                : 1099;

        try {

            WorkerNode node =
                    new WorkerNode(
                            workerId,
                            host,
                            port,
                            bootstrapHost,
                            bootstrapPort
                    );

            node.start();

            Runtime.getRuntime()
                    .addShutdownHook(
                            new Thread(
                                    node::shutdown
                            )
                    );

            Thread.currentThread()
                    .join();

        } catch (Exception e) {

            System.err.println(
                    "[Worker] Failed to start: "
                    + e.getMessage()
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}