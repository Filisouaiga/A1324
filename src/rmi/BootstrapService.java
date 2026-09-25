/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package rmi;

import common.WorkerInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 *
 * @author jeral
 */
public interface BootstrapService extends Remote {

    boolean registerWorker(WorkerInfo info) throws RemoteException;

    void unregisterWorker(int workerId) throws RemoteException;

    List<WorkerInfo> getActiveWorkers() throws RemoteException;

    WorkerInfo getRandomWorker() throws RemoteException;

    void heartbeat(int workerId) throws RemoteException;

    int getActiveCount() throws RemoteException;
}
