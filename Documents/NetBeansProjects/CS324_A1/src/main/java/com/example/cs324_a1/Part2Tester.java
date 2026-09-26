/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.jobtype.JobType;
import com.example.cs324_a1.rmi.BootstrapService;
import com.example.cs324_a1.rmi.WorkerService;

import java.rmi.Naming;
import java.util.Arrays;
import java.util.List;

public class Part2Tester {

    private static final String BOOTSTRAP_URL =
            "rmi://localhost:1099/BootstrapService";

    private static int passed = 0;
    private static int failed = 0;


    public static void main(String[] args) {

        try {

            System.out.println(
                    "=========================================="
            );
            System.out.println(
                    " CS324 MERGED DISTRIBUTED SYSTEM TESTER"
            );
            System.out.println(
                    "=========================================="
            );


            // =================================================
            // TEST 1 - CONNECT TO BOOTSTRAP
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 1: BOOTSTRAP =========="
            );

            BootstrapService bootstrap =
                    (BootstrapService)
                            Naming.lookup(
                                    BOOTSTRAP_URL
                            );

            pass(
                    "Connected to Bootstrap."
            );


            // =================================================
            // TEST 2 - ACTIVE WORKERS
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 2: ACTIVE WORKERS =========="
            );

            List<WorkerInfo> workers =
                    bootstrap.getActiveWorkers();

            System.out.println(
                    "Active workers = "
                    + workers.size()
            );

            if (workers.isEmpty()) {

                fail(
                        "No workers are registered."
                );

                printSummary();
                return;
            }

            if (workers.size() >= 4) {

                pass(
                        "Four or more workers are registered."
                );

            } else {

                fail(
                        "Expected 4 workers, but found "
                        + workers.size()
                );
            }


