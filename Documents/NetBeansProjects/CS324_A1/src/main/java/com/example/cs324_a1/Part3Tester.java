/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.cs324_a1.client.ClientConsole;
import com.example.cs324_a1.client.CsvLoader;
import com.example.cs324_a1.client.JobClient;
import com.example.cs324_a1.common.WorkerInfo;
import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobResult;
import com.example.cs324_a1.jobtype.JobType;
import com.example.cs324_a1.rmi.WorkerService;

// Tests for Person 3 (client, jobs and multithreading) against a RUNNING system.
// Start the Bootstrap Node and at least 2 workers (4 recommended) first.
//
// Usage: Part3Tester [bootstrapHost] [bootstrapPort]
//
// Every expected answer is computed here with an independent sieve of
// Eratosthenes, not with the workers' JobProcessor code.
public class Part3Tester {

    private static final Pattern RANGE = Pattern.compile("range (-?\\d+)-(-?\\d+)");
    private static final Pattern COUNT = Pattern.compile("(\\d+) numbers");

    private static int passed, failed;
    private static String host = "localhost";
    private static int port = 1099;
    private static JobClient client;
    private static int workerCount;
    private static final Random random = new Random(324);

    public static void main(String[] args) {

        if (args.length > 0) host = args[0];
        if (args.length > 1) port = Integer.parseInt(args[1]);

        client = new JobClient(host, port, "Part3Tester-" + ProcessHandle.current().pid(), 120000);

        System.out.println("==================================================");
        System.out.println(" CS324 PERSON 3 - CLIENT, JOBS & MULTITHREADING");
        System.out.println("==================================================");

        try {
            JobClient.ClusterStatus status = client.fetchStatus();
            workerCount = status.getWorkers().size();
            System.out.println("Bootstrap " + client.getBootstrapAddress() + " | " + status);

            if (workerCount < 2) {
                fail("SETUP", "At least 2 active workers are required (4 recommended); found " + workerCount);
                summary();
                return;
            }
        } catch (Exception e) {
            fail("SETUP", JobClient.rootMessage(e));
            summary();
            return;
        }

        testMax();
        testPrimeSum();
        testPrimeCount();
        testInputSizes();
        testCsvInput();
        testMultipleJobsAtOnce();
        testMultipleClients();
        testWorkersInParallel();
        testCombination();
        testThreadSafety();

        summary();
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------

    private static void testMax() {

        heading(1, "MAX");

        try {
            List<Integer> numbers = randomNumbers(5000, -1_000_000, 1_000_000);
            numbers.set(random.nextInt(numbers.size()), 1_234_567);

            JobRequest request = new JobRequest(JobType.MAX, numbers, client.getClientId());
            JobResult result = submit(request);
            printSplit(result);

            List<String> problems = check(request, result, 1_234_567);

            if (problems.isEmpty())
                pass("MAX of 5000 numbers = " + result.getResult() + ", combined from " + result.getPartialResults().size() + " partial maxima.");
            else
                fail("TEST 1", String.join("; ", problems));

        } catch (Exception e) {
            error("TEST 1", e);
        }
    }

    private static void testPrimeSum() {

        heading(2, "PRIMESUM");

        try {
            JobRequest request = new JobRequest(JobType.PRIMESUM, 1, 1000, client.getClientId());
            JobResult result = submit(request);
            printSplit(result);

            List<String> problems = check(request, result, 76127);

            if (problems.isEmpty())
                pass("PRIMESUM(1,1000) = 76127, range split evenly across " + result.getPartialResults().size() + " workers.");
            else
                fail("TEST 2", String.join("; ", problems));

        } catch (Exception e) {
            error("TEST 2", e);
        }
    }

    private static void testPrimeCount() {

        heading(3, "PRIMECOUNT");

        try {
            List<Integer> numbers = new ArrayList<>();
            for (int i = 1; i <= 10000; i++) numbers.add(i);

            JobRequest request = new JobRequest(JobType.PRIMECOUNT, numbers, client.getClientId());
            JobResult result = submit(request);
            printSplit(result);

            List<String> problems = check(request, result, 1229);

            if (problems.isEmpty())
                pass("PRIMECOUNT(1..10000) = 1229 primes, combined from " + result.getPartialResults().size() + " partial counts.");
            else
                fail("TEST 3", String.join("; ", problems));

        } catch (Exception e) {
            error("TEST 3", e);
        }
    }

    private static void testInputSizes() {

        heading(4, "DIFFERENT INPUT SIZES");

        List<JobRequest> requests = new ArrayList<>();
        String id = client.getClientId();

        requests.add(new JobRequest(JobType.MAX, Arrays.asList(42), id));
        requests.add(new JobRequest(JobType.MAX, Arrays.asList(-5, -2, -9), id));
        requests.add(new JobRequest(JobType.PRIMECOUNT, Arrays.asList(7, 9), id));
        requests.add(new JobRequest(JobType.PRIMECOUNT, Arrays.asList(-7, 0, 1, 2), id));
        requests.add(new JobRequest(JobType.PRIMESUM, 13, 13, id));
        requests.add(new JobRequest(JobType.PRIMESUM, 14, 16, id));
        requests.add(new JobRequest(JobType.PRIMESUM, -10, 10, id));
        requests.add(new JobRequest(JobType.PRIMESUM, 1, 2_000_000, id));
        requests.add(new JobRequest(JobType.PRIMECOUNT, randomNumbers(200_000, 1, 1_000_000), id));
        requests.add(new JobRequest(JobType.MAX, randomNumbers(500_000, -50_000_000, 50_000_000), id));

        int wrong = 0;

        for (JobRequest request : requests) {
            try {
                long start = System.currentTimeMillis();
                JobResult result = submit(request);
                List<String> problems = check(request, result, expected(request));

                System.out.printf("  %-5s %-28s = %-14d parts=%d  %d ms%s%n", problems.isEmpty() ? "ok" : "WRONG",
                        request.describe(), result.getResult(), result.getPartialResults().size(),
                        System.currentTimeMillis() - start, problems.isEmpty() ? "" : "  " + problems);

                if (!problems.isEmpty()) wrong++;

            } catch (Exception e) {
                wrong++;
                System.out.println("  ERROR " + request.describe() + ": " + JobClient.rootMessage(e));
            }
        }

        if (wrong == 0)
            pass("All " + requests.size() + " input sizes (1 number up to 500,000 numbers, single value up to 2,000,000 range) were correct.");
        else
            fail("TEST 4", wrong + " of " + requests.size() + " input sizes gave a wrong result.");
    }

    private static void testCsvInput() {

        heading(5, "CSV INPUT");

        try {
            Path folder = Files.createTempDirectory("cs324-csv");

            Path numbersFile = folder.resolve("numbers.csv");
            Files.write(numbersFile, ("﻿value\n12, 7, 99\n# comment line\n45;3  \"88\"\nabc, -4\n\n1000000\n")
                    .getBytes(StandardCharsets.UTF_8));

            CsvLoader.NumberData data = CsvLoader.readNumbers(numbersFile);
            List<Integer> expectedNumbers = Arrays.asList(12, 7, 99, 45, 3, 88, -4, 1000000);

            boolean parsedOk = data.getNumbers().equals(expectedNumbers) && data.getSkippedCount() == 2;
            System.out.println("  Parsed numbers.csv -> " + data.getNumbers() + ", skipped " + data.getSkippedSamples()
                    + (parsedOk ? "  (ok)" : "  (WRONG)"));

            JobResult max = submit(new JobRequest(JobType.MAX, data.getNumbers(), client.getClientId()));
            JobResult count = submit(new JobRequest(JobType.PRIMECOUNT, data.getNumbers(), client.getClientId()));
            System.out.println("  MAX from CSV = " + max.getResult() + ", PRIMECOUNT from CSV = " + count.getResult());

            Path batchFile = folder.resolve("batch.csv");
            Files.write(batchFile, ("# job,values\nMAX,4,19,7,88,23\nPRIMESUM,1,1000\nPRIMECOUNT,2,3,4,5,6,7,8,9,10,11\n"
                    + "HELLO,1,2\nPRIMESUM,50,10\n").getBytes(StandardCharsets.UTF_8));

            CsvLoader.BatchData batch = CsvLoader.readBatch(batchFile, client.getClientId());
            batch.getErrors().forEach(e -> System.out.println("  Batch row rejected: " + e));

            List<JobResult> batchResults = submitAll(batch.getJobs());
            List<Long> batchValues = new ArrayList<>();
            for (JobResult r : batchResults) batchValues.add(r.getResult());
            System.out.println("  Batch jobs " + batch.getJobs().size() + " -> results " + batchValues);

            boolean ok = parsedOk
                    && max.getResult() == 1000000 && count.getResult() == 2
                    && batch.getJobs().size() == 3 && batch.getErrors().size() == 2
                    && batchValues.equals(Arrays.asList(88L, 76127L, 5L));

            if (ok)
                pass("CSV numbers (header, BOM, comments, mixed separators, bad tokens) and a 3-job batch file were processed correctly.");
            else
                fail("TEST 5", "CSV parsing or CSV job results were not as expected.");

        } catch (Exception e) {
            error("TEST 5", e);
        }
    }

    private static void testMultipleJobsAtOnce() {

        heading(6, "MULTIPLE JOBS AT ONCE (ONE CLIENT)");

        try {
            List<JobRequest> requests = new ArrayList<>();
            for (int i = 0; i < 10; i++)
                requests.add(new JobRequest(JobType.PRIMESUM, 1, 3_000_000 + i * 10_000, client.getClientId()));

            long start = System.currentTimeMillis();
            List<JobResult> results = submitAll(requests);
            long wall = System.currentTimeMillis() - start;

            int wrong = 0;
            long coordinatorTime = 0;
            List<JobResult> allParts = new ArrayList<>();

            for (int i = 0; i < requests.size(); i++) {
                if (!check(requests.get(i), results.get(i), expected(requests.get(i))).isEmpty()) wrong++;
                coordinatorTime += results.get(i).getDurationMillis();
                allParts.addAll(results.get(i).getPartialResults());
            }

            // Did any single worker run several sub-jobs at the same moment on different threads?
            int bestWorker = -1, bestOverlap = 0;
            for (Map.Entry<Integer, List<JobResult>> entry : groupByWorker(allParts).entrySet()) {
                int overlap = maxOverlap(entry.getValue());
                Set<String> threads = new TreeSet<>();
                for (JobResult part : entry.getValue()) threads.add(part.getThreadName());
                System.out.println("  Worker " + entry.getKey() + ": " + entry.getValue().size() + " sub-jobs, up to "
                        + overlap + " at the same time, threads " + threads);
                if (overlap > bestOverlap) {
                    bestOverlap = overlap;
                    bestWorker = entry.getKey();
                }
            }

            System.out.println("  10 jobs finished in " + wall + " ms wall-clock; their processing times add up to "
                    + coordinatorTime + " ms");

            if (wrong == 0 && bestOverlap >= 2)
                pass("10 concurrent jobs all correct; Worker " + bestWorker + " processed " + bestOverlap
                        + " sub-jobs simultaneously on separate threads.");
            else
                fail("TEST 6", wrong + " wrong result(s); highest concurrency on one worker = " + bestOverlap);

        } catch (Exception e) {
            error("TEST 6", e);
        }
    }

    private static void testMultipleClients() {

        heading(7, "MULTIPLE CLIENT PROCESSES");

        try {
            Path folder = Files.createTempDirectory("cs324-clients");
            List<Integer> csvNumbers = randomNumbers(50_000, 1, 1_000_000);
            Path csv = folder.resolve("client-numbers.csv");
            StringBuilder text = new StringBuilder("value\n");
            for (int n : csvNumbers) text.append(n).append('\n');
            Files.write(csv, text.toString().getBytes(StandardCharsets.UTF_8));

            List<Integer> maxNumbers = randomNumbers(20, -1000, 1000);
            StringBuilder maxArg = new StringBuilder();
            for (int n : maxNumbers) maxArg.append(maxArg.length() == 0 ? "" : ",").append(n);

            List<List<String>> commands = Arrays.asList(
                    Arrays.asList("--copies=2", "PRIMESUM", "1", "1000000"),
                    Arrays.asList("CSV", "PRIMECOUNT", csv.toString()),
                    Arrays.asList("MAX", maxArg.toString()));

            long[][] expectedValues = {
                    {refPrimeSum(1, 1_000_000), refPrimeSum(1, 1_000_000)},
                    {refPrimeCount(csvNumbers)},
                    {refMax(maxNumbers)}};

            String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            List<Process> processes = new ArrayList<>();
            List<File> outputs = new ArrayList<>();

            // All three client processes are started before waiting for any of them.
            // Output goes to files: reading their pipes one by one could block a process
            // whose pipe buffer filled up while we were waiting on another.
            for (int i = 0; i < commands.size(); i++) {
                List<String> command = new ArrayList<>(Arrays.asList(java, "-cp", System.getProperty("java.class.path"),
                        ClientConsole.class.getName(), "--bootstrap=" + host + ":" + port));
                command.addAll(commands.get(i));

                File output = folder.resolve("client-" + (i + 1) + ".log").toFile();
                processes.add(new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output).start());
                outputs.add(output);
            }

            boolean ok = true;

            for (int i = 0; i < processes.size(); i++) {
                Process process = processes.get(i);
                boolean finished = process.waitFor(180, TimeUnit.SECONDS);
                if (!finished) process.destroyForcibly();

                List<Long> values = new ArrayList<>();
                String clientName = "?";

                for (String line : Files.readAllLines(outputs.get(i).toPath())) {
                    if (line.startsWith("RESULT ")) values.add(Long.parseLong(line.split(" = ")[1].split(" ")[0].trim()));
                    if (line.startsWith("[Console-") && clientName.equals("?")) clientName = line.substring(1, line.indexOf(']'));
                }

                List<Long> expectedList = new ArrayList<>();
                for (long v : expectedValues[i]) expectedList.add(v);

                boolean clientOk = finished && process.exitValue() == 0 && values.equals(expectedList);
                ok &= clientOk;

                System.out.println("  " + clientName + " (PID " + process.pid() + ") " + String.join(" ", commands.get(i)).replace(csv.toString(), "client-numbers.csv")
                        + " -> " + values + (clientOk ? "  (ok)" : "  (WRONG, expected " + expectedList + ", see " + outputs.get(i) + ")"));
            }

            if (ok)
                pass("3 separate client processes submitted jobs at the same time and all received correct results.");
            else
                fail("TEST 7", "At least one client process failed or received a wrong result.");

        } catch (Exception e) {
            error("TEST 7", e);
        }
    }

