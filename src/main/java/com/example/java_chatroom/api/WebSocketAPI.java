package com.example.java_chatroom.api;

import com.example.java_chatroom.component.OnlineUserManager;
import com.example.java_chatroom.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

@Component
public class WebSocketAPI extends TextWebSocketHandler {
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MessageSessionMapper messageSessionMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("[WebSocketAPI] 连接成功!");
        User user = (User) session.getAttributes().get("user");
        if(user == null){
            return;
        }
        onlineUserManager.online(user.getUserId(),session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("[WebSocketAPI] 连接关闭!,status:" + status);
        User user = (User) session.getAttributes().get("user");
        if(user == null){
            return;
        }
        onlineUserManager.offline(user.getUserId(),session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("[WebSocketAPI] 连接异常!,exception:" + exception);
        User user = (User) session.getAttributes().get("user");
        if(user == null){
            return;
        }
        onlineUserManager.offline(user.getUserId(),session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("[WebSocketAPI] 收到消息!,message:" + message.toString());
        User user = (User) session.getAttributes().get("user");
        if(user == null) {
            System.out.println("[WebSocketAPI] 用户未登录 不能进行转发");
            return;
        }
        MessageRequest req = objectMapper.readValue(message.getPayload(),MessageRequest.class);
        if(req.getType().equals("message")) {
            transferMessage(user,req);
        } else {
            System.out.println("[WebSocketAPI] req.type 有误!" + message.getPayload());
        }
    }

    public void transferMessage(User fromUser, MessageRequest req) throws IOException {
        MessageResponse resp = new MessageResponse();
        resp.setType("message");
        resp.setFromId(fromUser.getUserId());
        resp.setFromName(fromUser.getUsername());
        resp.setSessionId(req.getSessionId());
        resp.setContent(req.getContent());
        String respJson = objectMapper.writeValueAsString(resp);
        List<Friend> friends = messageSessionMapper.getFriendIdsBySessionId(req.getSessionId(), fromUser.getUserId());
        Friend myself = new Friend();
        myself.setFriendId(fromUser.getUserId());
        myself.setFriendName(fromUser.getUsername());
        friends.add(myself);
        for(Friend friend : friends) {
            WebSocketSession webSocketSession = onlineUserManager.getSession(friend.getFriendId());
            if(webSocketSession == null) {
                continue;
            }
            webSocketSession.sendMessage(new TextMessage(respJson));
        }
        Message message = new Message();
        message.setSessionId(req.getSessionId());
        message.setFromId(fromUser.getUserId());
        message.setContent(req.getContent());
        messageMapper.add(message);
    }
}
