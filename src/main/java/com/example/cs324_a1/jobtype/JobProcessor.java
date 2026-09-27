/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.jobtype;

import java.util.List;

// Stateless: one instance is shared by all job threads of a worker,
// which is safe because no fields are read or written while processing.
public class JobProcessor {

    public long process(JobRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Job request cannot be null.");
        }

        switch (request.getJobType()) {

            case MAX:
                return calculateMax(request.getNumbers());

            case PRIMESUM:
                return calculatePrimeSum(request.getStart(), request.getEnd());

            case PRIMECOUNT:
                return calculatePrimeCount(request.getNumbers());

            default:
                throw new IllegalArgumentException("Unsupported job type.");
        }
    }

    // Combine partial results from workers into the final answer:
    // MAX takes the largest partial maximum, PRIMESUM and PRIMECOUNT add the partials.
    public static long combine(JobType type, List<JobResult> partialResults) {

        if (partialResults == null || partialResults.isEmpty()) {
            throw new IllegalArgumentException("No partial results.");
        }

        if (type == JobType.MAX) {

            long maximum = Long.MIN_VALUE;

            for (JobResult result : partialResults) {
                maximum = Math.max(maximum, result.getResult());
            }

            return maximum;
        }

        long total = 0;

        for (JobResult result : partialResults) {
            total += result.getResult();
        }

        return total;
    }

    private long calculateMax(List<Integer> numbers) {

        if (numbers == null || numbers.isEmpty()) {

            throw new IllegalArgumentException("MAX requires at least one number.");
        }

        int max = numbers.get(0);

        for (int number : numbers) {

            if (number > max) {
                max = number;
            }
        }

        return max;
    }

    private long calculatePrimeSum(int start, int end) {

        if (start > end) {
            throw new IllegalArgumentException("Start cannot be greater than end.");
        }

        long sum = 0;

        // long loop variable: an int would overflow and never stop when end == Integer.MAX_VALUE
        for (long number = start; number <= end; number++) {

            if (isPrime((int) number)) {
                sum += number;
            }
        }

        return sum;
    }

    private long calculatePrimeCount(
            List<Integer> numbers) {

        if (numbers == null) {
            return 0;
        }

        long count = 0;

        for (int number : numbers) {

            if (isPrime(number)) {
                count++;
            }
        }

        return count;
    }

    private boolean isPrime(int number) {

        if (number < 2) {
            return false;
        }

        if (number == 2) {
            return true;
        }

        if (number % 2 == 0) {
            return false;
        }

        for (int i = 3; (long) i * i <= number; i += 2) {

            if (number % i == 0) {
                return false;
            }
        }

        return true;
    }
}