    private static void testWorkersInParallel() {

        heading(8, "MULTIPLE WORKERS PROCESSING SIMULTANEOUSLY");

        try {
            JobRequest request = new JobRequest(JobType.PRIMESUM, 1, 6_000_000, client.getClientId());
            JobResult result = submit(request);
            printSplit(result);

            List<JobResult> parts = result.getPartialResults();
            Set<Integer> workers = new TreeSet<>();
            long busy = 0;
            for (JobResult part : parts) {
                workers.add(part.getWorkerId());
                busy += part.getDurationMillis();
            }

            int overlap = maxOverlap(parts);
            List<String> problems = check(request, result, expected(request));

            System.out.println("  " + workers.size() + " different workers, up to " + overlap + " computing at the same moment");
            System.out.println("  Total worker compute time " + busy + " ms, finished in " + result.getDurationMillis()
                    + " ms on the coordinator (" + String.format("%.1f", busy / (double) Math.max(1, result.getDurationMillis())) + "x parallel)");

            if (problems.isEmpty() && workers.size() == parts.size() && overlap >= 2)
                pass(overlap + " workers were computing their parts of PRIMESUM(1,6000000) at the same time.");
            else
                fail("TEST 8", "Parallel processing across workers was not observed " + problems);

        } catch (Exception e) {
            error("TEST 8", e);
        }
    }

