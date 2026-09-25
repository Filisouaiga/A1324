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

public class CoordinatorMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String electionId;
    private final int coordinatorId;

    public CoordinatorMessage(String electionId, int coordinatorId) {
        this.electionId = electionId;
        this.coordinatorId = coordinatorId;
    }

    public String getElectionId() {
        return electionId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }
}
