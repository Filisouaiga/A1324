/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

import com.example.cs324_a1.common.WorkerInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ElectionReply implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String electionId;
    private final int senderId;

    private final List<WorkerInfo> candidates;

    public ElectionReply(String electionId, int senderId) {

        this.electionId = electionId;
        this.senderId = senderId;
        this.candidates = new ArrayList<>();
    }

    public String getElectionId() {
        return electionId;
    }

    public int getSenderId() {
        return senderId;
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

    public void addCandidates(List<WorkerInfo> workers) {

        if (workers == null) {
            return;
        }

        for (WorkerInfo worker : workers) {
            addCandidate(worker);
        }
    }

    public int getCandidateCount() {
        return candidates.size();
    }

    @Override
    public String toString() {

        return "ElectionReply{" + "electionId='" + electionId + '\'' + ", senderId=" + senderId + ", candidates=" + candidates.size() + '}';
    }
}