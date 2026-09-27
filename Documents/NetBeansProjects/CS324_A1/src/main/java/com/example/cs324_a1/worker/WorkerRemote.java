/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.worker;

import com.example.cs324_a1.coordinator.TermEndMessage;
import com.example.cs324_a1.election.CoordinatorMessage;
import com.example.cs324_a1.election.ElectionMessage;
import com.example.cs324_a1.election.ElectionReply;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface WorkerRemote extends Remote {

    int getWorkerId()
            throws RemoteException;

    int getJac()
            throws RemoteException;

    void receiveElection(
            ElectionMessage message,
            int senderId)
            throws RemoteException;

    void receiveElectionReply(
            ElectionReply reply)
            throws RemoteException;

    void receiveCoordinator(
            CoordinatorMessage message,
            int senderId)
            throws RemoteException;

    void receiveTermEnd(
            TermEndMessage message,
            int senderId)
            throws RemoteException;
}