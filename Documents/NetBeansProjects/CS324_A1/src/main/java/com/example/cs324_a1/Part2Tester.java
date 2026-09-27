/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1;

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.election.ElectionMessage;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobType;
import com.example.cs324_a1.rmi.BootstrapService;
import com.example.cs324_a1.rmi.WorkerService;

import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Part2Tester {

    private static final String BOOTSTRAP_URL = "rmi://localhost:1099/BootstrapService";
    private static int passed, failed;

    public static void main(String[] args) {
        try {
            BootstrapService bootstrap = (BootstrapService) Naming.lookup(BOOTSTRAP_URL);
            List<WorkerInfo> workers = bootstrap.getActiveWorkers();

            System.out.println("==================================================");
            System.out.println(" CS324 PERSON 2 - LEADER ELECTION & COORDINATOR");
            System.out.println("==================================================");

            if (workers.size() < 4) {
                fail("SETUP", "At least 4 active workers are required; found " + workers.size());
                summary();
                return;
            }

            workers.sort(Comparator.comparingInt(WorkerInfo::getId));
            ensureNoCoordinator(workers);

            // TEST 1
            heading(1, "NORMAL ELECTION");
            WorkerService initiator = getWorker(workers.get(0));
            int termBefore = maxTerm(workers);
            initiator.startElection();
            int coordinatorId = waitForAgreement(bootstrap, 8000);
            if (coordinatorId != -1 && maxTerm(bootstrap.getActiveWorkers()) > termBefore)
                pass("A real election elected Worker " + coordinatorId + ".");
            else
                fail("TEST 1", "Election did not produce coordinator agreement in a new term.");

            // TEST 2
            heading(2, "ELECTION THROUGH MULTIPLE NEIGHBOURS");
            workers = bootstrap.getActiveWorkers();
            WorkerInfo multiHopTarget = findNonNeighbourReachableTarget(workers, initiator.getId());
            if (multiHopTarget == null) {
                fail("TEST 2", "Current topology has no worker beyond the initiator's immediate neighbours.");
            } else {
                endCurrentTermForTest(workers);
                int before = getWorker(multiHopTarget).getProcessedElectionCount();
                initiator.startElection();
                int agreed = waitForAgreement(bootstrap, 8000);
                int after = getWorker(multiHopTarget).getProcessedElectionCount();

                if (agreed != -1 && after == before + 1)
                    pass("Worker " + multiHopTarget.getId()
                            + " processed the election although it is not an immediate neighbour of Worker "
                            + initiator.getId() + ".");
                else
                    fail("TEST 2", "Multi-hop election propagation was not observed.");
            }

            // TEST 3
            heading(3, "DUPLICATE ELECTION MESSAGE");
            workers = bootstrap.getActiveWorkers();
            WorkerService duplicateTarget = getWorker(workers.get(0));
            int senderId = chooseNeighbourSender(duplicateTarget);
            if (senderId == -1) {
                fail("TEST 3", "Target worker has no neighbour that can act as sender.");
            } else {
                String electionId = "duplicate-test-" + UUID.randomUUID();
                ElectionMessage duplicate = new ElectionMessage(electionId, senderId);
                int before = duplicateTarget.getProcessedElectionCount();

                duplicateTarget.receiveElection(duplicate, senderId);
                Thread.sleep(500);
                int afterFirst = duplicateTarget.getProcessedElectionCount();

                duplicateTarget.receiveElection(duplicate, senderId);
                Thread.sleep(500);
                int afterSecond = duplicateTarget.getProcessedElectionCount();

                if (afterFirst == before + 1 && afterSecond == afterFirst)
                    pass("The first ELECTION was processed once and the duplicate was ignored.");
                else
                    fail("TEST 3", "Duplicate ELECTION changed the unique processed-election count.");
            }

            // Restore a clean election state if duplicate test created no coordinator.
            workers = bootstrap.getActiveWorkers();
            if (waitForAgreement(bootstrap, 1500) == -1) {
                WorkerService w = getWorker(workers.get(0));
                if (!w.hasCoordinator()) w.startElection();
                waitForAgreement(bootstrap, 8000);
            }

            // TEST 4 + TEST 5 use the same genuine election.
            heading(4, "LOWEST JAC SELECTION");
            workers = bootstrap.getActiveWorkers();
            endCurrentTermForTest(workers);
            CandidateExpectation expected = expectedWinner(workers);
            getWorker(workers.get(0)).startElection();
            int actual = waitForAgreement(bootstrap, 8000);

            if (actual == expected.workerId)
                pass("Lowest-JAC rule selected Worker " + actual
                        + " (JAC=" + expected.jac + ").");
            else
                fail("TEST 4", "Expected Worker " + expected.workerId
                        + " from real JAC values, but coordinator was Worker " + actual + ".");

            heading(5, "SAME JAC - HIGHEST ID TIE BREAKER");
            if (expected.tiedIds.size() < 2) {
                fail("TEST 5", "No real minimum-JAC tie exists in the current worker state: "
                        + expected.tiedIds + ". Restart workers with equal JACs to exercise this required case.");
            } else {
                int highest = expected.tiedIds.stream().max(Integer::compareTo).orElse(-1);
                if (actual == highest)
                    pass("Minimum JAC was tied by " + expected.tiedIds
                            + "; highest ID Worker " + highest + " won.");
                else
                    fail("TEST 5", "Minimum-JAC tie was " + expected.tiedIds
                            + ", but Worker " + actual + " was elected.");
            }

            // TESTS 6 & 7 deliberately require a REAL worker process to become unavailable.
            heading(6, "WORKER BECOMES UNAVAILABLE");
            System.out.println("[ACTION REQUIRED] Stop/close the CURRENT COORDINATOR worker process now.");
            System.out.println("The tester will wait for Bootstrap's real heartbeat timeout (up to 50 seconds).");

            workers = bootstrap.getActiveWorkers();
            int failedCoordinatorId = waitForAgreement(bootstrap, 3000);
            int countBeforeFailure = workers.size();

            boolean removed = waitUntilWorkerRemoved(bootstrap, failedCoordinatorId, 50000);
            if (removed && bootstrap.getActiveCount() == countBeforeFailure - 1)
                pass("Bootstrap removed unavailable Worker " + failedCoordinatorId
                        + " using the real heartbeat mechanism.");
            else
                fail("TEST 6", "Worker " + failedCoordinatorId
                        + " was not removed by Bootstrap during the timeout.");

            heading(7, "NEW ELECTION WHEN COORDINATOR DISAPPEARS");
            int recoveredCoordinator = waitForAgreement(bootstrap, 20000);
            if (removed && recoveredCoordinator != -1 && recoveredCoordinator != failedCoordinatorId)
                pass("Remaining workers detected the lost coordinator and elected Worker "
                        + recoveredCoordinator + ".");
            else
                fail("TEST 7", "No genuine recovery election/agreement followed coordinator disappearance.");

            // TEST 8
            heading(8, "COORDINATOR AFTER FIVE JOBS");
            workers = bootstrap.getActiveWorkers();
            int first = waitForAgreement(bootstrap, 5000);
            if (first == -1) {
                fail("TEST 8", "No coordinator available for the five-job test.");
            } else {
                WorkerService coordinator = findWorker(workers, first);
                int oldTerm = coordinator.getCoordinatorTerm();

                for (int i = 1; i <= 5; i++) {
                    coordinator = requireCoordinator(bootstrap);
                    coordinator.submitJob(new JobRequest(
                            JobType.MAX, Arrays.asList(i, i + 10, i + 20, i + 30)));
                    System.out.println("Completed real coordinator job " + i + "/5.");
                    if (i < 5) Thread.sleep(300);
                }

                int afterFive = waitForAgreement(bootstrap, 12000);
                int newTerm = maxTerm(bootstrap.getActiveWorkers());

                if (afterFive != -1 && newTerm > oldTerm)
                    pass("Five real jobs ended Term " + oldTerm
                            + " and a new election established Term " + newTerm + ".");
                else
                    fail("TEST 8", "Five jobs did not result in a genuine new coordinator term.");
            }

            // TEST 9
            heading(9, "COORDINATOR MESSAGE PROPAGATION");
            workers = bootstrap.getActiveWorkers();
            int agreedCoordinator = waitForAgreement(bootstrap, 8000);
            if (agreedCoordinator == -1) {
                fail("TEST 9", "Workers do not all report the same coordinator.");
            } else {
                boolean allAgree = true;
                int agreedTerm = -1;
                for (WorkerInfo info : workers) {
                    WorkerService w = getWorker(info);
                    int known = w.getCoordinatorId();
                    int term = w.getCoordinatorTerm();
                    if (agreedTerm == -1) agreedTerm = term;

                    System.out.println("Worker " + w.getId()
                            + " -> coordinator=" + known + ", term=" + term);

                    if (known != agreedCoordinator || term != agreedTerm)
                        allAgree = false;
                }

                if (allAgree)
                    pass("All " + workers.size() + " reachable workers received and agree on Coordinator Worker "
                            + agreedCoordinator + ".");
                else
                    fail("TEST 9", "Coordinator ID/term is not consistent across reachable workers.");
            }

        } catch (Exception e) {
            failed++;
            System.out.println("[FAIL] Unexpected tester error: " + e.getMessage());
            e.printStackTrace();
        }

        summary();
    }

    private static WorkerService getWorker(WorkerInfo info) throws Exception {
        return (WorkerService) Naming.lookup(info.getRmiUrl());
    }

    private static WorkerService findWorker(List<WorkerInfo> workers, int id) throws Exception {
        for (WorkerInfo info : workers)
            if (info.getId() == id) return getWorker(info);
        return null;
    }

    private static WorkerService requireCoordinator(BootstrapService bootstrap) throws Exception {
        int id = waitForAgreement(bootstrap, 5000);
        if (id == -1) throw new IllegalStateException("No agreed coordinator.");
        WorkerService worker = findWorker(bootstrap.getActiveWorkers(), id);
        if (worker == null) throw new IllegalStateException("Coordinator is not active.");
        return worker;
    }

    private static int waitForAgreement(BootstrapService bootstrap, long timeout) throws Exception {
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            List<WorkerInfo> workers = bootstrap.getActiveWorkers();
            if (!workers.isEmpty()) {
                Integer coordinator = null;
                Integer term = null;
                boolean agree = true;
                int selfCoordinatorCount = 0;

                for (WorkerInfo info : workers) {
                    try {
                        WorkerService w = getWorker(info);
                        int known = w.getCoordinatorId();
                        int knownTerm = w.getCoordinatorTerm();

                        if (known == -1) { agree = false; break; }
                        if (coordinator == null) {
                            coordinator = known;
                            term = knownTerm;
                        } else if (known != coordinator || knownTerm != term) {
                            agree = false;
                            break;
                        }

                        if (w.isCoordinator()) selfCoordinatorCount++;
                    } catch (Exception e) {
                        agree = false;
                        break;
                    }
                }

                if (agree && coordinator != null && selfCoordinatorCount == 1)
                    return coordinator;
            }
            Thread.sleep(200);
        }
        return -1;
    }

    private static int maxTerm(List<WorkerInfo> workers) throws Exception {
        int max = 0;
        for (WorkerInfo info : workers)
            max = Math.max(max, getWorker(info).getCoordinatorTerm());
        return max;
    }

    private static CandidateExpectation expectedWinner(List<WorkerInfo> workers) throws Exception {
        int minJac = Integer.MAX_VALUE;
        List<Integer> tied = new ArrayList<>();

        for (WorkerInfo info : workers) {
            WorkerService w = getWorker(info);
            int jac = w.getJac();

            if (jac < minJac) {
                minJac = jac;
                tied.clear();
                tied.add(w.getId());
            } else if (jac == minJac) {
                tied.add(w.getId());
            }
        }

        int winner = tied.stream().max(Integer::compareTo).orElse(-1);
        System.out.println("Real minimum JAC = " + minJac + ", tied workers = " + tied);
        System.out.println("Expected by rule = Worker " + winner);
        return new CandidateExpectation(winner, minJac, tied);
    }

    private static void endCurrentTermForTest(List<WorkerInfo> workers) throws Exception {
        int coordinatorId = -1, term = -1;
        for (WorkerInfo info : workers) {
            WorkerService w = getWorker(info);
            if (w.getCoordinatorId() != -1) {
                coordinatorId = w.getCoordinatorId();
                term = w.getCoordinatorTerm();
                break;
            }
        }
        if (coordinatorId == -1) return;

        com.example.cs324_a1.coordinator.TermEndMessage end =
                new com.example.cs324_a1.coordinator.TermEndMessage(coordinatorId, term);

        getWorker(workers.get(0)).receiveTermEnd(end, -1);
        Thread.sleep(500);
    }

    private static WorkerInfo findNonNeighbourReachableTarget(
            List<WorkerInfo> workers, int initiatorId) throws Exception {

        WorkerService initiator = null;
        for (WorkerInfo info : workers)
            if (info.getId() == initiatorId) initiator = getWorker(info);

        if (initiator == null) return null;

        List<Integer> direct = new ArrayList<>();
        for (WorkerInfo n : initiator.getNeighbours()) direct.add(n.getId());

        for (WorkerInfo info : workers)
            if (info.getId() != initiatorId && !direct.contains(info.getId()))
                return info;

        return null;
    }

    private static int chooseNeighbourSender(WorkerService target) throws Exception {
        List<WorkerInfo> neighbours = target.getNeighbours();
        return neighbours.isEmpty() ? -1 : neighbours.get(0).getId();
    }

    private static boolean waitUntilWorkerRemoved(
            BootstrapService bootstrap, int workerId, long timeout) throws Exception {

        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            boolean found = false;
            for (WorkerInfo info : bootstrap.getActiveWorkers())
                if (info.getId() == workerId) { found = true; break; }

            if (!found) return true;
            Thread.sleep(1000);
        }
        return false;
    }

    private static void ensureNoCoordinator(List<WorkerInfo> workers) throws Exception {
        boolean has = false;
        for (WorkerInfo info : workers)
            if (getWorker(info).getCoordinatorId() != -1) { has = true; break; }
        if (has) endCurrentTermForTest(workers);
    }

    private static void heading(int number, String name) {
        System.out.println();
        System.out.println("========== TEST " + number + ": " + name + " ==========");
    }

    private static void pass(String message) {
        passed++;
        System.out.println("[PASS] " + message);
    }

    private static void fail(String test, String message) {
        failed++;
        System.out.println("[FAIL] " + test + " - " + message);
    }

    private static void summary() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println(" PERSON 2 TEST SUMMARY");
        System.out.println(" Passed: " + passed);
        System.out.println(" Failed: " + failed);
        System.out.println("==================================================");
    }

    private static class CandidateExpectation {
        final int workerId, jac;
        final List<Integer> tiedIds;

        CandidateExpectation(int workerId, int jac, List<Integer> tiedIds) {
            this.workerId = workerId;
            this.jac = jac;
            this.tiedIds = new ArrayList<>(tiedIds);
        }
    }
}
