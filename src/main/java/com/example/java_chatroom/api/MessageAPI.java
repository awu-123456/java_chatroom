package com.example.java_chatroom.api;

import com.example.java_chatroom.model.Message;
import com.example.java_chatroom.model.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class MessageAPI {
    @Autowired
    MessageMapper messageMapper;

    @GetMapping("/message")
    public Object getMessage(Integer sessionId) {
        List<Message> messages = messageMapper.getMessagesBySessionId(sessionId);
        Collections.reverse(messages);
        return messages;
    }
}