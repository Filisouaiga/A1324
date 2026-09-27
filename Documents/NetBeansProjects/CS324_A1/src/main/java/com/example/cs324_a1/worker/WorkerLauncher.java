/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.worker;

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.rmi.BootstrapService;

import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public class WorkerLauncher {

    private static final String BOOTSTRAP_HOST = "localhost";
    private static final int BOOTSTRAP_PORT = 1099;

    private static final String BOOTSTRAP_URL =
            "rmi://" + BOOTSTRAP_HOST + ":" + BOOTSTRAP_PORT
                    + "/BootstrapService";

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("================================");
        System.out.println("       CS324 WORKER LAUNCHER");
        System.out.println("================================");

        while (true) {

            System.out.println();
            System.out.println("1. Add New Worker");
            System.out.println("2. Show Active Workers");
            System.out.println("3. Exit");
            System.out.print("Select option: ");

            String option = scanner.nextLine().trim();

            switch (option) {

                case "1":
                    startNewWorker();
                    break;

                case "2":
                    showActiveWorkers();
                    break;

                case "3":
                    System.out.println("Worker Launcher closed.");
                    scanner.close();
                    return;

                default:
                    System.out.println(
                            "Invalid option. Please select 1, 2 or 3."
                    );
            }
        }
    }

    // Start a new worker with an automatic ID
    private static void startNewWorker() {

        try {

            // Connect to Bootstrap
            BootstrapService bootstrap =
                    (BootstrapService) Naming.lookup(BOOTSTRAP_URL);

            // Get workers currently in the system
            List<WorkerInfo> workers =
                    bootstrap.getActiveWorkers();

            // Find the next unique worker ID
            int workerId = getNextWorkerId(workers);

            // Give the worker its RMI port
            int workerPort = 1100 + workerId;

            System.out.println();
            System.out.println("Starting new worker...");
            System.out.println("Assigned Worker ID: " + workerId);
            System.out.println("Assigned RMI Port: " + workerPort);

            // Find the Java executable
            String javaHome = System.getProperty("java.home");

            String javaExecutable =
                    javaHome
                    + File.separator
                    + "bin"
                    + File.separator
                    + "java";

            if (System.getProperty("os.name")
                    .toLowerCase()
                    .contains("win")) {

                javaExecutable += ".exe";
            }

            // Get the current project classpath
            String classPath =
                    System.getProperty("java.class.path");

            // Start WorkerNode as a separate Java process
            ProcessBuilder processBuilder =
                    new ProcessBuilder(
                            javaExecutable,
                            "-cp",
                            classPath,
                            WorkerNode.class.getName(),
                            String.valueOf(workerId),
                            "localhost",
                            String.valueOf(workerPort),
                            BOOTSTRAP_HOST,
                            String.valueOf(BOOTSTRAP_PORT)
                    );

            // Show worker output in NetBeans
            processBuilder.inheritIO();

            Process process =
                    processBuilder.start();

            System.out.println(
                    "Worker " + workerId
                    + " process started successfully."
            );

            System.out.println(
                    "Process ID: " + process.pid()
            );

        } catch (Exception e) {

            System.err.println(
                    "Could not start Worker: "
                    + e.getMessage()
            );
        }
    }

    // Find the next available worker ID
    private static int getNextWorkerId(
            List<WorkerInfo> workers) {

        int workerId = 1;

        while (true) {

            boolean used = false;

            for (WorkerInfo worker : workers) {

                if (worker.getId() == workerId) {
                    used = true;
                    break;
                }
            }

            if (!used) {
                return workerId;
            }

            workerId++;
        }
    }

    // Display all active workers
    private static void showActiveWorkers() {

        try {

            // Connect to Bootstrap
            BootstrapService bootstrap =
                    (BootstrapService) Naming.lookup(BOOTSTRAP_URL);

            // Get active workers
            List<WorkerInfo> workers =
                    bootstrap.getActiveWorkers();

            workers.sort(
                    Comparator.comparingInt(
                            WorkerInfo::getId
                    )
            );

            System.out.println();
            System.out.println("================================");
            System.out.println("         ACTIVE WORKERS");
            System.out.println("================================");

            if (workers.isEmpty()) {

                System.out.println("No active workers.");

            } else {

                for (WorkerInfo worker : workers) {

                    System.out.println(
                            "Worker " + worker.getId()
                            + " | "
                            + worker.getHost()
                            + ":"
                            + worker.getPort()
                    );
                }
            }

            System.out.println();
            System.out.println(
                    "Total Active Workers: "
                    + workers.size()
            );

        } catch (Exception e) {

            System.err.println(
                    "Could not get active workers: "
                    + e.getMessage()
            );
        }
    }
}