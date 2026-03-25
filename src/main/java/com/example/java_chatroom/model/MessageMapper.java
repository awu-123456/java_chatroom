package com.example.java_chatroom.model;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper {

    String getLastMessageBySessionId(Integer sessionId);

    List<Message> getMessagesBySessionId(Integer sessionId);

    void add(Message message);
}
