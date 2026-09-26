/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.cs324_a1.rmi;

import com.example.cs324_a1.common.WorkerInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for the Bootstrap Node.
 *
 * The Bootstrap Node keeps track of active workers
 * in the distributed system.
 *
 * Originally implemented by Person 1 and integrated
 * into the combined Person 1 + Person 2 system.
 */
public interface BootstrapService extends Remote {

    /**
     * Register a worker with the Bootstrap Node.
     */
    boolean registerWorker(WorkerInfo info)
            throws RemoteException;

    /**
     * Remove a worker from the Bootstrap Node.
     */
    void unregisterWorker(int workerId)
            throws RemoteException;

    /**
     * Return all currently active workers.
     */
    List<WorkerInfo> getActiveWorkers()
            throws RemoteException;

    /**
     * Return a randomly selected active worker.
     */
    WorkerInfo getRandomWorker()
            throws RemoteException;

    /**
     * Receive a heartbeat from a worker.
     */
    void heartbeat(int workerId)
            throws RemoteException;

    /**
     * Return the number of active workers.
     */
    int getActiveCount()
            throws RemoteException;
}