/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.jobtype;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobResult implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String jobId;
    private final JobType jobType;
    private final long result;

    // For a partial result: the worker that computed it.
    // For a combined result: the coordinator that split and combined the job.
    private final int workerId;

    // Which portion of the job this result covers, e.g. "range 1-250"
    private final String detail;

    // Java thread that executed the work (partial results only)
    private final String threadName;

    private final long startedAt;
    private final long finishedAt;

    // Coordinator term the job was accepted in (combined results only, otherwise -1)
    private final int coordinatorTerm;

    // Partial results that were combined into this result (empty for partial results)
    private final List<JobResult> partialResults;

    public JobResult(String jobId, JobType jobType, long result, int workerId) {

        this(jobId, jobType, result, workerId, "", "", 0, 0, -1, null);
    }

    private JobResult(String jobId, JobType jobType, long result, int workerId, String detail, String threadName,
            long startedAt, long finishedAt, int coordinatorTerm, List<JobResult> partialResults) {

        this.jobId = jobId;
        this.jobType = jobType;
        this.result = result;
        this.workerId = workerId;
        this.detail = detail;
        this.threadName = threadName;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.coordinatorTerm = coordinatorTerm;
        this.partialResults = partialResults == null
                ? new ArrayList<>()
                : new ArrayList<>(partialResults);
    }

    // Result of one worker's portion of a distributed job
    public static JobResult partial(JobRequest part, long result, int workerId, String threadName,
            long startedAt, long finishedAt) {

        return new JobResult(part.getJobId(), part.getJobType(), result, workerId, part.describePortion(),
                threadName, startedAt, finishedAt, -1, null);
    }

    // Copy of a partial result with extra information added to its detail text
    public JobResult withNote(String note) {

        return new JobResult(jobId, jobType, result, workerId, detail + " (" + note + ")", threadName,
                startedAt, finishedAt, coordinatorTerm, partialResults);
    }

    // Final result the coordinator returns to the client
    public static JobResult combined(JobRequest request, long result, int coordinatorId, int term,
            List<JobResult> partialResults, long startedAt, long finishedAt) {

        return new JobResult(request.getJobId(), request.getJobType(), result, coordinatorId,
                request.describePortion(), "", startedAt, finishedAt, term, partialResults);
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

    public String getDetail() {
        return detail;
    }

    public String getThreadName() {
        return threadName;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getFinishedAt() {
        return finishedAt;
    }

    public long getDurationMillis() {
        return Math.max(0, finishedAt - startedAt);
    }

    public int getCoordinatorTerm() {
        return coordinatorTerm;
    }

    public List<JobResult> getPartialResults() {
        return Collections.unmodifiableList(partialResults);
    }

    @Override
    public String toString() {

        return "JobResult{" + "jobId='" + jobId + '\'' + ", jobType=" + jobType + ", result=" + result + ", workerId=" + workerId + '}';
    }
}
