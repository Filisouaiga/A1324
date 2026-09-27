/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.common;

import java.io.Serializable;
import java.util.Objects;

public class WorkerInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    private final int workerId;
    private final String host;
    private final int port;
    private final String rmiName;
    private int jac;

    public WorkerInfo(int workerId, String host, int port, String rmiName) {

        this(workerId, host, port, rmiName, 0);
    }

    public WorkerInfo(int workerId,String host,int port,String rmiName,int jac) {

        if (workerId < 0) {
            throw new IllegalArgumentException("Worker ID cannot be negative.");
        }

        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Worker host cannot be empty.");
        }

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid worker port: " + port);
        }

        if (rmiName == null || rmiName.isBlank()) {
            throw new IllegalArgumentException("RMI name cannot be empty.");
        }

        this.workerId = workerId;
        this.host = host;
        this.port = port;
        this.rmiName = rmiName;

        this.jac = Math.max(0,jac);
    }

    public WorkerInfo(
            int workerId,
            int jac) {

        this(workerId, "localhost", 1100 + workerId, "WorkerService-" + workerId, jac);
    }

    public int getWorkerId() {

        return workerId;
    }

    public int getId() {

        return workerId;
    }

    public String getHost() {

        return host;
    }

    public int getPort() {

        return port;
    }

    public String getRmiName() {

        return rmiName;
    }

    public String getRmiUrl() {

        return "rmi://" + host + ":" + port + "/" + rmiName;
    }

    public synchronized int getJac() {

        return jac;
    }

    public synchronized void incrementJac() {

        jac++;

        System.out.println("[JAC] Worker " + workerId + " JAC increased to " + jac );
    }

    public synchronized void setJac(
            int jac) {

        if (jac < 0) {

            throw new IllegalArgumentException("JAC cannot be negative.");
        }

        this.jac = jac;
    }

    public synchronized void resetJac() {

        jac = 0;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {

            return true;
        }

        if (!(obj instanceof WorkerInfo)) {

            return false;
        }

        WorkerInfo other = (WorkerInfo) obj;

        return workerId == other.workerId;
    }

    @Override
    public int hashCode() {

        return Objects.hash(workerId);
    }

    @Override
    public synchronized String toString() {

        return "WorkerInfo{" + "id=" + workerId + ", host=" + host + ", port=" + port + ", rmiName=" + rmiName + ", JAC=" + jac + '}';
    }
}