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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Reply sent back during a leader election.
 *
 * Each worker returns the active workers it discovered
 * back towards the worker that started the election.
 *
 * @author janth
 */
public class ElectionReply implements Serializable {

    private static final long serialVersionUID = 1L;

    // Election this reply belongs to
    private final String electionId;

    // Worker sending this reply
    private final int senderId;

    // Workers discovered through this part of the network
    private final List<WorkerInfo> candidates;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ElectionReply(String electionId, int senderId) {

        this.electionId = electionId;
        this.senderId = senderId;
        this.candidates = new ArrayList<>();
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getElectionId() {
        return electionId;
    }

    public int getSenderId() {
        return senderId;
    }

    public List<WorkerInfo> getCandidates() {
        return candidates;
    }

    // =========================================================
    // ADD ONE CANDIDATE
    // =========================================================

    public void addCandidate(WorkerInfo worker) {

        if (worker == null) {
            return;
        }

        // Prevent duplicate workers
        for (WorkerInfo existing : candidates) {

            if (existing.getWorkerId()
                    == worker.getWorkerId()) {

                return;
            }
        }

        candidates.add(worker);
    }

    // =========================================================
    // ADD MULTIPLE CANDIDATES
    // =========================================================

    public void addCandidates(List<WorkerInfo> workers) {

        if (workers == null) {
            return;
        }

        for (WorkerInfo worker : workers) {
            addCandidate(worker);
        }
    }

    // =========================================================
    // CANDIDATE COUNT
    // =========================================================

    public int getCandidateCount() {
        return candidates.size();
    }

    // =========================================================
    // TO STRING
    // =========================================================

    @Override
    public String toString() {

        return "ElectionReply{"
                + "electionId='" + electionId + '\''
                + ", senderId=" + senderId
                + ", candidates=" + candidates.size()
                + '}';
    }
}