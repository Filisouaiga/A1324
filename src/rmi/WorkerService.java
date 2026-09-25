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
 * Remote Interface for a Worker
 * @author jeral
 */
public interface WorkerService extends Remote {

    int getId() throws RemoteException;

    WorkerInfo getInfo() throws RemoteException;

    String getStatus() throws RemoteException;

    void addNeighbour(WorkerInfo neighbour) throws RemoteException;

    List<WorkerInfo> getNeighbours() throws RemoteException;

    /** Simple method so we can test RMI communication between workers */
    String ping(String message) throws RemoteException;
}
