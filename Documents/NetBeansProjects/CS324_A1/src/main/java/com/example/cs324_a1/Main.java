/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.example.cs324_a1;

/**
 *
 * @author janth
 */

public class Main {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("      CS324 DISTRIBUTED SYSTEM");
        System.out.println("========================================");

        System.out.println();
        System.out.println("The distributed system uses separate");
        System.out.println("Java processes for Bootstrap and workers.");

        System.out.println();
        System.out.println("Start components in this order:");

        System.out.println();
        System.out.println("1. Start BootstrapNode");

        System.out.println();
        System.out.println("2. Start Worker 1");
        System.out.println("   WorkerNode 1");

        System.out.println();
        System.out.println("3. Start Worker 2");
        System.out.println("   WorkerNode 2");

        System.out.println();
        System.out.println("4. Start Worker 3");
        System.out.println("   WorkerNode 3");

        System.out.println();
        System.out.println("5. Start Worker 4");
        System.out.println("   WorkerNode 4");

        System.out.println();
        System.out.println("Each worker will:");
        System.out.println("- register with Bootstrap");
        System.out.println("- connect to a random active worker");
        System.out.println("- communicate using Java RMI");
        System.out.println("- participate in leader elections");
        System.out.println("- execute distributed jobs");

        System.out.println();
        System.out.println("========================================");
    }
}