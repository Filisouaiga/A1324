/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

import com.example.cs324_a1.common.WorkerInfo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ElectionManager {

    // Counts elections created by this worker
    private final AtomicInteger electionCounter = new AtomicInteger(0);

    private final Set<String> processedElections = new HashSet<>();

    private final Map<String, Integer> electionParents = new HashMap<>();

    private final Map<String, Set<Integer>> pendingReplies = new HashMap<>();
 
    private final Map<String, List<WorkerInfo>> collectedCandidates = new HashMap<>();
    
    private final Set<String> repliesSent = new HashSet<>();

    
    // Create a new unique election
    public ElectionMessage createElection(int initiatorId) {

        // Increase the local election number
        int number = electionCounter.incrementAndGet();

        // Create a short unique code
        String uniqueCode = UUID.randomUUID()
                .toString()
                .substring(0, 4)
                .toUpperCase();

        // Create a readable election ID
        String electionId =
                "ELECTION-W"
                + initiatorId
                + "-"
                + String.format("%03d", number)
                + "-"
                + uniqueCode;

        ElectionMessage message =
                new ElectionMessage(
                        electionId,
                        initiatorId
                );

        System.out.println();
        System.out.println("==============================");
        System.out.println(" NEW ELECTION CREATED");
        System.out.println("==============================");
        System.out.println("Election ID : " + electionId);
        System.out.println("Initiator   : Worker " + initiatorId);

        return message;
    }

    public synchronized boolean shouldProcess(
            String electionId) {

        if (electionId == null) {
            return false;
        }

        if (processedElections.contains(electionId)) {

            System.out.println("[ELECTION] Duplicate ignored: " + electionId);

            return false;
        }

        processedElections.add(electionId);

        return true;
    }

    public synchronized boolean hasProcessed(
            String electionId) {

        return processedElections.contains(electionId);
    }

    public synchronized int getProcessedElectionCount() {

        return processedElections.size();
    }

    public synchronized void recordParent(String electionId, int senderId) {

        electionParents.putIfAbsent(electionId, senderId);
    }

    public synchronized int getParent(String electionId) {

        return electionParents.getOrDefault(electionId, -1);
    }

    public synchronized boolean hasParent(String electionId) {

        return electionParents.containsKey(electionId);
    }

    public synchronized void initializeElectionState(String electionId, WorkerInfo self, List<Integer> expectedNeighbours) {

        List<WorkerInfo> candidates = new ArrayList<>();

        if (self != null) {

            candidates.add(self);
        }

        collectedCandidates.put(electionId, candidates);

        Set<Integer> waiting = new HashSet<>();

        if (expectedNeighbours != null) {

            waiting.addAll(expectedNeighbours);
        }

        pendingReplies.put(electionId, waiting);

        repliesSent.remove(electionId);
    }

    public synchronized void addCandidate(String electionId, WorkerInfo worker) {

        if (worker == null) {
            return;
        }

        List<WorkerInfo> candidates = collectedCandidates.computeIfAbsent(electionId, key -> new ArrayList<>());

        for (int i = 0; i < candidates.size(); i++) {

            WorkerInfo existing = candidates.get(i);

            if (existing.getWorkerId() == worker.getWorkerId()) {

                candidates.set(i, worker);

                return;
            }
        }

        candidates.add(worker);
    }

    public synchronized void addCandidates(String electionId, List<WorkerInfo> workers) {

        if (workers == null) {
            return;
        }

        for (WorkerInfo worker : workers) {

            addCandidate(electionId, worker);
        }
    }

    public synchronized void processReply(ElectionReply reply) {

        if (reply == null) {
            return;
        }

        String electionId = reply.getElectionId();

        addCandidates(electionId, reply.getCandidates());

        Set<Integer> waiting = pendingReplies.get(electionId);

        if (waiting != null) {

            waiting.remove(reply.getSenderId());
        }

        System.out.println("[ELECTION] Reply received from Worker " + reply.getSenderId());

        System.out.println("[ELECTION] Candidates collected = "+ getCollectedCandidates(electionId).size());

        System.out.println("[ELECTION] Replies remaining = " + getPendingReplyCount(electionId));
    }

    public synchronized boolean hasPendingReplies(
            String electionId) {

        Set<Integer> waiting = pendingReplies.get(electionId);

        return waiting != null && !waiting.isEmpty();
    }

    public synchronized int getPendingReplyCount(
            String electionId) {

        Set<Integer> waiting = pendingReplies.get(electionId);

        if (waiting == null) {
            return 0;
        }

        return waiting.size();
    }

    public synchronized Set<Integer> getPendingReplies(
            String electionId) {

        Set<Integer> waiting = pendingReplies.get(electionId);

        if (waiting == null) {

            return new HashSet<>();
        }

        return new HashSet<>(waiting);
    }

    public synchronized List<WorkerInfo>getCollectedCandidates(String electionId) {

        List<WorkerInfo> candidates = collectedCandidates.get(electionId);

        if (candidates == null) {

            return new ArrayList<>();
        }

        return new ArrayList<>(candidates);
    }

    public synchronized boolean markReplySent(String electionId) {

        if (repliesSent.contains(electionId)) {

            return false;
        }

        repliesSent.add(electionId);

        return true;
    }

    public synchronized boolean hasSentReply(String electionId) {

        return repliesSent.contains(electionId);
    }

    public synchronized ElectionReply createReply(String electionId, int senderId) {

        ElectionReply reply = new ElectionReply(electionId, senderId);

        reply.addCandidates(getCollectedCandidates(electionId));

        return reply;
    }

    // Select coordinator using JAC and Worker ID
    public WorkerInfo selectCoordinator(List<WorkerInfo> workers) {

        if (workers == null || workers.isEmpty()) {
            return null;
        }

        WorkerInfo winner = null;
        int lowestJac = Integer.MAX_VALUE;
        List<Integer> eligibleWorkers = new ArrayList<>();

        System.out.println();
        System.out.println("==============================");
        System.out.println(" COORDINATOR SELECTION");
        System.out.println("==============================");

        // Display workers and find the lowest JAC
        for (WorkerInfo worker : workers) {

            if (worker == null) {
                continue;
            }

            System.out.println(
                    "Worker " + worker.getWorkerId()
                    + " | JAC = " + worker.getJac()
            );

            if (worker.getJac() < lowestJac) {

                lowestJac = worker.getJac();

                eligibleWorkers.clear();
                eligibleWorkers.add(worker.getWorkerId());

                winner = worker;

            } else if (worker.getJac() == lowestJac) {

                eligibleWorkers.add(worker.getWorkerId());

                // Highest ID wins when JAC is equal
                if (winner == null
                        || worker.getWorkerId() > winner.getWorkerId()) {

                    winner = worker;
                }
            }
        }

        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Lowest JAC       : " + lowestJac);
        System.out.println("Eligible Workers : " + eligibleWorkers);

        if (eligibleWorkers.size() > 1) {

            System.out.println("Tie Detected     : YES");
            System.out.println("Tie-Break Rule   : Highest Worker ID");

        } else {

            System.out.println("Tie Detected     : NO");
            System.out.println("Selection Rule   : Lowest JAC");
        }

        if (winner != null) {

            System.out.println();
            System.out.println(
                    "Selected Worker  : Worker "
                    + winner.getWorkerId()
            );

            if (eligibleWorkers.size() > 1) {

                System.out.println(
                        "Reason           : Lowest JAC tied, highest ID wins"
                );

            } else {

                System.out.println(
                        "Reason           : Worker has the lowest JAC"
                );
            }
        }

        System.out.println("------------------------------");

        return winner;
    }

    public synchronized void clearElection(String electionId) {

        electionParents.remove(electionId);

        pendingReplies.remove(electionId);

        collectedCandidates.remove(electionId);

        repliesSent.remove(electionId);
    }
}