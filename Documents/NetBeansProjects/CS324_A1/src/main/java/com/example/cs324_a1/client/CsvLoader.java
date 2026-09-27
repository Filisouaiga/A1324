/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.client;

import com.example.cs324_a1.jobtype.JobRequest;
import com.example.cs324_a1.jobtype.JobType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Reads job input from CSV files and from text typed into the client.
//
// Number files: any mix of commas, semicolons, spaces and new lines, e.g.
//     value
//     12, 7, 99
//     45;3
// Tokens that are not whole numbers (such as a header row) are skipped and reported.
//
// Batch files: one job per row, the first column being the job type, e.g.
//     MAX,4,19,7,88
//     PRIMESUM,1,1000
//     PRIMECOUNT,2,3,4,5,6,7
// Lines starting with # are comments in both formats.
public final class CsvLoader {

    private static final String SEPARATORS = "[,;\\s]+";

    private CsvLoader() {
    }

    public static NumberData readNumbers(Path file) throws IOException {

        return parseNumbers(readText(file));
    }

    public static NumberData parseNumbers(String text) {

        NumberData data = new NumberData();

        for (String line : text.split("\\R")) {

            if (line.trim().startsWith("#")) {
                continue;
            }

            for (String token : line.split(SEPARATORS)) {
                data.accept(clean(token));
            }
        }

        return data;
    }

    public static BatchData readBatch(Path file, String clientId) throws IOException {

        BatchData batch = new BatchData();
        String[] lines = readText(file).split("\\R");

        for (int i = 0; i < lines.length; i++) {

            String line = lines[i].trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] tokens = line.split(SEPARATORS);
            String rowLabel = "Line " + (i + 1) + ": ";
            JobType type;

            try {
                type = JobType.valueOf(clean(tokens[0]).toUpperCase());
            } catch (IllegalArgumentException e) {
                batch.errors.add(rowLabel + "'" + tokens[0] + "' is not a job type (MAX, PRIMESUM or PRIMECOUNT) - skipped");
                continue;
            }

            NumberData values = new NumberData();

            for (int t = 1; t < tokens.length; t++) {
                values.accept(clean(tokens[t]));
            }

            if (values.getSkippedCount() > 0) {
                batch.errors.add(rowLabel + "not a whole number: " + values.getSkippedSamples() + " - row skipped");
                continue;
            }

            List<Integer> numbers = values.getNumbers();

            if (type == JobType.PRIMESUM) {

                if (numbers.size() != 2) {
                    batch.errors.add(rowLabel + "PRIMESUM needs exactly two values (start,end) - row skipped");
                } else if (numbers.get(0) > numbers.get(1)) {
                    batch.errors.add(rowLabel + "PRIMESUM start is greater than end - row skipped");
                } else {
                    batch.jobs.add(new JobRequest(type, numbers.get(0), numbers.get(1), clientId));
                }

            } else if (numbers.isEmpty()) {
                batch.errors.add(rowLabel + type + " needs at least one number - row skipped");
            } else {
                batch.jobs.add(new JobRequest(type, numbers, clientId));
            }
        }

        return batch;
    }

    // Decoding never fails on odd bytes, and a UTF-8 byte-order mark (added by Excel) is removed
    private static String readText(Path file) throws IOException {

        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);

        return text.startsWith("﻿") ? text.substring(1) : text;
    }

    private static String clean(String token) {

        return token.trim().replace("\"", "").replace("'", "");
    }

    public static class NumberData {

        private final List<Integer> numbers = new ArrayList<>();
        private final List<String> skippedSamples = new ArrayList<>();
        private int skippedCount;

        private void accept(String token) {

            if (token.isEmpty()) {
                return;
            }

            try {
                numbers.add(Integer.parseInt(token));
            } catch (NumberFormatException e) {
                skippedCount++;

                if (skippedSamples.size() < 5) {
                    skippedSamples.add(token);
                }
            }
        }

        public List<Integer> getNumbers() {
            return numbers;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public List<String> getSkippedSamples() {
            return skippedSamples;
        }
    }

    public static class BatchData {

        private final List<JobRequest> jobs = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public List<JobRequest> getJobs() {
            return jobs;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
