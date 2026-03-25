package com.example.java_chatroom.component;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class OnlineUserManager {
    private ConcurrentMap<Integer,WebSocketSession> sessions = new ConcurrentHashMap<Integer, WebSocketSession>();

    public void online(int userId, WebSocketSession session) {
        if(sessions.get(userId) != null){
            System.out.println("[" + userId + "] 已经被登录，登录失败!");
            return;
        }
        sessions.put(userId, session);
        System.out.println("[" + userId + "] 上线!");
    }

    public void offline(int userId, WebSocketSession session) {
        WebSocketSession existSession = sessions.get(userId);
        if(existSession == session) {
            sessions.remove(userId);
            System.out.println("[" + userId + "] 下线!");
        }
    }

    public WebSocketSession getSession(int userId) {
        return sessions.get(userId);
    }
}
