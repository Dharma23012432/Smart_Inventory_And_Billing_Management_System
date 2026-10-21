package com.smartinventory.notificationservice.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Standard uniform API response envelope for error scenarios.
 */
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
    private List<ErrorDetail> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.details = Collections.emptyList();
    }

    public ErrorResponse(int status, String error, String message, String path, List<ErrorDetail> details) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
        this.details = details != null ? details : Collections.emptyList();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ErrorDetail> details) {
        this.details = details;
    }
}
