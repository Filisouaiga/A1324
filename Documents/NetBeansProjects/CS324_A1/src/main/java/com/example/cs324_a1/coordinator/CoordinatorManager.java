/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.coordinator;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.election.CoordinatorMessage;
import java.util.HashSet;
import java.util.Set;

public class CoordinatorManager {

    // Current coordinator
    private int coordinatorId = -1;

    // Current term number
    private int currentTerm = 0;

    // Jobs assigned during current term
    private int jobsAssignedThisTerm = 0;

    // Maximum jobs allowed per term
    private static final int MAX_JOBS_PER_TERM = 5;

    // Coordinator messages already processed
    private final Set<String> processedCoordinatorMessages =
            new HashSet<>();

    // Term-end messages already processed
    private final Set<String> processedTermEndMessages =
            new HashSet<>();

    // =========================================================
    // COORDINATOR STATUS
    // =========================================================

    public synchronized boolean hasCoordinator() {
        return coordinatorId != -1;
    }

    public synchronized int getCoordinatorId() {
        return coordinatorId;
    }

    public synchronized int getCurrentTerm() {
        return currentTerm;
    }

    // =========================================================
    // NEXT TERM
    // =========================================================

    public synchronized int nextTerm() {
        return currentTerm + 1;
    }

    // =========================================================
    // PROCESS COORDINATOR MESSAGE
    // =========================================================

    public synchronized boolean processCoordinatorMessage(
            CoordinatorMessage message) {

        if (message == null) {
            return false;
        }

        // Ignore duplicate message
        if (processedCoordinatorMessages.contains(
                message.getMessageId())) {

            System.out.println(
                    "[COORDINATOR] Duplicate message ignored."
            );

            return false;
        }

        processedCoordinatorMessages.add(
                message.getMessageId()
        );

        // Ignore coordinator announcement from older term
        if (message.getTerm() < currentTerm) {

            System.out.println(
                    "[COORDINATOR] Old coordinator term ignored."
            );

            return false;
        }

        // Store coordinator
        coordinatorId =
                message.getCoordinatorId();

        // Store term
        currentTerm =
                message.getTerm();

        // New coordinator term begins at zero jobs
        jobsAssignedThisTerm = 0;

        System.out.println();
        System.out.println("==============================");
        System.out.println(" NEW COORDINATOR");
        System.out.println("==============================");

        System.out.println(
                "Coordinator : Worker "
                + coordinatorId
        );

        System.out.println(
                "Term        : "
                + currentTerm
        );

        System.out.println(
                "Jobs        : "
                + jobsAssignedThisTerm
                + "/"
                + MAX_JOBS_PER_TERM
        );

        System.out.println(
                "leaderman   : "
                + message.getLeaderman()
        );

        return true;
    }

    // =========================================================
    // RECORD JOB ASSIGNMENT
    // =========================================================

    /**
     * Records one job during the coordinator's current term.
     *
     * Returns true when the coordinator reaches the
     * five-job limit.
     */
    public synchronized boolean recordJobAssignment() {

        if (!hasCoordinator()) {

            System.out.println(
                    "[TERM] No active coordinator."
            );

            return false;
        }

        /*
         * Do not allow the counter to go beyond 5.
         */
        if (jobsAssignedThisTerm
                >= MAX_JOBS_PER_TERM) {

            return true;
        }

        jobsAssignedThisTerm++;

        System.out.println();
        System.out.println(
                "[TERM] Coordinator Worker "
                + coordinatorId
                + " assigned job "
                + jobsAssignedThisTerm
                + "/"
                + MAX_JOBS_PER_TERM
        );

        // true means term must now finish
        return jobsAssignedThisTerm
                >= MAX_JOBS_PER_TERM;
    }

    // =========================================================
    // JOB INFORMATION
    // =========================================================

    public synchronized int getJobsAssignedThisTerm() {
        return jobsAssignedThisTerm;
    }

    public synchronized int getMaxJobsPerTerm() {
        return MAX_JOBS_PER_TERM;
    }

    public synchronized boolean hasReachedJobLimit() {

        return jobsAssignedThisTerm
                >= MAX_JOBS_PER_TERM;
    }

    // =========================================================
    // END LOCAL TERM
    // =========================================================

    /**
     * Ends the coordinator term on this worker.
     *
     * The term number is NOT reset.
     *
     * Example:
     *
     * Term 1 finishes:
     * currentTerm = 1
     * coordinatorId = -1
     *
     * Therefore nextTerm() returns 2.
     */
    public synchronized void endTerm() {

        if (!hasCoordinator()) {
            return;
        }

        System.out.println();
        System.out.println("==============================");
        System.out.println(" COORDINATOR TERM ENDED");
        System.out.println("==============================");

        System.out.println(
                "Coordinator : Worker "
                + coordinatorId
        );

        System.out.println(
                "Term        : "
                + currentTerm
        );

        System.out.println(
                "Jobs        : "
                + jobsAssignedThisTerm
                + "/"
                + MAX_JOBS_PER_TERM
        );

        // Remove current coordinator
        coordinatorId = -1;

        // Reset job counter for future term
        jobsAssignedThisTerm = 0;

        /*
         * DO NOT reset currentTerm.
         *
         * We need it so the next term becomes:
         *
         * currentTerm + 1
         */
        System.out.println(
                "[TERM] No active coordinator."
        );
    }

    // =========================================================
    // PROCESS TERM-END MESSAGE
    // =========================================================

    /**
     * Called when another worker receives a TermEndMessage.
     *
     * Returns true if this is a new valid term-end message.
     * Returning true tells WorkerNode to forward the message
     * to its other neighbours.
     */
    public synchronized boolean processTermEndMessage(
            TermEndMessage message) {

        if (message == null) {
            return false;
        }

        // =====================================================
        // DUPLICATE CHECK
        // =====================================================

        if (processedTermEndMessages.contains(
                message.getMessageId())) {

            System.out.println(
                    "[TERM] Duplicate TERM-END message ignored."
            );

            return false;
        }

        processedTermEndMessages.add(
                message.getMessageId()
        );

        // =====================================================
        // OLD TERM CHECK
        // =====================================================

        if (message.getTerm() < currentTerm) {

            System.out.println(
                    "[TERM] Old TERM-END message ignored."
            );

            return false;
        }

        // =====================================================
        // FUTURE TERM CHECK
        // =====================================================

        if (message.getTerm() > currentTerm) {

            System.out.println(
                    "[TERM] Invalid future TERM-END message ignored."
            );

            return false;
        }

        // =====================================================
        // COORDINATOR CHECK
        // =====================================================

        /*
         * If we currently have a coordinator, the term-end
         * message should refer to that same coordinator.
         */
        if (hasCoordinator()
                && coordinatorId
                != message.getCoordinatorId()) {

            System.out.println(
                    "[TERM] TERM-END coordinator does not match."
            );

            return false;
        }

        // =====================================================
        // END TERM
        // =====================================================

        System.out.println();
        System.out.println(
                "[TERM] Worker received TERM-END:"
        );

        System.out.println(
                "Coordinator : Worker "
                + message.getCoordinatorId()
        );

        System.out.println(
                "Term        : "
                + message.getTerm()
        );

        /*
         * Every worker now agrees that there is
         * no active coordinator.
         */
        coordinatorId = -1;

        jobsAssignedThisTerm = 0;

        System.out.println(
                "[TERM] Term "
                + currentTerm
                + " is now closed."
        );

        System.out.println(
                "[TERM] Coordinator = NONE"
        );

        return true;
    }
}