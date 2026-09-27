/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.client;

import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.jobtype.JobType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

// Command-line client without a GUI. Handy for starting several client
// processes at once from a script or from Part3Tester.
//
// Usage:
//   ClientConsole [--bootstrap=host:port] [--copies=N] MAX 4,19,7,88
//   ClientConsole PRIMECOUNT 2 3 4 5 6 7
//   ClientConsole PRIMESUM 1 1000
//   ClientConsole CSV MAX numbers.csv        (numbers read from a CSV file)
//   ClientConsole BATCH jobs.csv             (one job per row)
//
// Every job is submitted at the same time. Each result is printed as
//   RESULT <job> = <value> | coordinator=W<id> term=<t> | parts=<n>
// and the exit code is 0 only if every job succeeded.
public class ClientConsole {

    public static void main(String[] args) throws Exception {

        String host = "localhost";
        int port = 1099;
        int copies = 1;
        List<String> rest = new ArrayList<>();

        for (String arg : args) {
            if (arg.startsWith("--bootstrap=")) {
                String[] address = arg.substring("--bootstrap=".length()).split(":");
                host = address[0];
                port = Integer.parseInt(address[1]);
            } else if (arg.startsWith("--copies=")) {
                copies = Integer.parseInt(arg.substring("--copies=".length()));
            } else {
                rest.add(arg);
            }
        }

        String clientId = "Console-" + ProcessHandle.current().pid();
        List<JobRequest> jobs;

        try {
            jobs = parseJobs(rest, copies, clientId);

            if (jobs.isEmpty()) {
                throw new IllegalArgumentException("no valid jobs to submit");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Usage: ClientConsole [--bootstrap=host:port] [--copies=N] "
                    + "(MAX|PRIMECOUNT <numbers> | PRIMESUM <start> <end> | CSV <type> <file> | BATCH <file>)");
            System.exit(2);
            return;
        }

        JobClient client = new JobClient(host, port, clientId);
        System.out.println("[" + clientId + "] Submitting " + jobs.size() + " job(s) via Bootstrap " + client.getBootstrapAddress());

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(8, jobs.size()));
        List<Future<JobResult>> futures = new ArrayList<>();

        for (JobRequest job : jobs) {
            futures.add(executor.submit(() -> client.submit(job,
                    message -> System.out.println("[" + clientId + "] " + job.describe() + ": " + message))));
        }

        int failed = 0;

        for (int i = 0; i < jobs.size(); i++) {

            try {
                JobResult result = futures.get(i).get();
                System.out.println("RESULT " + jobs.get(i).describe() + " = " + result.getResult()
                        + " | coordinator=W" + result.getWorkerId() + " term=" + result.getCoordinatorTerm()
                        + " | parts=" + result.getPartialResults().size());
            } catch (Exception e) {
                failed++;
                System.out.println("FAILED " + jobs.get(i).describe() + " : " + JobClient.rootMessage(e));
            }
        }

        executor.shutdown();
        System.exit(failed == 0 ? 0 : 1);
    }

    private static List<JobRequest> parseJobs(List<String> args, int copies, String clientId) throws Exception {

        if (args.isEmpty()) {
            throw new IllegalArgumentException("no job given");
        }

        String command = args.get(0).toUpperCase();

        if (command.equals("BATCH")) {

            if (args.size() != 2) {
                throw new IllegalArgumentException("BATCH needs a file");
            }

            CsvLoader.BatchData batch = CsvLoader.readBatch(Path.of(args.get(1)), clientId);
            batch.getErrors().forEach(error -> System.err.println("Skipped - " + error));
            return repeat(batch.getJobs(), copies);
        }

        List<Integer> numbers;
        JobType type;

        if (command.equals("CSV")) {

            if (args.size() != 3) {
                throw new IllegalArgumentException("CSV needs a job type and a file");
            }

            type = JobType.valueOf(args.get(1).toUpperCase());
            numbers = CsvLoader.readNumbers(Path.of(args.get(2))).getNumbers();

        } else {

            type = JobType.valueOf(command);
            CsvLoader.NumberData data = CsvLoader.parseNumbers(String.join(" ", args.subList(1, args.size())));

            if (data.getSkippedCount() > 0) {
                throw new IllegalArgumentException("not whole numbers: " + data.getSkippedSamples());
            }

            numbers = data.getNumbers();
        }

        JobRequest job;

        if (type == JobType.PRIMESUM) {

            if (numbers.size() != 2 || numbers.get(0) > numbers.get(1)) {
                throw new IllegalArgumentException("PRIMESUM needs <start> <end> with start <= end");
            }

            job = new JobRequest(type, numbers.get(0), numbers.get(1), clientId);

        } else {

            if (numbers.isEmpty()) {
                throw new IllegalArgumentException(type + " needs at least one number");
            }

            job = new JobRequest(type, numbers, clientId);
        }

        return repeat(Arrays.asList(job), copies);
    }

    // Each copy is a separate JobRequest with its own job ID
    private static List<JobRequest> repeat(List<JobRequest> jobs, int copies) {

        List<JobRequest> result = new ArrayList<>();

        for (int c = 0; c < copies; c++) {
            for (JobRequest job : jobs) {
                result.add(job.getJobType() == JobType.PRIMESUM
                        ? new JobRequest(job.getJobType(), job.getStart(), job.getEnd(), job.getClientId())
                        : new JobRequest(job.getJobType(), job.getNumbers(), job.getClientId()));
            }
        }

        return result;
    }
}
