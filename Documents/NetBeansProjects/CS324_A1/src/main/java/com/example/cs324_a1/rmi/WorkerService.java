/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.example.cs324_a1.rmi;

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.coordinator.TermEndMessage;
import com.example.cs324_a1.election.CoordinatorMessage;
import com.example.cs324_a1.election.ElectionMessage;
import com.example.cs324_a1.election.ElectionReply;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface WorkerService extends Remote {

    int getId() throws RemoteException;

    WorkerInfo getInfo() throws RemoteException;

    String getStatus() throws RemoteException;

    void addNeighbour(WorkerInfo neighbour) throws RemoteException;

    List<WorkerInfo> getNeighbours() throws RemoteException;

    String ping( String message) throws RemoteException;

    int getJac() throws RemoteException;

    void startElection() throws RemoteException;

    void receiveElection(ElectionMessage message, int senderId) throws RemoteException;

    void receiveElectionReply( ElectionReply reply) throws RemoteException;

    boolean hasCoordinator() throws RemoteException;

    boolean isCoordinator() throws RemoteException;

    int getCoordinatorId() throws RemoteException;

    int getCoordinatorTerm() throws RemoteException;

    void receiveCoordinator(CoordinatorMessage message, int senderId) throws RemoteException;

    void receiveTermEnd(TermEndMessage message, int senderId) throws RemoteException;

    int getProcessedElectionCount() throws RemoteException;

    JobResult submitJob(JobRequest request) throws RemoteException;

    JobResult executeSubJob(JobRequest request) throws RemoteException;
}