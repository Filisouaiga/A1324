/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bootstrap;

import common.WorkerInfo;
import rmi.BootstrapService;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author jeral
 */
public class BootstrapNode extends UnicastRemoteObject implements BootstrapService {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PORT = 1099;
    public static final String SERVICE_NAME = "BootstrapService";

    private final Map<Integer, WorkerInfo> activeWorkers = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastHeartbeat = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private static final long HEARTBEAT_TIMEOUT_MS = 30000;

    public BootstrapNode() throws RemoteException {
        super();
        Thread cleaner = new Thread(this::cleanupDeadWorkers, "Bootstrap-Cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    @Override
    public boolean registerWorker(WorkerInfo info) throws RemoteException {
        if (info == null) {
            return false;
        }
        activeWorkers.put(info.getId(), info);
        lastHeartbeat.put(info.getId(), System.currentTimeMillis());
        System.out.println("[Bootstrap] Registered: " + info);
        return true;
    }

    @Override
    public void unregisterWorker(int workerId) throws RemoteException {
        activeWorkers.remove(workerId);
        lastHeartbeat.remove(workerId);
        System.out.println("[Bootstrap] Unregistered worker " + workerId);
    }
    
    @Override
    public List<WorkerInfo> getActiveWorkers() throws RemoteException {
        return new ArrayList<>(activeWorkers.values());
    }

    @Override
    public WorkerInfo getRandomWorker() throws RemoteException {
        List<WorkerInfo> list = new ArrayList<>(activeWorkers.values());
        if (list.isEmpty()) {
            return null;
        }
        return list.get(random.nextInt(list.size()));
    }

    @Override
    public void heartbeat(int workerId) throws RemoteException {
        if (activeWorkers.containsKey(workerId)) {
            lastHeartbeat.put(workerId, System.currentTimeMillis());
        }
    }

    @Override
    public int getActiveCount() throws RemoteException {
        return activeWorkers.size();
    }

    private void cleanupDeadWorkers() {
        while (true) {
            try {
                Thread.sleep(10000);
                long now = System.currentTimeMillis();
                List<Integer> toRemove = new ArrayList<>();
                for (Map.Entry<Integer, Long> entry : lastHeartbeat.entrySet()) {
                    if (now - entry.getValue() > HEARTBEAT_TIMEOUT_MS) {
                        toRemove.add(entry.getKey());
                    }
                }
                for (int id : toRemove) {
                    activeWorkers.remove(id);
                    lastHeartbeat.remove(id);
                    System.out.println("[Bootstrap] Timed-out worker " + id);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(port);
                System.out.println("[Bootstrap] Created RMI registry on port " + port);
            } catch (RemoteException e) {
                registry = LocateRegistry.getRegistry(port);
                System.out.println("[Bootstrap] Using existing registry on port " + port);
            }

            BootstrapNode node = new BootstrapNode();
            registry.rebind(SERVICE_NAME, node);

            System.out.println("[Bootstrap] Service bound as '" + SERVICE_NAME + "'");
            System.out.println("[Bootstrap] Ready - waiting for workers...");

            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[Bootstrap] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}