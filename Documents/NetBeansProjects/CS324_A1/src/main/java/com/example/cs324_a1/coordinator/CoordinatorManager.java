/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.coordinator;

/**
 *
 * @author janth
 */

public class CoordinatorManager {

    private int coordinatorId = -1;
    private int jobsAssignedThisTerm = 0;

    private static final int MAX_JOBS_PER_TERM = 5;

    public void setCoordinator(int coordinatorId) {

        this.coordinatorId = coordinatorId;
        this.jobsAssignedThisTerm = 0;

        System.out.println(
                "Worker " + coordinatorId
                + " is now the coordinator."
        );
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public boolean hasCoordinator() {
        return coordinatorId != -1;
    }

    public boolean recordJobAssignment() {

        if (!hasCoordinator()) {
            return false;
        }

        jobsAssignedThisTerm++;

        System.out.println(
                "Coordinator " + coordinatorId
                + " assigned job "
                + jobsAssignedThisTerm + "/5"
        );

        return jobsAssignedThisTerm >= MAX_JOBS_PER_TERM;
    }

    public void endTerm() {

        System.out.println(
                "Coordinator " + coordinatorId
                + " has completed its term."
        );

        coordinatorId = -1;
        jobsAssignedThisTerm = 0;
    }
}
