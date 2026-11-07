package com.teligati.app.network.model;

import java.util.List;

public class ApiError {
    private String code;
    private String message;
    private List<String> details;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
}
