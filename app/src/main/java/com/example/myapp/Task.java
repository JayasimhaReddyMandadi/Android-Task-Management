package com.example.myapp;

import java.util.Date;

public class Task {
    private int id;
    private String status;
    private String priority;
    private Date dueDate;
    private String title;
    private String description;

    public Task(int id, String status, String priority, Date dueDate, String title, String description) {
        this.id=id;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.title = title;
        this.description = description;

    }

    // Getters
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Date getDueDate() { return dueDate; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }

    public int getId() {  // Change return type to int
        return id;
    }
}