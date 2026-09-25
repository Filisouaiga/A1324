/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.worker;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.election.CoordinatorMessage;
import com.example.cs324_a1.election.ElectionMessage;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRemote extends Remote {

    int getWorkerId() throws RemoteException;

    int getJac() throws RemoteException;

    void receiveElection(ElectionMessage message)
            throws RemoteException;

    void receiveCoordinator(CoordinatorMessage message)
            throws RemoteException;
}
