package com.example.java_chatroom.model;

import lombok.Data;

import java.util.List;

@Data
public class MessageSession {
    private int sessionId;
    private List<Friend> friends;
    private String lastMessage;
}
