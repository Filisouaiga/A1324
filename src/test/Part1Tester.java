/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package test;

import common.WorkerInfo;
import rmi.BootstrapService;
import rmi.WorkerService;
import java.rmi.Naming;
import java.util.List;

/**
 *
 * @author jeral
 */
public class Part1Tester {

    public static void main(String[] args) throws Exception {
        String bootHost = "localhost";
        int bootPort = 1099;

        System.out.println("=== A1_UDS Part 1 Tester ===");
        System.out.println("Looking up Bootstrap...");

        BootstrapService bootstrap = (BootstrapService) Naming.lookup(
                "rmi://" + bootHost + ":" + bootPort + "/BootstrapService");

        int count = bootstrap.getActiveCount();
        System.out.println("Active workers reported by Bootstrap: " + count);

        List<WorkerInfo> workers = bootstrap.getActiveWorkers();
        if (workers.isEmpty()) {
            System.out.println("No workers registered. Start some workers first.");
            return;
        }

        System.out.println("\n--- Registered workers ---");
        for (WorkerInfo w : workers) {
            System.out.println("  " + w);
        }

        System.out.println("\n--- Checking each worker via RMI ---");
        for (WorkerInfo w : workers) {
            try {
                WorkerService ws = (WorkerService) Naming.lookup(w.getRmiUrl());
                System.out.println("Worker " + w.getId() + " status : " + ws.getStatus());

                List<WorkerInfo> neighbours = ws.getNeighbours();
                System.out.println("  Neighbours (" + neighbours.size() + "):");
                for (WorkerInfo n : neighbours) {
                    System.out.println("    -> Worker " + n.getId());
                }
            } catch (Exception e) {
                System.out.println("  ERROR contacting Worker " + w.getId() + ": " + e.getMessage());
            }
        }

        // Test inter-worker communication
        if (workers.size() >= 2) {
            System.out.println("\n--- Testing inter-worker communication (ping) ---");
            WorkerInfo b = workers.get(1);
            try {
                WorkerService wsB = (WorkerService) Naming.lookup(b.getRmiUrl());
                String reply = wsB.ping("Hello from Part1Tester");
                System.out.println("Ping reply: " + reply);
            } catch (Exception e) {
                System.out.println("Ping failed: " + e.getMessage());
            }
        }

        System.out.println("\n=== Test finished ===");
    }
}