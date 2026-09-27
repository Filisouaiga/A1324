/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

import java.io.Serializable;
import java.util.UUID;

public class CoordinatorMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String LEADERMAN = "cs324";
    private final String messageId;
    private final String electionId;
    private final int coordinatorId;
    private final int term;

    public CoordinatorMessage(String electionId, int coordinatorId, int term) {

        this.messageId = UUID.randomUUID().toString();
        this.electionId = electionId;
        this.coordinatorId = coordinatorId;
        this.term = term;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getElectionId() {
        return electionId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public int getTerm() {
        return term;
    }

    public String getLeaderman() {
        return LEADERMAN;
    }

    @Override
    public String toString() {

        return "COORDINATOR" + " | Worker=" + coordinatorId + " | Term=" + term + " | Election=" + electionId + " | leaderman=" + LEADERMAN;
    }
}