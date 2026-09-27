/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.cs324_a1.rmi;

import com.example.cs324_a1.common.WorkerInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BootstrapService extends Remote {

    boolean registerWorker(WorkerInfo info) throws RemoteException;

    void unregisterWorker(int workerId) throws RemoteException;

    List<WorkerInfo> getActiveWorkers() throws RemoteException;

    WorkerInfo getRandomWorker() throws RemoteException;

    void heartbeat(int workerId) throws RemoteException;

    int getActiveCount() throws RemoteException;
}