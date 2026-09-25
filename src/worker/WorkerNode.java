/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package worker;

import common.WorkerInfo;
import rmi.BootstrapService;
import rmi.WorkerService;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author jeral
 */
public class WorkerNode extends UnicastRemoteObject implements WorkerService {

    private static final long serialVersionUID = 1L;

    // Required by the assignment
    private final String leaderman = "cs324";

    private final int id;
    private final String host;
    private final int port;
    private final String rmiName;

    private final List<WorkerInfo> neighbours =
            Collections.synchronizedList(new ArrayList<>());

    private BootstrapService bootstrap;
    private final String bootstrapHost;
    private final int bootstrapPort;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public WorkerNode(int id, String host, int port,
                      String bootstrapHost, int bootstrapPort) throws RemoteException {
        super();
        this.id = id;
        this.host = host;
        this.port = port;
        this.rmiName = "WorkerService-" + id;
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
    }
    
    public void start() throws Exception {
        // 1. Create local RMI registry and bind this worker
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(port);
            System.out.println("[Worker " + id + "] Created local RMI registry on port " + port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(port);
            System.out.println("[Worker " + id + "] Using existing registry on port " + port);
        }
        registry.rebind(rmiName, this);
        System.out.println("[Worker " + id + "] Bound as " + rmiName);

        // 2. Look up the Bootstrap
        String bootstrapUrl = "rmi://" + bootstrapHost + ":" + bootstrapPort + "/BootstrapService";
        bootstrap = (BootstrapService) Naming.lookup(bootstrapUrl);
        System.out.println("[Worker " + id + "] Connected to Bootstrap");

        // 3. Register ourselves
        WorkerInfo myInfo = new WorkerInfo(id, host, port, rmiName);
        bootstrap.registerWorker(myInfo);

        // 4. Connect to up to 3 existing workers (unstructured network)
        List<WorkerInfo> all = bootstrap.getActiveWorkers();
        Collections.shuffle(all);
        int linked = 0;
        for (WorkerInfo peerInfo : all) {
            if (peerInfo.getId() == id) continue;
            if (linked >= 3) break;
            try {
                addNeighbour(peerInfo);
                WorkerService peer = (WorkerService) Naming.lookup(peerInfo.getRmiUrl());
                peer.addNeighbour(myInfo);   // make the link bidirectional
                System.out.println("[Worker " + id + "] Linked with peer " + peerInfo.getId());
                linked++;
            } catch (Exception ex) {
                System.err.println("[Worker " + id + "] Could not link with "
                        + peerInfo.getId() + ": " + ex.getMessage());
            }
        }
        if (linked == 0) {
            System.out.println("[Worker " + id + "] No other peers yet (first worker)");
        }
        
        // 5. Start heartbeat
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 5, 10, TimeUnit.SECONDS);

        System.out.println("[Worker " + id + "] Ready | leaderman=" + leaderman
                + " | neighbours=" + neighbours.size());
    }
    
    private void sendHeartbeat() {
        try {
            if (bootstrap != null) {
                bootstrap.heartbeat(id);
            }
        } catch (Exception e) {
            System.err.println("[Worker " + id + "] Heartbeat failed: " + e.getMessage());
        }
    }

    // ----- WorkerService methods -----

    @Override
    public int getId() throws RemoteException {
        return id;
    }

    @Override
    public WorkerInfo getInfo() throws RemoteException {
        return new WorkerInfo(id, host, port, rmiName);
    }

    @Override
    public String getStatus() throws RemoteException {
        return "Worker " + id + " | neighbours=" + neighbours.size()
                + " | leaderman=" + leaderman;
    }

    @Override
    public void addNeighbour(WorkerInfo neighbour) throws RemoteException {
        if (neighbour == null || neighbour.getId() == id) return;
        synchronized (neighbours) {
            if (!neighbours.contains(neighbour)) {
                neighbours.add(neighbour);
                System.out.println("[Worker " + id + "] Added neighbour " + neighbour.getId());
            }
        }
    }

    @Override
    public List<WorkerInfo> getNeighbours() throws RemoteException {
        synchronized (neighbours) {
            return new ArrayList<>(neighbours);
        }
    }

    @Override
    public String ping(String message) throws RemoteException {
        System.out.println("[Worker " + id + "] Received ping: " + message);
        return "Pong from Worker " + id + " (got: " + message + ")";
    }
    
    public void shutdown() {
        try {
            if (bootstrap != null) {
                bootstrap.unregisterWorker(id);
            }
        } catch (Exception ignored) {
        }
        scheduler.shutdownNow();
        System.out.println("[Worker " + id + "] Shutdown complete");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: WorkerNode <id> [host] [port] [bootstrapHost] [bootstrapPort]");
            System.exit(1);
        }

        int workerId = Integer.parseInt(args[0]);
        String host = args.length > 1 ? args[1] : "localhost";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 1100 + workerId;
        String bootHost = args.length > 3 ? args[3] : "localhost";
        int bootPort = args.length > 4 ? Integer.parseInt(args[4]) : 1099;

        try {
            WorkerNode node = new WorkerNode(workerId, host, port, bootHost, bootPort);
            node.start();
            Runtime.getRuntime().addShutdownHook(new Thread(node::shutdown));
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("[Worker] Failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}