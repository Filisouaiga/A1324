/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.example.cs324_a1;

/**
 *
 * @author janth
 */

import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.election.ElectionManager;
import com.example.cs324_a1.election.ElectionMessage;
import com.example.cs324_a1.worker.WorkerNode;

public class Main {

    public static void main(String[] args) {

        try {

            System.out.println("==============================");
            System.out.println(" CS324 LEADER ELECTION TEST");
            System.out.println("==============================");

            WorkerNode w1 = new WorkerNode(1, 2);
            WorkerNode w2 = new WorkerNode(2, 0);
            WorkerNode w3 = new WorkerNode(3, 1);
            WorkerNode w4 = new WorkerNode(4, 0);

            /*
                 W1 ------- W2
                 |          |
                 |          |
                 W3 ------- W4
            */

            connect(w1, w2);
            connect(w1, w3);
            connect(w2, w4);
            connect(w3, w4);

            System.out.println("\nWorkers:");

            System.out.println(w1.getInfo());
            System.out.println(w2.getInfo());
            System.out.println(w3.getInfo());
            System.out.println(w4.getInfo());

            System.out.println("\nStarting election from Worker 1...\n");

            ElectionMessage election =
                    w1.getElectionManager()
                            .createElection(w1.getWorkerId());

            w1.receiveElection(election);

            ElectionManager manager =
                    w1.getElectionManager();

            WorkerInfo winner =
                    manager.selectCoordinator(
                            election.getCandidates()
                    );

            if (winner != null) {

                System.out.println("\n==============================");
                System.out.println(" ELECTION RESULT");
                System.out.println("==============================");

                System.out.println(
                        "Winner: Worker "
                        + winner.getWorkerId()
                );

                System.out.println(
                        "JAC: " + winner.getJac()
                );
            }

        } catch (Exception e) {

            System.err.println(
                    "Election error: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static void connect(
            WorkerNode a,
            WorkerNode b) {

        a.addNeighbour(b);
        b.addNeighbour(a);
    }
}
