/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.cs214_assignment;

/**
 *
 * @author janth
 */
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Insertion_linked_list extends orter {
    @Override
    public <T extends Comparable<? super T>> void sort(List<T> list) {
        resetCounter();
        if (list.size() < 2) return;

        // Create an empty destination list to store elements in a sorted manner
        List<T> sortedList = new LinkedList<>();

        for (T element : list) {
            ListIterator<T> it = sortedList.listIterator();
            boolean inserted = false;

            while (it.hasNext()) {
                comparisonCount++;
                if (it.next().compareTo(element) > 0) {
                    it.previous(); // Step back to insert before the larger item
                    it.add(element);
                    inserted = true;
                    break;
                }
            }
            // If it is the largest element, append it to the end
            if (!inserted) {
                it.add(element);
            }
        }

        // Copy back the sorted elements into the original list structure
        list.clear();
        list.addAll(sortedList);
    }
    
}
