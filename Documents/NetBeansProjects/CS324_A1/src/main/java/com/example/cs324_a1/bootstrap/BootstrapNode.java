/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.bootstrap;

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.rmi.BootstrapService;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class BootstrapNode extends UnicastRemoteObject implements BootstrapService {

    private static final long serialVersionUID = 1L;

    // Default RMI settings
    public static final int DEFAULT_PORT = 1099;
    public static final String SERVICE_NAME = "BootstrapService";

    // Worker heartbeat timeout
    private static final long HEARTBEAT_TIMEOUT_MS = 30000;

    // Stores active workers
    private final Map<Integer, WorkerInfo> activeWorkers =
            new ConcurrentHashMap<>();

    // Stores each worker's last heartbeat
    private final Map<Integer, Long> lastHeartbeat =
            new ConcurrentHashMap<>();

    // Used to select a random worker
    private final Random random = new Random();

    // Creates the Bootstrap Node
    public BootstrapNode() throws RemoteException {
        super();

        // Start background worker cleanup
        Thread cleaner = new Thread(
                this::cleanupDeadWorkers,
                "Bootstrap-Cleaner"
        );

        cleaner.setDaemon(true);
        cleaner.start();
    }

    // Register a new worker
    @Override
    public boolean registerWorker(WorkerInfo info)
            throws RemoteException {

        if (info == null) {
            return false;
        }

        activeWorkers.put(info.getId(), info);

        lastHeartbeat.put(
                info.getId(),
                System.currentTimeMillis()
        );

        System.out.println(
                "[Bootstrap] Registered: " + info
        );

        return true;
    }

    // Remove a worker
    @Override
    public void unregisterWorker(int workerId)
            throws RemoteException {

        activeWorkers.remove(workerId);
        lastHeartbeat.remove(workerId);

        System.out.println(
                "[Bootstrap] Unregistered worker "
                + workerId
        );
    }

    // Return all active workers
    @Override
    public List<WorkerInfo> getActiveWorkers()
            throws RemoteException {

        return new ArrayList<>(
                activeWorkers.values()
        );
    }

    // Return a random active worker
    @Override
    public WorkerInfo getRandomWorker()
            throws RemoteException {

        List<WorkerInfo> workers =
                new ArrayList<>(
                        activeWorkers.values()
                );

        if (workers.isEmpty()) {
            return null;
        }

        return workers.get(
                random.nextInt(workers.size())
        );
    }

    // Update a worker's heartbeat
    @Override
    public void heartbeat(int workerId)
            throws RemoteException {

        if (activeWorkers.containsKey(workerId)) {

            lastHeartbeat.put(
                    workerId,
                    System.currentTimeMillis()
            );
        }
    }

    // Return the number of active workers
    @Override
    public int getActiveCount()
            throws RemoteException {

        return activeWorkers.size();
    }

    // Remove workers that stop sending heartbeats
    private void cleanupDeadWorkers() {

        while (true) {

            try {

                // Check workers every 10 seconds
                Thread.sleep(10000);

                long now =
                        System.currentTimeMillis();

                List<Integer> toRemove =
                        new ArrayList<>();

                // Find timed-out workers
                for (Map.Entry<Integer, Long> entry
                        : lastHeartbeat.entrySet()) {

                    long lastSeen =
                            entry.getValue();

                    if (now - lastSeen
                            > HEARTBEAT_TIMEOUT_MS) {

                        toRemove.add(
                                entry.getKey()
                        );
                    }
                }

                // Remove timed-out workers
                for (int workerId : toRemove) {

                    activeWorkers.remove(workerId);
                    lastHeartbeat.remove(workerId);

                    System.out.println(
                            "[Bootstrap] Timed-out worker "
                            + workerId
                    );
                }

            } catch (InterruptedException e) {

                // Stop the cleanup thread
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Start the Bootstrap RMI service
    public static void main(String[] args) {

        int port = DEFAULT_PORT;

        try {

            Registry registry;

            try {

                // Create the RMI registry
                registry =
                        LocateRegistry.createRegistry(
                                port
                        );

                System.out.println(
                        "[Bootstrap] Created RMI registry "
                        + "on port "
                        + port
                );

            } catch (RemoteException e) {

                // Use the existing RMI registry
                registry =
                        LocateRegistry.getRegistry(
                                port
                        );

                System.out.println(
                        "[Bootstrap] Using existing registry "
                        + "on port "
                        + port
                );
            }

            // Create the Bootstrap Node
            BootstrapNode node =
                    new BootstrapNode();

            // Register Bootstrap with RMI
            registry.rebind(
                    SERVICE_NAME,
                    node
            );

            System.out.println(
                    "[Bootstrap] Service bound as '"
                    + SERVICE_NAME
                    + "'"
            );

            System.out.println(
                    "[Bootstrap] Ready - waiting for workers..."
            );

            // Keep Bootstrap running
            Thread.currentThread().join();

        } catch (Exception e) {

            // Handle startup errors
            System.err.println(
                    "[Bootstrap] Failed to start: "
                    + e.getMessage()
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}