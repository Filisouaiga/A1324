/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.example.cs324_a1;

public class Main {

    public static void main(String[] args) {

        // Display the system title
        System.out.println("========================================");
        System.out.println("      CS324 DISTRIBUTED SYSTEM");
        System.out.println("========================================");

        System.out.println();

        // Display the startup instructions
        System.out.println("System startup:");
        System.out.println();

        // Step 1: Start the Bootstrap Node
        System.out.println("1. Start the Bootstrap Node.");
        System.out.println();

        // Step 2: Start the Worker Launcher
        System.out.println("2. Start the Worker Launcher.");
        System.out.println();

        // Step 3: Add worker nodes to the distributed system
        System.out.println("3. Use the Worker Launcher to add");
        System.out.println("   workers dynamically to the system.");
        System.out.println();

        // Explain what happens when a new worker joins the system
        System.out.println("When a worker joins, it will:");

        // Each worker uses a unique integer ID
        System.out.println("   - obtain/use a unique integer ID");

        // Worker connects to the Bootstrap Node
        System.out.println("   - connect to the Bootstrap Node");

        // Worker connects to a random active worker in the network
        System.out.println("   - connect to a random active worker");

        // Worker registers its information with the Bootstrap Node
        System.out.println("   - register with the Bootstrap Node");

        // Workers communicate with each other using Java RMI
        System.out.println("   - communicate using Java RMI");

        // Worker can take part in the leader election process
        System.out.println("   - participate in leader elections");

        // Worker keeps track of its Job Allocation Counter (JAC)
        System.out.println("   - maintain its JAC");

        // Worker can execute parts of distributed computational jobs
        System.out.println("   - process distributed jobs");

        System.out.println();

        // Inform the user that more workers can be added at runtime
        System.out.println("Workers can join while the system");
        System.out.println("is running.");

        System.out.println();

        // Display the end of the startup information
        System.out.println("========================================");
    }
}