/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

import com.example.cs324_a1.common.WorkerInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ElectionMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String electionId;
    private final int initiatorId;
    
    private final List<WorkerInfo> candidates;

    public ElectionMessage(String electionId, int initiatorId) {
        this.electionId = electionId;
        this.initiatorId = initiatorId;
        this.candidates = new ArrayList<>();
    }

    public String getElectionId() {
        return electionId;
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    public List<WorkerInfo> getCandidates() {
        return candidates;
    }

    public void addCandidate(WorkerInfo worker) {

        if (worker == null) {
            return;
        }

        for (WorkerInfo existing : candidates) {

            if (existing.getWorkerId() == worker.getWorkerId()) {
                return;
            }
        }

        candidates.add(worker);
    }

    public boolean containsCandidate(int workerId) {

        for (WorkerInfo worker : candidates) {

            if (worker.getWorkerId() == workerId) {
                return true;
            }
        }

        return false;
    }

    public int getCandidateCount() {
        return candidates.size();
    }

    @Override
    public String toString() {

        return "ElectionMessage{" + "electionId='" + electionId + '\'' + ", initiatorId=" + initiatorId + ", candidates=" + candidates.size() + '}';
    }
}