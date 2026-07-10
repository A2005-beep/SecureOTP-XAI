package com.example.secureotp_xai.model;

public class AppModel {

    private String name;
    private String risk;
    private String reason;

    public AppModel(String name, String risk, String reason) {
        this.name = name;
        this.risk = risk;
        this.reason = reason;
    }

    public String getName() { return name; }
    public String getRisk() { return risk; }
    public String getReason() { return reason; }
}