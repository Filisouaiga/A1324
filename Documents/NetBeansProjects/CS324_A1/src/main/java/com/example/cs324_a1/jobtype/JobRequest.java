/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.jobtype;

/**
 *
 * @author janth
 */

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final JobType jobType;

    // Used by MAX and PRIMECOUNT
    private final List<Integer> numbers;

    // Used by PRIMESUM
    private final int start;
    private final int end;


    // =========================================================
    // MAX / PRIMECOUNT
    // =========================================================

    public JobRequest(
            JobType jobType,
            List<Integer> numbers) {

        this.jobId =
                UUID.randomUUID().toString();

        this.jobType =
                jobType;

        this.numbers =
                numbers == null
                ? new ArrayList<>()
                : new ArrayList<>(numbers);

        this.start = 0;
        this.end = 0;
    }


    // =========================================================
    // PRIMESUM
    // =========================================================

    public JobRequest(
            JobType jobType,
            int start,
            int end) {

        this.jobId =
                UUID.randomUUID().toString();

        this.jobType =
                jobType;

        this.start = start;
        this.end = end;

        this.numbers =
                new ArrayList<>();
    }


    public String getJobId() {
        return jobId;
    }


    public JobType getJobType() {
        return jobType;
    }


    public List<Integer> getNumbers() {
        return new ArrayList<>(numbers);
    }


    public int getStart() {
        return start;
    }


    public int getEnd() {
        return end;
    }
}
