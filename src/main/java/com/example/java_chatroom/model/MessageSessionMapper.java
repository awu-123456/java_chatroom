package com.example.java_chatroom.model;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageSessionMapper {

    List<Integer> getSessionIdsByUserId(int userId);

    List<Friend> getFriendIdsBySessionId(int sessionId, int selfUserId);

    int addMessageSession(MessageSession messageSession);

    void addMessageSessionUser(MessageSessionUserItem messageSessionUserItem);
}
