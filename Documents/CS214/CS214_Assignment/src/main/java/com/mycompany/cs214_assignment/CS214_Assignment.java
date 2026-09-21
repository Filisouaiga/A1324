/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.cs214_assignment;

/**
 *
 * @author janth
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class CS214_Assignment {

    // Define the exact file name provided in your Moodle assignment prompt
    private static final String CSV_FILE_PATH = "World University Rankings 2023-Cleaned.csv";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n=============================================");
            System.out.println("   CS214 DESIGN & ANALYSIS OF ALGORITHMS     ");
            System.out.println("=============================================");
            System.out.println("Select a sorting option (1-7) or 0 to exit:");
            System.out.println("1. Insertion Sort (Linked List)");
            System.out.println("2. Insertion Sort (Array List)");
            System.out.println("3. Bubble Sort (Linked List)");
            System.out.println("4. Bubble Sort (Array List)");
            System.out.println("5. Merge Sort (Linked List)");
            System.out.println("6. Merge Sort (Array List)");
            System.out.println("7. Java Built-in Sort");
            System.out.println("0. Exit Application");
            System.out.print("Enter your choice: ");

            int choice = -1;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 0 and 7.");
                continue;
            }

            if (choice == 0) {
                System.out.println("Exiting application. Goodbye!");
                break;
            }

            if (choice < 1 || choice > 7) {
                System.out.println("Error: Option must be between 1 and 7.");
                continue;
            }

            // Determine data structure strategy based on user selection
            boolean useLinkedList = (choice == 1 || choice == 3 || choice == 5);
            
            System.out.println("Loading records from CSV dataset file...");
            List<University> targetList = loadDatasetFromCSV(useLinkedList);

            // Handle scenario where file is missing or unreadable
            if (targetList == null || targetList.isEmpty()) {
                System.out.println("Aborting sort process due to data initialization failure.");
                continue;
            }

            Sorter sorter = getSorterImplementation(choice);

            if (sorter != null) {
                System.out.println("Successfully loaded " + targetList.size() + " rows.");
                System.out.println("\n--- First 5 Records (Unsorted) ---");
                printDatasetSample(targetList, 5);

                System.out.println("\nExecuting algorithmic sort routine...");
                long startTime = System.nanoTime();
                sorter.sort(targetList);
                long endTime = System.nanoTime();
                
                double executionTimeMs = (endTime - startTime) / 1_000_000.0;

                System.out.println("\n--- First 5 Records (Sorted by Rank) ---");
                printDatasetSample(targetList, 5);
                System.out.println("---------------------------------------------");
                System.out.println("Total Comparisons Executed: " + sorter.getComparisonCount());
                System.out.printf("Time Taken by Algorithm: %.3f ms\n", executionTimeMs);
                System.out.println("=============================================");
            }
        }
        scanner.close();
    }

    /**
     * CSV Parsing Utility Engine using robust comma isolation matching patterns.
     */
    private static List<University> loadDatasetFromCSV(boolean useLinkedList) {
        List<University> records = useLinkedList ? new LinkedList<>() : new ArrayList<>();
        
        // This splits by commas while ignoring commas inside quotation marks
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        String line;

        try (BufferedReader br = new BufferedReader(new FileReader(CSV_FILE_PATH))) {
            // Read and discard the header row label strings
            String header = br.readLine(); 
            if (header == null) {
                System.out.println("Error: The CSV data file appears to be completely empty.");
                return null;
            }

            while ((line = br.readLine()) != null) {
                // Skip completely blank rows safely
                if (line.trim().isEmpty()) continue; 

                String[] columns = line.split(csvSplitBy, -1);

                // Ensure row has the required minimum field count matching data models
                if (columns.length >= 4) {
                    try {
                        // Clean quotes from strings if wrapped by the exporter
                        int rank = Integer.parseInt(columns[0].replace("\"", "").trim());
                        String name = columns[1].replace("\"", "").trim();
                        String country = columns[2].replace("\"", "").trim();
                        double score = Double.parseDouble(columns[3].replace("\"", "").trim());

                        records.add(new University(rank, name, country, score));
                    } catch (NumberFormatException e) {
                        // Suppresses header errors or faulty text data formatting comfortably
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("CRITICAL ERROR: Could not locate or open '" + CSV_FILE_PATH + "'.");
            System.out.println("Please make sure the file is placed directly inside your main project directory folder.");
            return null;
        }

        return records;
    }

    /**
     * Factory utility to initialize sorting class variations.
     */
    private static Sorter getSorterImplementation(int choice) {
        switch (choice) {
            case 1: return new Insertion_linked_list();
            case 2: return new Insertion_array_list();
            case 3: return new Bubble_linked_list();
            case 4: return new Bubble_array_list();
            case 5: return new Merge_linked_list();
            case 6: return new Merge_array_list();
            case 7: return new Built_in_sort();
            default: return null;
        }
    }

    /**
     * Print a specified maximum number of entries to avoid flooding the console view panel.
     */
    private static void printDatasetSample(List<University> list, int maxEntries) {
        int limit = Math.min(list.size(), maxEntries);
        for (int i = 0; i < limit; i++) {
            System.out.println("  " + list.get(i));
        }
        if (list.size() > maxEntries) {
            System.out.println("  ... and " + (list.size() - maxEntries) + " more entries hiding below.");
        }
    }
}