            // =================================================
            // TEST 3 - RMI COMMUNICATION
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 3: RMI =========="
            );

            boolean allReachable = true;

            for (WorkerInfo info : workers) {

                try {

                    WorkerService worker =
                            getWorker(
                                    info
                            );

                    String reply =
                            worker.ping(
                                    "Tester"
                            );

                    System.out.println(
                            "Worker "
                            + worker.getId()
                            + " replied: "
                            + reply
                    );

                } catch (Exception e) {

                    allReachable = false;

                    System.out.println(
                            "Worker "
                            + info.getId()
                            + " could not be reached."
                    );
                }
            }

            if (allReachable) {

                pass(
                        "All workers are reachable through RMI."
                );

            } else {

                fail(
                        "One or more workers could not be reached."
                );
            }


            // =================================================
            // TEST 4 - NETWORK NEIGHBOURS
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 4: NETWORK =========="
            );

            for (WorkerInfo info : workers) {

                WorkerService worker =
                        getWorker(
                                info
                        );

                List<WorkerInfo> neighbours =
                        worker.getNeighbours();

                System.out.println(
                        "Worker "
                        + worker.getId()
                        + " has "
                        + neighbours.size()
                        + " neighbour(s)."
                );

                for (WorkerInfo neighbour : neighbours) {

                    System.out.println(
                            "   -> Worker "
                            + neighbour.getId()
                    );
                }
            }

            pass(
                    "Worker neighbour information retrieved."
            );


            // =================================================
            // TEST 5 - INITIAL WORKER STATE
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 5: WORKER STATE =========="
            );

            printWorkers(
                    workers
            );


            // =================================================
            // TEST 6 - ELECTION
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 6: ELECTION =========="
            );

            WorkerService coordinator =
                    findCoordinator(
                            workers
                    );

            if (coordinator == null) {

                System.out.println(
                        "No coordinator detected."
                );

                WorkerInfo initiatorInfo =
                        workers.get(0);

                WorkerService initiator =
                        getWorker(
                                initiatorInfo
                        );

                System.out.println(
                        "Worker "
                        + initiator.getId()
                        + " starts the election."
                );

                initiator.startElection();

                coordinator =
                        waitForCoordinator(
                                workers,
                                5000
                        );
            }


            if (coordinator == null) {

                fail(
                        "No coordinator was elected."
                );

                printSummary();
                return;

            } else {

                pass(
                        "Coordinator elected: Worker "
                        + coordinator.getId()
                );
            }


            // =================================================
            // TEST 7 - COORDINATOR AGREEMENT
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 7: COORDINATOR AGREEMENT =========="
            );

            int coordinatorCount =
                    countCoordinators(
                            workers
                    );

            System.out.println(
                    "Workers reporting themselves "
                    + "as coordinator = "
                    + coordinatorCount
            );

            if (coordinatorCount == 1) {

                pass(
                        "Exactly one coordinator is active."
                );

            } else {

                fail(
                        "Expected exactly one coordinator."
                );
            }


            int firstCoordinatorId =
                    coordinator.getId();

            int jacBeforeJobs =
                    coordinator.getJac();

            System.out.println(
                    "Coordinator = Worker "
                    + firstCoordinatorId
            );

            System.out.println(
                    "Coordinator JAC before jobs = "
                    + jacBeforeJobs
            );


            // =================================================
            // TEST 8 - MAX
            // JOB 1
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 8: MAX - JOB 1 =========="
            );

            JobRequest maxRequest =
                    new JobRequest(
                            JobType.MAX,
                            Arrays.asList(
                                    14,
                                    88,
                                    23,
                                    51,
                                    100,
                                    9,
                                    72,
                                    64
                            )
                    );

            JobResult maxResult =
                    coordinator.submitJob(
                            maxRequest
                    );

            System.out.println(
                    "MAX result = "
                    + maxResult.getResult()
            );

            if (maxResult.getResult() == 100) {

                pass(
                        "MAX result is correct."
                );

            } else {

                fail(
                        "MAX expected 100."
                );
            }


            // =================================================
            // TEST 9 - PRIMESUM
            // JOB 2
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 9: PRIMESUM - JOB 2 =========="
            );

            coordinator =
                    requireCoordinator(
                            workers
                    );

            JobRequest primeSumRequest =
                    new JobRequest(
                            JobType.PRIMESUM,
                            1,
                            10
                    );

            JobResult primeSumResult =
                    coordinator.submitJob(
                            primeSumRequest
                    );

            System.out.println(
                    "PRIMESUM result = "
                    + primeSumResult.getResult()
            );

            // 2 + 3 + 5 + 7 = 17
            if (primeSumResult.getResult() == 17) {

                pass(
                        "PRIMESUM result is correct."
                );

            } else {

                fail(
                        "PRIMESUM expected 17."
                );
            }


            // =================================================
            // TEST 10 - PRIMECOUNT
            // JOB 3
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 10: PRIMECOUNT - JOB 3 =========="
            );

            coordinator =
                    requireCoordinator(
                            workers
                    );

            JobRequest primeCountRequest =
                    new JobRequest(
                            JobType.PRIMECOUNT,
                            Arrays.asList(
                                    2,
                                    3,
                                    4,
                                    5,
                                    6,
                                    7,
                                    8,
                                    9,
                                    10,
                                    11
                            )
                    );

            JobResult primeCountResult =
                    coordinator.submitJob(
                            primeCountRequest
                    );

            System.out.println(
                    "PRIMECOUNT result = "
                    + primeCountResult.getResult()
            );

            // Prime numbers: 2, 3, 5, 7, 11
            if (primeCountResult.getResult() == 5) {

                pass(
                        "PRIMECOUNT result is correct."
                );

            } else {

                fail(
                        "PRIMECOUNT expected 5."
                );
            }


            // =================================================
            // TEST 11 - JAC
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 11: JAC =========="
            );

            WorkerService firstCoordinator =
                    findWorkerById(
                            workers,
                            firstCoordinatorId
                    );

            int jacAfterThreeJobs =
                    firstCoordinator.getJac();

            System.out.println(
                    "Worker "
                    + firstCoordinatorId
                    + " JAC before jobs = "
                    + jacBeforeJobs
            );

            System.out.println(
                    "Worker "
                    + firstCoordinatorId
                    + " JAC after 3 jobs = "
                    + jacAfterThreeJobs
            );

            if (jacAfterThreeJobs > jacBeforeJobs) {

                pass(
                        "Coordinator JAC increased "
                        + "after assigning work."
                );

            } else {

                fail(
                        "Coordinator JAC did not increase."
                );
            }


            // =================================================
            // TEST 12 - JOB 4
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 12: JOB 4 =========="
            );

            coordinator =
                    requireCoordinator(
                            workers
                    );

            JobRequest job4 =
                    new JobRequest(
                            JobType.MAX,
                            Arrays.asList(
                                    5,
                                    10,
                                    15,
                                    20,
                                    25
                            )
                    );

            JobResult job4Result =
                    coordinator.submitJob(
                            job4
                    );

            System.out.println(
                    "Job 4 result = "
                    + job4Result.getResult()
            );

            if (job4Result.getResult() == 25) {

                pass(
                        "Job 4 completed correctly."
                );

            } else {

                fail(
                        "Job 4 expected 25."
                );
            }


            // =================================================
            // TEST 13 - JOB 5
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 13: JOB 5 =========="
            );

            coordinator =
                    requireCoordinator(
                            workers
                    );

            JobRequest job5 =
                    new JobRequest(
                            JobType.PRIMESUM,
                            1,
                            20
                    );

            JobResult job5Result =
                    coordinator.submitJob(
                            job5
                    );

            System.out.println(
                    "Job 5 result = "
                    + job5Result.getResult()
            );

            // 2 + 3 + 5 + 7 + 11 + 13 + 17 + 19 = 77
            if (job5Result.getResult() == 77) {

                pass(
                        "Job 5 completed correctly."
                );

            } else {

                fail(
                        "Job 5 expected 77."
                );
            }


            // =================================================
            // TEST 14 - SECOND ELECTION
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 14: SECOND ELECTION =========="
            );

            /*
             * Job 5 should end the coordinator term.
             * WorkerNode should then start another election.
             */

            WorkerService secondCoordinator =
                    waitForCoordinator(
                            workers,
                            5000
                    );

            if (secondCoordinator == null) {

                fail(
                        "No coordinator after the five-job term."
                );

            } else {

                pass(
                        "Coordinator exists after re-election."
                );

                System.out.println(
                        "New coordinator = Worker "
                        + secondCoordinator.getId()
                );
            }


            // =================================================
            // TEST 15 - COORDINATOR AFTER TERM
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 15: NEW COORDINATOR =========="
            );

            if (secondCoordinator != null) {

                int secondCoordinatorId =
                        secondCoordinator.getId();

                System.out.println(
                        "Previous coordinator = Worker "
                        + firstCoordinatorId
                );

                System.out.println(
                        "Current coordinator = Worker "
                        + secondCoordinatorId
                );

                /*
                 * Normally the previous coordinator's JAC
                 * increased because it assigned work to
                 * other workers.
                 *
                 * The next election therefore considers
                 * the updated JAC values.
                 */
                if (secondCoordinatorId
                        != firstCoordinatorId) {

                    pass(
                            "Election used updated JAC values "
                            + "and selected a new coordinator."
                    );

                } else {

                    System.out.println(
                            "[INFO] Same worker was elected again."
                    );

                    System.out.println(
                            "[INFO] Check the workers' JAC values "
                            + "before deciding whether this is wrong."
                    );
                }
            }


            // =================================================
            // TEST 16 - FINAL COORDINATOR AGREEMENT
            // =================================================

            System.out.println();
            System.out.println(
                    "========== TEST 16: FINAL AGREEMENT =========="
            );

            int finalCoordinatorCount =
                    countCoordinators(
                            workers
                    );

            if (finalCoordinatorCount == 1) {

                pass(
                        "All workers reached a state with "
                        + "one active coordinator."
                );

            } else {

                fail(
                        "Expected one coordinator, found "
                        + finalCoordinatorCount
                );
            }


            // =================================================
            // FINAL WORKER STATE
            // =================================================

            System.out.println();
            System.out.println(
                    "========== FINAL WORKER STATE =========="
            );

            workers =
                    bootstrap.getActiveWorkers();

            printWorkers(
                    workers
            );


            // =================================================
            // SUMMARY
            // =================================================

            printSummary();


        } catch (Exception e) {

            System.err.println();
            System.err.println(
                    "TEST ERROR: "
                    + e.getMessage()
            );

            e.printStackTrace();

            failed++;

            printSummary();
        }
    }


    // =========================================================
    // GET REMOTE WORKER
    // =========================================================

    private static WorkerService getWorker(
            WorkerInfo info)
            throws Exception {

        return (WorkerService)
                Naming.lookup(
                        info.getRmiUrl()
                );
    }


    // =========================================================
    // FIND COORDINATOR
    // =========================================================

    private static WorkerService findCoordinator(
            List<WorkerInfo> workers)
            throws Exception {

        for (WorkerInfo info : workers) {

            WorkerService worker =
                    getWorker(
                            info
                    );

            if (worker.isCoordinator()) {

                return worker;
            }
        }

        return null;
    }


    // =========================================================
    // WAIT FOR COORDINATOR
    // =========================================================

    private static WorkerService waitForCoordinator(
            List<WorkerInfo> workers,
            long timeout)
            throws Exception {

        long start =
                System.currentTimeMillis();

        while (System.currentTimeMillis()
                - start < timeout) {

            WorkerService coordinator =
                    findCoordinator(
                            workers
                    );

            if (coordinator != null) {

                return coordinator;
            }

            Thread.sleep(
                    200
            );
        }

        return null;
    }


    // =========================================================
    // REQUIRE COORDINATOR
    // =========================================================

    private static WorkerService requireCoordinator(
            List<WorkerInfo> workers)
            throws Exception {

        WorkerService coordinator =
                findCoordinator(
                        workers
                );

        if (coordinator == null) {

            throw new IllegalStateException(
                    "No active coordinator."
            );
        }

        return coordinator;
    }


    // =========================================================
    // COUNT COORDINATORS
    // =========================================================

    private static int countCoordinators(
            List<WorkerInfo> workers)
            throws Exception {

        int count = 0;

        for (WorkerInfo info : workers) {

            WorkerService worker =
                    getWorker(
                            info
                    );

            if (worker.isCoordinator()) {

                count++;
            }
        }

        return count;
    }


    // =========================================================
    // FIND WORKER BY ID
    // =========================================================

    private static WorkerService findWorkerById(
            List<WorkerInfo> workers,
            int workerId)
            throws Exception {

        for (WorkerInfo info : workers) {

            if (info.getId() == workerId) {

                return getWorker(
                        info
                );
            }
        }

        throw new IllegalStateException(
                "Worker "
                + workerId
                + " was not found."
        );
    }


    // =========================================================
    // DISPLAY WORKERS
    // =========================================================

    private static void printWorkers(
            List<WorkerInfo> workers)
            throws Exception {

        for (WorkerInfo info : workers) {

            WorkerService worker =
                    getWorker(
                            info
                    );

            System.out.println(
                    "Worker "
                    + worker.getId()
                    + " | JAC = "
                    + worker.getJac()
                    + " | Coordinator = "
                    + worker.isCoordinator()
                    + " | Neighbours = "
                    + worker.getNeighbours().size()
            );
        }
    }


    // =========================================================
    // PASS
    // =========================================================

    private static void pass(
            String message) {

        passed++;

        System.out.println(
                "[PASS] "
                + message
        );
    }


    // =========================================================
    // FAIL
    // =========================================================

    private static void fail(
            String message) {

        failed++;

        System.out.println(
                "[FAIL] "
                + message
        );
    }


    // =========================================================
    // FINAL SUMMARY
    // =========================================================

    private static void printSummary() {

        System.out.println();
        System.out.println(
                "=========================================="
        );

        System.out.println(
                " TEST SUMMARY"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Passed: "
                + passed
        );

        System.out.println(
                "Failed: "
                + failed
        );

        System.out.println(
                "=========================================="
        );
    }
}