    private static void testCombination() {

        heading(9, "CORRECT COMBINATION OF PARTIAL RESULTS");

        List<JobRequest> requests = new ArrayList<>();
        List<Integer> ascending = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) ascending.add(i);

        requests.add(new JobRequest(JobType.PRIMESUM, 1, 10, client.getClientId()));
        requests.add(new JobRequest(JobType.MAX, ascending, client.getClientId()));
        requests.add(new JobRequest(JobType.PRIMECOUNT, ascending, client.getClientId()));

        boolean ok = true;

        for (JobRequest request : requests) {
            try {
                JobResult result = submit(request);
                System.out.println("  " + request.describe() + ":");
                printSplit(result);

                // check() also recomputes every partial result on its own
                List<String> problems = check(request, result, expected(request));
                if (!problems.isEmpty()) {
                    ok = false;
                    System.out.println("    WRONG: " + problems);
                }
            } catch (Exception e) {
                ok = false;
                System.out.println("  ERROR " + request.describe() + ": " + JobClient.rootMessage(e));
            }
        }

        if (ok)
            pass("Every partial result was correct for its portion, portions covered the input exactly once, and the combined results were right.");
        else
            fail("TEST 9", "Partial results were not split or combined correctly.");
    }

    private static void testThreadSafety() {

        heading(10, "THREAD SAFETY / RACE CONDITIONS");

        try {
            JobClient.ClusterStatus status = client.fetchStatus();
            Map<Integer, Integer> jacBefore = readJacs(status.getWorkers());

            List<JobRequest> requests = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                requests.add(new JobRequest(JobType.MAX, randomNumbers(20_000, -1_000_000, 1_000_000), client.getClientId()));
                requests.add(new JobRequest(JobType.PRIMESUM, 1, 800_000 + i, client.getClientId()));
                requests.add(new JobRequest(JobType.PRIMECOUNT, randomNumbers(20_000, 1, 1_000_000), client.getClientId()));
            }

            // Release all 15 threads at the same instant to maximise contention
            ExecutorService pool = Executors.newFixedThreadPool(requests.size());
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<JobResult>> futures = new ArrayList<>();
            for (JobRequest request : requests) {
                futures.add(pool.submit(() -> {
                    startGate.await();
                    return submit(request);
                }));
            }
            startGate.countDown();

            List<JobResult> results = new ArrayList<>();
            int wrong = 0;
            for (int i = 0; i < futures.size(); i++) {
                JobResult result = futures.get(i).get();
                results.add(result);
                if (!check(requests.get(i), result, expected(requests.get(i))).isEmpty()) wrong++;
            }
            pool.shutdown();

            // (a) Five-job limit: no coordinator term may have accepted more than five of these jobs
            Map<Integer, Integer> jobsPerTerm = new TreeMap<>();
            Map<Integer, Set<Integer>> coordinatorsPerTerm = new TreeMap<>();
            for (JobResult result : results) {
                jobsPerTerm.merge(result.getCoordinatorTerm(), 1, Integer::sum);
                coordinatorsPerTerm.computeIfAbsent(result.getCoordinatorTerm(), t -> new TreeSet<>()).add(result.getWorkerId());
            }
            boolean limitOk = true;
            for (Map.Entry<Integer, Integer> entry : jobsPerTerm.entrySet()) {
                System.out.println("  Term " + entry.getKey() + " (coordinator Worker " + coordinatorsPerTerm.get(entry.getKey())
                        + ") handled " + entry.getValue() + " of these jobs");
                if (entry.getValue() > 5) limitOk = false;
            }

            // (b) Only one coordinator per term
            boolean oneCoordinatorPerTerm = true;
            for (Set<Integer> coordinators : coordinatorsPerTerm.values())
                if (coordinators.size() > 1) oneCoordinatorPerTerm = false;

            // (c) JAC: concurrent dispatch threads increment the coordinator's JAC once per
            // sub-job sent to another worker. A lost update would make the totals disagree.
            Map<Integer, Integer> expectedIncrease = new HashMap<>();
            for (JobResult result : results)
                for (JobResult part : result.getPartialResults())
                    if (part.getWorkerId() != result.getWorkerId() || part.getDetail().contains("reassigned"))
                        expectedIncrease.merge(result.getWorkerId(), 1, Integer::sum);

            Map<Integer, Integer> jacAfter = readJacs(status.getWorkers());
            boolean jacOk = true;
            for (int workerId : jacBefore.keySet()) {
                int actual = jacAfter.getOrDefault(workerId, jacBefore.get(workerId)) - jacBefore.get(workerId);
                int expectedJac = expectedIncrease.getOrDefault(workerId, 0);
                System.out.println("  Worker " + workerId + " JAC " + jacBefore.get(workerId) + " -> " + jacAfter.get(workerId)
                        + " (+" + actual + ", expected +" + expectedJac + ")");
                if (actual != expectedJac) jacOk = false;
            }

            System.out.println("  Correct results: " + (requests.size() - wrong) + "/" + requests.size()
                    + " | five-job limit respected: " + limitOk
                    + " | one coordinator per term: " + oneCoordinatorPerTerm
                    + " | no lost JAC updates: " + jacOk);

            if (wrong == 0 && limitOk && oneCoordinatorPerTerm && jacOk)
                pass("15 simultaneous jobs: all correct, no term exceeded 5 jobs, and every JAC increment was counted.");
            else
                fail("TEST 10", "A race condition was detected (see details above).");

        } catch (Exception e) {
            error("TEST 10", e);
        }
    }

    // ------------------------------------------------------------------
    // Checking results
    // ------------------------------------------------------------------

    // Returns a list of problems (empty = correct). Checks the final value, that the
    // portions cover the input exactly once and evenly, that every partial result is
    // right for its own portion, and that combining the partials gives the final value.
    private static List<String> check(JobRequest request, JobResult result, long expected) {

        List<String> problems = new ArrayList<>();
        List<JobResult> parts = result.getPartialResults();

        if (result.getResult() != expected)
            problems.add("result " + result.getResult() + " but expected " + expected);

        if (parts.isEmpty()) {
            problems.add("no partial results returned");
            return problems;
        }

        long combined = request.getJobType() == JobType.MAX ? Long.MIN_VALUE : 0;
        for (JobResult part : parts)
            combined = request.getJobType() == JobType.MAX ? Math.max(combined, part.getResult()) : combined + part.getResult();

        if (combined != result.getResult())
            problems.add("partials combine to " + combined + " not " + result.getResult());

        List<Integer> sizes = new ArrayList<>();

        if (request.getJobType() == JobType.PRIMESUM) {

            long next = request.getStart();

            for (JobResult part : parts) {
                Matcher m = RANGE.matcher(part.getDetail());
                if (!m.find()) {
                    problems.add("cannot read portion '" + part.getDetail() + "'");
                    return problems;
                }
                int a = Integer.parseInt(m.group(1)), b = Integer.parseInt(m.group(2));
                if (a != next) problems.add("gap or overlap before " + a);
                if (part.getResult() != refPrimeSum(a, b)) problems.add("partial for " + a + "-" + b + " is wrong");
                sizes.add(b - a + 1);
                next = (long) b + 1;
            }

            if (next != (long) request.getEnd() + 1) problems.add("ranges do not end at " + request.getEnd());

        } else {

            List<Integer> numbers = request.getNumbers();
            int index = 0;

            for (JobResult part : parts) {
                Matcher m = COUNT.matcher(part.getDetail());
                if (!m.find()) {
                    problems.add("cannot read portion '" + part.getDetail() + "'");
                    return problems;
                }
                int size = Integer.parseInt(m.group(1));
                if (index + size > numbers.size()) {
                    problems.add("portions cover more numbers than were sent");
                    return problems;
                }
                List<Integer> chunk = numbers.subList(index, index + size);
                long partExpected = request.getJobType() == JobType.MAX ? refMax(chunk) : refPrimeCount(chunk);
                if (part.getResult() != partExpected) problems.add("partial for numbers " + index + ".." + (index + size - 1) + " is wrong");
                sizes.add(size);
                index += size;
            }

            if (index != numbers.size()) problems.add("portions cover " + index + " of " + numbers.size() + " numbers");
        }

        int smallest = sizes.stream().min(Integer::compare).orElse(0);
        int largest = sizes.stream().max(Integer::compare).orElse(0);
        if (largest - smallest > 1) problems.add("uneven split " + sizes);

        return problems;
    }

    private static long expected(JobRequest request) {

        switch (request.getJobType()) {
            case MAX:
                return refMax(request.getNumbers());
            case PRIMESUM:
                return refPrimeSum(request.getStart(), request.getEnd());
            default:
                return refPrimeCount(request.getNumbers());
        }
    }

    // Largest number of time intervals [start, finish] that overlap at one moment
    private static int maxOverlap(List<JobResult> parts) {

        List<long[]> events = new ArrayList<>();
        for (JobResult part : parts) {
            events.add(new long[]{part.getStartedAt(), 1});
            events.add(new long[]{part.getFinishedAt(), -1});
        }
        // Ends before starts at the same millisecond, so touching intervals do not count
        events.sort((x, y) -> x[0] != y[0] ? Long.compare(x[0], y[0]) : Long.compare(x[1], y[1]));

        int current = 0, best = 0;
        for (long[] event : events) {
            current += (int) event[1];
            best = Math.max(best, current);
        }
        return best;
    }

    private static Map<Integer, List<JobResult>> groupByWorker(List<JobResult> parts) {

        Map<Integer, List<JobResult>> groups = new TreeMap<>();
        for (JobResult part : parts) groups.computeIfAbsent(part.getWorkerId(), k -> new ArrayList<>()).add(part);
        return groups;
    }

    // ------------------------------------------------------------------
    // Independent reference answers (sieve of Eratosthenes)
    // ------------------------------------------------------------------

    private static boolean[] sieve = new boolean[0];

    private static synchronized boolean[] primesUpTo(int limit) {

        if (sieve.length <= limit) {
            boolean[] prime = new boolean[limit + 1];
            Arrays.fill(prime, true);
            prime[0] = false;
            if (limit >= 1) prime[1] = false;
            for (long i = 2; i * i <= limit; i++)
                if (prime[(int) i])
                    for (long j = i * i; j <= limit; j += i) prime[(int) j] = false;
            sieve = prime;
        }
        return sieve;
    }

    private static long refPrimeSum(int start, int end) {

        if (end < 2) return 0;
        boolean[] prime = primesUpTo(end);
        long sum = 0;
        for (int n = Math.max(2, start); n <= end; n++) if (prime[n]) sum += n;
        return sum;
    }

    private static long refPrimeCount(List<Integer> numbers) {

        int largest = 2;
        for (int n : numbers) largest = Math.max(largest, n);
        boolean[] prime = primesUpTo(largest);
        long count = 0;
        for (int n : numbers) if (n >= 2 && prime[n]) count++;
        return count;
    }

    private static long refMax(List<Integer> numbers) {

        long max = Long.MIN_VALUE;
        for (int n : numbers) max = Math.max(max, n);
        return max;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static JobResult submit(JobRequest request) throws Exception {

        return client.submit(request, message -> {
            if (message.startsWith("No coordinator")) System.out.println("    (" + message + ")");
        });
    }

    // Submit several jobs at the same time and return results in the same order
    private static List<JobResult> submitAll(List<JobRequest> requests) throws Exception {

        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, requests.size()));
        List<Future<JobResult>> futures = new ArrayList<>();
        for (JobRequest request : requests) futures.add(pool.submit(() -> submit(request)));

        List<JobResult> results = new ArrayList<>();
        for (Future<JobResult> future : futures) results.add(future.get());
        pool.shutdown();
        return results;
    }

    private static Map<Integer, Integer> readJacs(List<WorkerInfo> workers) throws Exception {

        Map<Integer, Integer> jacs = new TreeMap<>();
        for (WorkerInfo info : workers) jacs.put(info.getId(), ((WorkerService) Naming.lookup(info.getRmiUrl())).getJac());
        return jacs;
    }

    private static List<Integer> randomNumbers(int count, int min, int max) {

        List<Integer> numbers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) numbers.add(min + random.nextInt(max - min + 1));
        return numbers;
    }

    private static void printSplit(JobResult result) {

        System.out.println("    Coordinator Worker " + result.getWorkerId() + " (term " + result.getCoordinatorTerm() + ") split the job into "
                + result.getPartialResults().size() + " part(s):");
        for (JobResult part : result.getPartialResults())
            System.out.printf("      Worker %-3d %-30s -> %-14d %-18s %d ms%n", part.getWorkerId(), part.getDetail(),
                    part.getResult(), part.getThreadName(), part.getDurationMillis());
        System.out.println("      Combined result = " + result.getResult());
    }

    private static void heading(int number, String name) {
        System.out.println();
        System.out.println("========== TEST " + number + ": " + name + " ==========");
    }

    private static void pass(String message) {
        passed++;
        System.out.println("[PASS] " + message);
    }

    private static void fail(String test, String message) {
        failed++;
        System.out.println("[FAIL] " + test + " - " + message);
    }

    private static void error(String test, Exception e) {
        fail(test, "error: " + JobClient.rootMessage(e));
    }

    private static void summary() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println(" PERSON 3 TEST SUMMARY");
        System.out.println(" Passed: " + passed);
        System.out.println(" Failed: " + failed);
        System.out.println("==================================================");
    }
}
