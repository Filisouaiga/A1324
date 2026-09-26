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

public class JobResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String jobId;

    private final JobType jobType;

    private final long result;

    private final int workerId;


    public JobResult(
            String jobId,
            JobType jobType,
            long result,
            int workerId) {

        this.jobId = jobId;
        this.jobType = jobType;
        this.result = result;
        this.workerId = workerId;
    }


    public String getJobId() {
        return jobId;
    }


    public JobType getJobType() {
        return jobType;
    }


    public long getResult() {
        return result;
    }


    public int getWorkerId() {
        return workerId;
    }


    @Override
    public String toString() {

        return "JobResult{"
                + "jobId='" + jobId + '\''
                + ", jobType=" + jobType
                + ", result=" + result
                + ", workerId=" + workerId
                + '}';
    }
}
