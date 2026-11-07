package com.teligati.app.network.model;

public class MessageSendNetworkRequest {
    private String content;

    public MessageSendNetworkRequest() {}

    public MessageSendNetworkRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
