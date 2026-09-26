/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.common;

/**
 *
 * @author janth
 */

import java.io.Serializable;
import java.util.Objects;

/**
 * Information about a worker in the distributed system.
 *
 * Combines:
 * - Worker identity and RMI connection information
 * - Job Allocation Counter (JAC) for leader election
 */
public class WorkerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    // =========================================================
    // WORKER / RMI INFORMATION
    // =========================================================

    private final int workerId;
    private final String host;
    private final int port;
    private final String rmiName;

    // =========================================================
    // PERSON 2 - JOB ALLOCATION COUNTER
    // =========================================================

    private int jac;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    /**
     * Creates a worker with JAC starting at 0.
     * Used when a normal worker joins the distributed system.
     */
    public WorkerInfo(
            int workerId,
            String host,
            int port,
            String rmiName) {

        this(workerId, host, port, rmiName, 0);
    }

    /**
     * Creates a worker with a specified JAC.
     * Useful for election testing.
     */
    public WorkerInfo(
            int workerId,
            String host,
            int port,
            String rmiName,
            int jac) {

        this.workerId = workerId;
        this.host = host;
        this.port = port;
        this.rmiName = rmiName;
        this.jac = Math.max(0, jac);
    }

    /**
     * Keeps compatibility with the original Person 2 tests.
     *
     * This constructor should only be used for local/testing
     * situations where RMI information is not required.
     */
    public WorkerInfo(int workerId, int jac) {

        this(
                workerId,
                "localhost",
                1100 + workerId,
                "WorkerService-" + workerId,
                jac
        );
    }

    // =========================================================
    // WORKER ID
    // =========================================================

    /**
     * Person 2 compatibility method.
     */
    public int getWorkerId() {
        return workerId;
    }

    /**
     * Person 1 compatibility method.
     */
    public int getId() {
        return workerId;
    }

    // =========================================================
    // RMI INFORMATION
    // =========================================================

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRmiName() {
        return rmiName;
    }

    /**
     * Full RMI lookup URL.
     *
     * Example:
     * rmi://localhost:1101/WorkerService-1
     */
    public String getRmiUrl() {

        return "rmi://"
                + host
                + ":"
                + port
                + "/"
                + rmiName;
    }

    // =========================================================
    // JAC
    // =========================================================

    public synchronized int getJac() {
        return jac;
    }

    /**
     * Increase JAC when this worker, while acting as coordinator,
     * assigns work to another worker.
     */
    public synchronized void incrementJac() {

        jac++;

        System.out.println(
                "[JAC] Worker "
                + workerId
                + " JAC increased to "
                + jac
        );
    }

    // =========================================================
    // OBJECT METHODS
    // =========================================================

    @Override
    public synchronized String toString() {

        return "WorkerInfo{"
                + "id=" + workerId
                + ", host=" + host
                + ", port=" + port
                + ", rmiName=" + rmiName
                + ", JAC=" + jac
                + '}';
    }

    /**
     * Worker ID uniquely identifies a worker.
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (!(obj instanceof WorkerInfo)) {
            return false;
        }

        WorkerInfo other =
                (WorkerInfo) obj;

        return workerId == other.workerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(workerId);
    }
}