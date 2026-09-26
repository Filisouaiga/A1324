/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.election;

/**
 *
 * @author janth
 */

import java.io.Serializable;
import java.util.UUID;

/**
 * Message used to announce the elected coordinator.
 */
public class CoordinatorMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // Required assignment value
    private static final String LEADERMAN = "cs324";

    // Unique ID for this coordinator message
    private final String messageId;

    // Election that produced this coordinator
    private final String electionId;

    // Worker selected as coordinator
    private final int coordinatorId;

    // Coordinator term number
    private final int term;

    public CoordinatorMessage(
            String electionId,
            int coordinatorId,
            int term) {

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

        return "COORDINATOR"
                + " | Worker=" + coordinatorId
                + " | Term=" + term
                + " | Election=" + electionId
                + " | leaderman=" + LEADERMAN;
    }
}