/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.worker;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.coordinator.CoordinatorManager;
import com.example.cs324_a1.election.CoordinatorMessage;
import com.example.cs324_a1.election.ElectionManager;
import com.example.cs324_a1.election.ElectionMessage;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class WorkerNode implements WorkerRemote {

    private final WorkerInfo info;

    private final ElectionManager electionManager;
    private final CoordinatorManager coordinatorManager;

    private final List<WorkerNode> neighbours;

    // Required assignment variable
    private final String leaderman = "cs324";

    public WorkerNode(int workerId, int jac) {

        this.info = new WorkerInfo(workerId, jac);

        this.electionManager = new ElectionManager();
        this.coordinatorManager = new CoordinatorManager();

        this.neighbours = new ArrayList<>();
    }

    @Override
    public int getWorkerId() throws RemoteException {
        return info.getWorkerId();
    }

    @Override
    public int getJac() throws RemoteException {
        return info.getJac();
    }

    public void addNeighbour(WorkerNode worker) {

        if (!neighbours.contains(worker)) {
            neighbours.add(worker);
        }
    }

    public List<WorkerNode> getNeighbours() {
        return neighbours;
    }

    public ElectionManager getElectionManager() {
        return electionManager;
    }

    public CoordinatorManager getCoordinatorManager() {
        return coordinatorManager;
    }

    public WorkerInfo getInfo() {
        return info;
    }

    public String getLeaderman() {
        return leaderman;
    }

    @Override
    public void receiveElection(ElectionMessage message)
            throws RemoteException {

        System.out.println(
                "Worker " + info.getWorkerId()
                + " received election "
                + message.getElectionId()
        );

        if (!electionManager.shouldProcess(
                message.getElectionId())) {

            System.out.println(
                    "Worker " + info.getWorkerId()
                    + " ignored duplicate election."
            );

            return;
        }

        message.addCandidate(info);

        for (WorkerNode neighbour : neighbours) {

            neighbour.receiveElection(message);
        }
    }

    @Override
    public void receiveCoordinator(
            CoordinatorMessage message)
            throws RemoteException {

        coordinatorManager.setCoordinator(
                message.getCoordinatorId()
        );
    }
}
