/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.jobtype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JobRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String jobId;
    private final JobType jobType;
    private final List<Integer> numbers;
    private final int start;
    private final int end;

    // Which client process submitted the job (shown in coordinator logs)
    private final String clientId;

    public JobRequest(JobType jobType, List<Integer> numbers) {

        this(jobType, numbers, "unknown-client");
    }

    public JobRequest(JobType jobType, int start, int end) {

        this(jobType, start, end, "unknown-client");
    }

    public JobRequest(JobType jobType, List<Integer> numbers, String clientId) {

        this.jobId = UUID.randomUUID().toString();

        this.jobType = jobType;

        this.numbers = numbers == null
                ? new ArrayList<>()
                : new ArrayList<>(numbers);

        this.start = 0;
        this.end = 0;

        this.clientId = clientId;
    }

    public JobRequest(JobType jobType, int start, int end, String clientId) {

        this.jobId = UUID.randomUUID().toString();

        this.jobType = jobType;

        this.start = start;
        this.end = end;

        this.numbers = new ArrayList<>();

        this.clientId = clientId;
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

    // Avoids copying the whole list when only the size is needed
    public int getNumberCount() {
        return numbers.size();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getClientId() {
        return clientId;
    }

    // Short human-readable form, e.g. "PRIMESUM(1-1000)" or "MAX(250 numbers)"
    public String describe() {

        if (jobType == JobType.PRIMESUM) {
            return jobType + "(" + start + "-" + end + ")";
        }

        return jobType + "(" + numbers.size() + " numbers)";
    }

    // Describes the portion of work this request covers, used for partial results
    public String describePortion() {

        if (jobType == JobType.PRIMESUM) {
            return "range " + start + "-" + end;
        }

        return numbers.size() + " numbers";
    }

    @Override
    public String toString() {

        return "JobRequest{" + "jobId='" + jobId + '\'' + ", job=" + describe() + ", client=" + clientId + '}';
    }
}
