package com.example.spotiflop;

import androidx.annotation.NonNull;

public class LLMResponse {
    private String action;
    private String subject;

    public LLMResponse(String action, String subject) {
        this.action = action;
        this.subject = subject;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @NonNull
    @Override
    public String toString() {
        return "Action: " + action + ", Subject: " + subject;
    }
}
