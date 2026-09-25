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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ElectionManager {

    private final Set<String> processedElections = new HashSet<>();

    public ElectionMessage createElection(int initiatorId) {

        String electionId = UUID.randomUUID().toString();

        return new ElectionMessage(
                electionId,
                initiatorId
        );
    }

    public synchronized boolean shouldProcess(String electionId) {

        if (processedElections.contains(electionId)) {
            return false;
        }

        processedElections.add(electionId);
        return true;
    }

    public WorkerInfo selectCoordinator(List<WorkerInfo> workers) {

        if (workers == null || workers.isEmpty()) {
            return null;
        }

        WorkerInfo winner = workers.get(0);

        for (WorkerInfo worker : workers) {

            // Lowest JAC wins
            if (worker.getJac() < winner.getJac()) {

                winner = worker;

            // Same JAC -> highest ID wins
            } else if (worker.getJac() == winner.getJac()
                    && worker.getWorkerId() > winner.getWorkerId()) {

                winner = worker;
            }
        }

        return winner;
    }
}
