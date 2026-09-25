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

public class WorkerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int workerId;
    private int jac;

    public WorkerInfo(int workerId, int jac) {
        this.workerId = workerId;
        this.jac = jac;
    }

    public int getWorkerId() {
        return workerId;
    }

    public int getJac() {
        return jac;
    }

    public void incrementJac() {
        jac++;
    }

    @Override
    public String toString() {
        return "Worker " + workerId + " | JAC = " + jac;
    }
}
