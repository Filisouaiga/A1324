/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.example.cs324_a1.coordinator;

import java.io.Serializable;
import java.util.UUID;

public class TermEndMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final int coordinatorId;
    private final int term;

    public TermEndMessage(int coordinatorId, int term) {

        this.messageId = UUID.randomUUID().toString();
        this.coordinatorId = coordinatorId;
        this.term = term;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getCoordinatorId() {
        return coordinatorId;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public String toString() {

        return "TermEndMessage{" + "coordinatorId=" + coordinatorId + '}';
    }
}
