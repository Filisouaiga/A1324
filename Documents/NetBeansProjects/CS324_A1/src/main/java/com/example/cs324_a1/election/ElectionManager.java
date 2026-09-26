/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.common.WorkerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ElectionManager {

    // Elections already processed by this worker
    private final Set<String> processedElections =
            new HashSet<>();

    // Parent / first sender for each election
    private final Map<String, Integer> electionParents =
            new HashMap<>();

    // Neighbours that this worker is waiting for
    private final Map<String, Set<Integer>> pendingReplies =
            new HashMap<>();

    // Candidates collected for each election
    private final Map<String, List<WorkerInfo>> collectedCandidates =
            new HashMap<>();

    // Prevent a worker from sending its final reply twice
    private final Set<String> repliesSent =
            new HashSet<>();

    // =========================================================
    // CREATE ELECTION
    // =========================================================

    public ElectionMessage createElection(int initiatorId) {

        String electionId =
                UUID.randomUUID().toString();

        ElectionMessage message =
                new ElectionMessage(
                        electionId,
                        initiatorId
                );

        System.out.println();
        System.out.println("==============================");
        System.out.println(" NEW ELECTION CREATED");
        System.out.println("==============================");

        System.out.println(
                "Election ID : "
                + electionId
        );

        System.out.println(
                "Initiator   : Worker "
                + initiatorId
        );

        return message;
    }

    // =========================================================
    // DUPLICATE ELECTION DETECTION
    // =========================================================

    public synchronized boolean shouldProcess(
            String electionId) {

        if (processedElections.contains(electionId)) {

            System.out.println(
                    "[ELECTION] Duplicate ignored: "
                    + electionId
            );

            return false;
        }

        processedElections.add(electionId);

        return true;
    }

    public synchronized boolean hasProcessed(
            String electionId) {

        return processedElections.contains(electionId);
    }

    // =========================================================
    // PARENT
    // =========================================================

    /**
     * The first worker that sent us the election becomes
     * our parent for this election.
     */
    public synchronized void recordParent(
            String electionId,
            int senderId) {

        electionParents.putIfAbsent(
                electionId,
                senderId
        );
    }

    public synchronized int getParent(
            String electionId) {

        return electionParents.getOrDefault(
                electionId,
                -1
        );
    }

    public synchronized boolean hasParent(
            String electionId) {

        return electionParents.containsKey(
                electionId
        );
    }

    // =========================================================
    // INITIALISE ELECTION STATE
    // =========================================================

    /**
     * Creates the local state needed to collect replies.
     *
     * expectedNeighbours contains the IDs of the workers
     * that this worker forwarded the ELECTION message to.
     *
     * IMPORTANT:
     * This must be called BEFORE forwarding the election.
     */
    public synchronized void initializeElectionState(
            String electionId,
            WorkerInfo self,
            List<Integer> expectedNeighbours) {

        // Candidate list
        List<WorkerInfo> candidates =
                new ArrayList<>();

        if (self != null) {
            candidates.add(self);
        }

        collectedCandidates.put(
                electionId,
                candidates
        );

        // Workers we expect replies from
        Set<Integer> waiting =
                new HashSet<>();

        if (expectedNeighbours != null) {
            waiting.addAll(expectedNeighbours);
        }

        pendingReplies.put(
                electionId,
                waiting
        );

        // Make sure old reply state is removed
        repliesSent.remove(electionId);
    }

    // =========================================================
    // ADD CANDIDATE
    // =========================================================

    public synchronized void addCandidate(
            String electionId,
            WorkerInfo worker) {

        if (worker == null) {
            return;
        }

        List<WorkerInfo> candidates =
                collectedCandidates.computeIfAbsent(
                        electionId,
                        key -> new ArrayList<>()
                );

        // Check if this worker already exists
        for (int i = 0; i < candidates.size(); i++) {

            WorkerInfo existing =
                    candidates.get(i);

            if (existing.getWorkerId()
                    == worker.getWorkerId()) {

                /*
                 * Replace the old copy with the latest
                 * WorkerInfo received.
                 *
                 * This ensures the latest JAC is used.
                 */
                candidates.set(i, worker);

                return;
            }
        }

        // New worker
        candidates.add(worker);
    }

    // =========================================================
    // ADD MULTIPLE CANDIDATES
    // =========================================================

    public synchronized void addCandidates(
            String electionId,
            List<WorkerInfo> workers) {

        if (workers == null) {
            return;
        }

        for (WorkerInfo worker : workers) {
            addCandidate(
                    electionId,
                    worker
            );
        }
    }

    // =========================================================
    // RECEIVE REPLY
    // =========================================================

    /**
     * Processes an ElectionReply.
     *
     * 1. Add candidates from the reply.
     * 2. Remove the sender from pending replies.
     */
    public synchronized void processReply(
            ElectionReply reply) {

        if (reply == null) {
            return;
        }

        String electionId =
                reply.getElectionId();

        // Merge candidates
        addCandidates(
                electionId,
                reply.getCandidates()
        );

        // This sender has now replied
        Set<Integer> waiting =
                pendingReplies.get(electionId);

        if (waiting != null) {

            waiting.remove(
                    reply.getSenderId()
            );
        }

        System.out.println(
                "[ELECTION] Reply received from Worker "
                + reply.getSenderId()
        );

        System.out.println(
                "[ELECTION] Candidates collected = "
                + getCollectedCandidates(
                        electionId
                ).size()
        );

        System.out.println(
                "[ELECTION] Replies remaining = "
                + getPendingReplyCount(
                        electionId
                )
        );
    }

    // =========================================================
    // PENDING REPLIES
    // =========================================================

    public synchronized boolean hasPendingReplies(
            String electionId) {

        Set<Integer> waiting =
                pendingReplies.get(electionId);

        return waiting != null
                && !waiting.isEmpty();
    }

    public synchronized int getPendingReplyCount(
            String electionId) {

        Set<Integer> waiting =
                pendingReplies.get(electionId);

        if (waiting == null) {
            return 0;
        }

        return waiting.size();
    }

    public synchronized Set<Integer> getPendingReplies(
            String electionId) {

        Set<Integer> waiting =
                pendingReplies.get(electionId);

        if (waiting == null) {
            return new HashSet<>();
        }

        // Return copy so outside code cannot change
        // ElectionManager's internal state
        return new HashSet<>(waiting);
    }

    // =========================================================
    // GET COLLECTED CANDIDATES
    // =========================================================

    public synchronized List<WorkerInfo>
            getCollectedCandidates(
                    String electionId) {

        List<WorkerInfo> candidates =
                collectedCandidates.get(
                        electionId
                );

        if (candidates == null) {
            return new ArrayList<>();
        }

        // Return a copy
        return new ArrayList<>(
                candidates
        );
    }

    // =========================================================
    // REPLY SENT
    // =========================================================

    /**
     * Prevents the same worker from sending its completed
     * election reply more than once.
     */
    public synchronized boolean markReplySent(
            String electionId) {

        if (repliesSent.contains(
                electionId)) {

            return false;
        }

        repliesSent.add(electionId);

        return true;
    }

    public synchronized boolean hasSentReply(
            String electionId) {

        return repliesSent.contains(
                electionId
        );
    }

    // =========================================================
    // CREATE REPLY
    // =========================================================

    /**
     * Creates a reply containing everything this worker
     * discovered in its section of the network.
     */
    public synchronized ElectionReply createReply(
            String electionId,
            int senderId) {

        ElectionReply reply =
                new ElectionReply(
                        electionId,
                        senderId
                );

        reply.addCandidates(
                getCollectedCandidates(
                        electionId
                )
        );

        return reply;
    }

    // =========================================================
    // SELECT COORDINATOR
    // =========================================================

    /**
     * Assignment rule:
     *
     * 1. Lowest JAC wins.
     * 2. If JAC is equal, highest Worker ID wins.
     */
    public WorkerInfo selectCoordinator(
            List<WorkerInfo> workers) {

        if (workers == null
                || workers.isEmpty()) {

            return null;
        }

        WorkerInfo winner = null;

        System.out.println();
        System.out.println("==============================");
        System.out.println(" COORDINATOR SELECTION");
        System.out.println("==============================");

        for (WorkerInfo worker : workers) {

            if (worker == null) {
                continue;
            }

            System.out.println(
                    "Worker "
                    + worker.getWorkerId()
                    + " | JAC = "
                    + worker.getJac()
            );

            // First valid worker
            if (winner == null) {

                winner = worker;

                continue;
            }

            // Lower JAC wins
            if (worker.getJac()
                    < winner.getJac()) {

                winner = worker;

            // Same JAC -> highest ID wins
            } else if (
                    worker.getJac()
                    == winner.getJac()
                    && worker.getWorkerId()
                    > winner.getWorkerId()) {

                winner = worker;
            }
        }

        if (winner != null) {

            System.out.println(
                    "------------------------------"
            );

            System.out.println(
                    "Winner: Worker "
                    + winner.getWorkerId()
                    + " | JAC = "
                    + winner.getJac()
            );

            System.out.println(
                    "------------------------------"
            );
        }

        return winner;
    }

    // =========================================================
    // CLEAR ELECTION
    // =========================================================

    /**
     * Removes temporary information after the election
     * has completely finished.
     */
    public synchronized void clearElection(
            String electionId) {

        processedElections.remove(
                electionId
        );

        electionParents.remove(
                electionId
        );

        pendingReplies.remove(
                electionId
        );

        collectedCandidates.remove(
                electionId
        );

        repliesSent.remove(
                electionId
        );
    }
}