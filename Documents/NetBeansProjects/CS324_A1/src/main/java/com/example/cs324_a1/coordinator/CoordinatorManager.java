/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.coordinator;

import com.example.cs324_a1.election.CoordinatorMessage;

import java.util.HashSet;
import java.util.Set;

public class CoordinatorManager {

    private int coordinatorId = -1;
    private int currentTerm = 0;
    private int jobsAssignedThisTerm = 0;
    private static final int MAX_JOBS_PER_TERM = 5;

    private final Set<String> processedCoordinatorMessages = new HashSet<>();

    private final Set<String> processedTermEndMessages = new HashSet<>();

    public synchronized boolean hasCoordinator() {

        return coordinatorId != -1;
    }

    public synchronized int getCoordinatorId() {

        return coordinatorId;
    }

    public synchronized int getCurrentTerm() {

        return currentTerm;
    }

    public synchronized int nextTerm() {

        return currentTerm + 1;
    }

    public synchronized boolean processCoordinatorMessage(CoordinatorMessage message) {

        if (message == null) {

            return false;
        }

        if (processedCoordinatorMessages.contains(message.getMessageId())) {

            System.out.println("[COORDINATOR] Duplicate message ignored.");

            return false;
        }

        processedCoordinatorMessages.add(message.getMessageId());

        if (message.getTerm() < currentTerm) {

            System.out.println("[COORDINATOR] Old coordinator term ignored.");

            return false;
        }

        if (message.getTerm() == currentTerm && hasCoordinator() && coordinatorId != message.getCoordinatorId()) {

            System.out.println("[COORDINATOR] Conflicting coordinator " + "for current term ignored.");

            return false;
        }

        coordinatorId = message.getCoordinatorId();

        currentTerm = message.getTerm();

        jobsAssignedThisTerm = 0;

        System.out.println();
        System.out.println("==============================");
        System.out.println(" NEW COORDINATOR");
        System.out.println("==============================");

        System.out.println("Coordinator : Worker " + coordinatorId);

        System.out.println("Term        : " + currentTerm);

        System.out.println("Jobs        : " + jobsAssignedThisTerm + "/" + MAX_JOBS_PER_TERM );

        System.out.println("leaderman   : " + message.getLeaderman());

        return true;
    }

    public synchronized int
            getProcessedCoordinatorMessageCount() {

        return processedCoordinatorMessages.size();
    }

    public synchronized boolean recordJobAssignment() {

        if (!hasCoordinator()) {

            System.out.println("[TERM] No active coordinator.");

            return false;
        }

        if (jobsAssignedThisTerm >= MAX_JOBS_PER_TERM) {

            System.out.println("[TERM] Five-job limit already reached.");

            return true;
        }

        jobsAssignedThisTerm++;

        System.out.println();

        System.out.println("[TERM] Coordinator Worker " + coordinatorId + " completed job " + jobsAssignedThisTerm + "/" + MAX_JOBS_PER_TERM);

        return jobsAssignedThisTerm >= MAX_JOBS_PER_TERM;
    }

    public synchronized int getJobsAssignedThisTerm() {

        return jobsAssignedThisTerm;
    }

    public synchronized int getMaxJobsPerTerm() {

        return MAX_JOBS_PER_TERM;
    }

    public synchronized boolean hasReachedJobLimit() {

        return jobsAssignedThisTerm >= MAX_JOBS_PER_TERM;
    }

    public synchronized void endTerm() {

        if (!hasCoordinator()) {

            return;
        }

        System.out.println();
        System.out.println("==============================");
        System.out.println(" COORDINATOR TERM ENDED");
        System.out.println("==============================");

        System.out.println("Coordinator : Worker " + coordinatorId );

        System.out.println("Term        : " + currentTerm );

        System.out.println("Jobs        : " + jobsAssignedThisTerm + " / " + MAX_JOBS_PER_TERM);

        coordinatorId = -1;

        jobsAssignedThisTerm = 0;

        System.out.println("[TERM] No active coordinator.");
    }

    public synchronized boolean processTermEndMessage(
            TermEndMessage message) {

        if (message == null) {

            return false;
        }

        if (processedTermEndMessages.contains(
                message.getMessageId())) {

            System.out.println("[TERM] Duplicate TERM-END message ignored.");

            return false;
        }

        processedTermEndMessages.add(message.getMessageId());

        if (message.getTerm() < currentTerm) {

            System.out.println("[TERM] Old TERM-END message ignored.");

            return false;
        }

        if (message.getTerm() > currentTerm) {

            System.out.println("[TERM] Invalid future TERM-END message ignored.");

            return false;
        }

        if (hasCoordinator() && coordinatorId != message.getCoordinatorId()) {

            System.out.println("[TERM] TERM-END coordinator does not match.");

            return false;
        }

        System.out.println();

        System.out.println("[TERM] Worker received TERM-END:");

        System.out.println("Coordinator : Worker "+ message.getCoordinatorId());

        System.out.println("Term       : " + message.getTerm());

        coordinatorId = -1;

        jobsAssignedThisTerm = 0;

        System.out.println("[TERM] Term "+ currentTerm+ " is now closed.");

        System.out.println("[TERM] Coordinator = NONE");

        return true;
    }

    public synchronized int
            getProcessedTermEndMessageCount() {

        return processedTermEndMessages.size();
    }
}