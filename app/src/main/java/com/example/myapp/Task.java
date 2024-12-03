package com.example.myapp;

import java.util.Date;

public class Task {
    private int id;
    private String status;
    private String priority;
    private Date dueDate;
    private String title;
    private String description;
    private Date createdAt;
    private String user;

    // Default constructor
    public Task() {
    }

    // Constructor for basic task creation
    public Task(int id, String status, String priority, Date dueDate, String title, String description) {
        this.id = id;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.title = title;
        this.description = description;
    }

    // Full constructor with all fields
    public Task(int id, String title, String description, String status, String priority,
                Date dueDate, Date createdAt, String user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.user = user;
    }

    // Getters
    public int getId() { return id; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Date getDueDate() { return dueDate; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Date getCreatedAt() { return createdAt; }
    public String getUser() { return user; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUser(String user) { this.user = user; }

    // Builder pattern
    public static class Builder {
        private int id;
        private String status;
        private String priority;
        private Date dueDate;
        private String title;
        private String description;
        private Date createdAt;
        private String user;

        public Builder() {
        }

        public Builder id(int id) {
            this.id = id;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder dueDate(Date dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Task build() {
            return new Task(id, title, description, status, priority, dueDate, createdAt, user);
        }
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", deadline=" + dueDate +
                ", created_at=" + createdAt +
                ", user='" + user + '\'' +
                '}';
    }
}