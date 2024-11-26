package com.example.myapp;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class TaskFilter {
    private static final String TAG = "TaskFilter";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final String status;
    private final String priority;
    private final Date startDate;
    private final Date endDate;

    private TaskFilter(Builder builder) {
        this.status = builder.status;
        this.priority = builder.priority;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
    }

    public List<Task> apply(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            Log.d(TAG, "No tasks to filter");
            return new ArrayList<>();
        }

        // Log initial state
        Log.d(TAG, "Initial tasks count: " + tasks.size());
        for (Task task : tasks) {
            Log.d(TAG, String.format("Task before filter - Title: %s, Status: %s, Priority: %s, Due Date: %s",
                    task.getTitle(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate() != null ? DATE_FORMAT.format(task.getDueDate()) : "null"));
        }

        List<Task> filteredTasks = new ArrayList<>(tasks);

        // Apply status filter
        if (!"All".equals(status)) {
            filteredTasks = filteredTasks.stream()
                    .filter(task -> status.equalsIgnoreCase(task.getStatus()))
                    .collect(Collectors.toList());
            Log.d(TAG, "After status filter: " + filteredTasks.size());
        }

        // Apply priority filter
        if (!"All".equals(priority)) {
            filteredTasks = filteredTasks.stream()
                    .filter(task -> priority.equalsIgnoreCase(task.getPriority()))
                    .collect(Collectors.toList());
            Log.d(TAG, "After priority filter: " + filteredTasks.size());
        }

        // Apply date filter
        if (startDate != null || endDate != null) {
            filteredTasks = filteredTasks.stream()
                    .filter(this::isWithinDateRange)
                    .collect(Collectors.toList());
            Log.d(TAG, "After date filter: " + filteredTasks.size());
        }

        // Log final results
        Log.d(TAG, "Final filtered tasks count: " + filteredTasks.size());
        for (Task task : filteredTasks) {
            Log.d(TAG, String.format("Task after filter - Title: %s, Status: %s, Priority: %s, Due Date: %s",
                    task.getTitle(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate() != null ? DATE_FORMAT.format(task.getDueDate()) : "null"));
        }

        return filteredTasks;
    }

    private boolean isWithinDateRange(Task task) {
        Date taskDate = task.getDueDate();

        // If no date range is specified, include all tasks
        if (startDate == null && endDate == null) {
            return true;
        }

        // If task has no due date but date range is specified
        if (taskDate == null) {
            Log.d(TAG, "Task has no due date: " + task.getTitle());
            return false;
        }

        // Convert all dates to Calendar for comparison
        Calendar taskCal = Calendar.getInstance();
        taskCal.setTime(taskDate);
        setToStartOfDay(taskCal);

        boolean withinRange = true;

        if (startDate != null) {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            setToStartOfDay(startCal);
            withinRange = !taskCal.before(startCal);
            Log.d(TAG, String.format("Start date comparison - Task: %s, Start: %s, Within: %b",
                    DATE_FORMAT.format(taskCal.getTime()),
                    DATE_FORMAT.format(startCal.getTime()),
                    withinRange));
        }

        if (withinRange && endDate != null) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endDate);
            setToStartOfDay(endCal);
            withinRange = !taskCal.after(endCal);
            Log.d(TAG, String.format("End date comparison - Task: %s, End: %s, Within: %b",
                    DATE_FORMAT.format(taskCal.getTime()),
                    DATE_FORMAT.format(endCal.getTime()),
                    withinRange));
        }

        return withinRange;
    }

    private void setToStartOfDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public static class Builder {
        private String status = "All";
        private String priority = "All";
        private Date startDate = null;
        private Date endDate = null;

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder setPriority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder setDateRange(Date startDate, Date endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            return this;
        }

        public TaskFilter build() {
            return new TaskFilter(this);
        }
    }
}