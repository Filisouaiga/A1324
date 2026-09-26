/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boostrap;

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

public class BootstrapNode
        extends UnicastRemoteObject
        implements BootstrapService {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 1099;

    public static final String SERVICE_NAME =
            "BootstrapService";

    private final Map<Integer, WorkerInfo> activeWorkers =
            new ConcurrentHashMap<>();

    private final Map<Integer, Long> lastHeartbeat =
            new ConcurrentHashMap<>();

    private final Random random =
            new Random();

    private static final long HEARTBEAT_TIMEOUT_MS =
            30000;

    public BootstrapNode() throws RemoteException {

        super();

        // Background thread removes workers
        // that stop sending heartbeats.
        Thread cleaner =
                new Thread(
                        this::cleanupDeadWorkers,
                        "Bootstrap-Cleaner"
                );

        cleaner.setDaemon(true);
        cleaner.start();
    }

    // REGISTER WORKER
    @Override
    public boolean registerWorker(WorkerInfo info)
            throws RemoteException {

        if (info == null) {
            return false;
        }

        activeWorkers.put(
                info.getId(),
                info
        );

        lastHeartbeat.put(
                info.getId(),
                System.currentTimeMillis()
        );

        System.out.println(
                "[Bootstrap] Registered: " + info
        );

        return true;
    }

    // UNREGISTER WORKER

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

    // ACTIVE WORKERS

    @Override
    public List<WorkerInfo> getActiveWorkers()
            throws RemoteException {

        return new ArrayList<>(
                activeWorkers.values()
        );
    }

    // RANDOM WORKER

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

    // HEARTBEAT

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

    // ACTIVE WORKER COUNT

    @Override
    public int getActiveCount()
            throws RemoteException {

        return activeWorkers.size();
    }

    // REMOVE DEAD WORKERS

    private void cleanupDeadWorkers() {

        while (true) {

            try {

                // Check workers every 10 seconds
                Thread.sleep(10000);

                long now =
                        System.currentTimeMillis();

                List<Integer> toRemove =
                        new ArrayList<>();

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

                for (int workerId : toRemove) {

                    activeWorkers.remove(workerId);

                    lastHeartbeat.remove(workerId);

                    System.out.println(
                            "[Bootstrap] Timed-out worker "
                            + workerId
                    );
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // MAIN

    public static void main(String[] args) {

        int port =
                DEFAULT_PORT;

        // Allow optional custom bootstrap port
        if (args.length > 0) {

            try {

                port =
                        Integer.parseInt(
                                args[0]
                        );

            } catch (NumberFormatException ignored) {

                System.out.println(
                        "[Bootstrap] Invalid port. "
                        + "Using default port "
                        + DEFAULT_PORT
                );

                port =
                        DEFAULT_PORT;
            }
        }

        try {

            Registry registry;

            try {

                // Try creating the RMI registry
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

                // Registry may already exist
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

            // Create Bootstrap Node
            BootstrapNode node =
                    new BootstrapNode();

            // Bind it to the registry
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

            // Keep process running
            Thread.currentThread().join();

        } catch (Exception e) {

            System.err.println(
                    "[Bootstrap] Failed to start: "
                    + e.getMessage()
            );

            e.printStackTrace();

            System.exit(1);
        }
    }
}
