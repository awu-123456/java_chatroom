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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private UserMapper userMapper;

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
        sendPendingFriendRequests(user.getUserId());
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
        } else if(req.getType().equals("friendRequestResponse")) {
            handleFriendRequestResponse(user,req);
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

    public void sendFriendRequest(Integer toUserId, Integer fromUserId, String fromUsername, Integer requestId) throws IOException {
        MessageResponse resp = new MessageResponse();
        resp.setType("friendRequest");
        resp.setFromId(fromUserId);
        resp.setFromName(fromUsername);
        Map<String,Object> contentMap = new HashMap<>();
        contentMap.put("requestId", requestId);
        resp.setContent(objectMapper.writeValueAsString(contentMap));
        String respJson = objectMapper.writeValueAsString(resp);
        WebSocketSession targetSession  = onlineUserManager.getSession(toUserId);
        if(targetSession  != null) {
            targetSession.sendMessage(new TextMessage(respJson));
            System.out.println("[WebSocketAPI] 发送好友请求给用户 " + toUserId);
        }
    }

    public void handleFriendRequestResponse(User currentUser, MessageRequest req) throws IOException {
        Map<String,Object> contentMap = objectMapper.readValue(req.getContent(),Map.class);
        Integer  requestId = (Integer)contentMap.get("requestId");
        Integer fromUserId = (Integer)contentMap.get("fromUserId");
        Integer agreed = (Integer)contentMap.get("agreed");
        System.out.println("[WebSocketAPI] 收到好友请求响应: requestId=" + requestId +
                ", fromUserId=" + fromUserId +
                ", agreed=" + agreed +
                ", currentUser=" + currentUser.getUserId());
        if(agreed ==1) {
            friendRequestMapper.updateStatus(requestId, 1);
            friendMapper.addFriend(currentUser.getUserId(), fromUserId);
            friendMapper.addFriend(fromUserId, currentUser.getUserId());
            System.out.println("[WebSocketAPI] 用户 " + currentUser.getUserId() + " 同意了 " + fromUserId + " 的好友请求，已添加好友");
        } else {
            friendRequestMapper.updateStatus(requestId, 2);
            System.out.println("[WebSocketAPI] 用户 " + currentUser.getUserId() + " 拒绝了 " + fromUserId + " 的好友请求");
        }
        MessageResponse resp = new MessageResponse();
        resp.setType("friendRequestResponse");
        resp.setFromId(currentUser.getUserId());
        resp.setFromName(currentUser.getUsername());
        Map<String, Object> notifyMap = new HashMap<>();
        notifyMap.put("requestId", requestId);
        notifyMap.put("agreed", agreed);
        resp.setContent(objectMapper.writeValueAsString(notifyMap));
        String respJson = objectMapper.writeValueAsString(resp);
        WebSocketSession fromSession = onlineUserManager.getSession(fromUserId);
        if (fromSession != null) {
            fromSession.sendMessage(new TextMessage(respJson));
            System.out.println("[WebSocketAPI] 发送好友请求响应给用户 " + fromUserId);
        } else {
            System.out.println("[WebSocketAPI] 用户 " + fromUserId + " 不在线，无法发送响应");
        }
    }

    public void sendPendingFriendRequests(Integer userId) throws IOException {
        List<FriendRequest> pendingRequests  = friendRequestMapper.getPendingRequests(userId);
        if(pendingRequests == null) {
            System.out.println("[WebSocketAPI] 用户 " + userId + " 没有未处理的好友请求");
            return;
        }
        System.out.println("[WebSocketAPI] 用户 " + userId + " 有 " + pendingRequests.size() + " 条未处理的好友请求");
        for(FriendRequest friendRequest : pendingRequests) {
            User fromUser = userMapper.selectById(friendRequest.getFromUserId());
            if(fromUser == null) {
                continue;
            }
            MessageResponse resp = new MessageResponse();
            resp.setType("friendRequest");
            resp.setFromId(fromUser.getUserId());
            resp.setFromName(fromUser.getUsername());
            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("requestId",friendRequest.getRequestId());
            resp.setContent(objectMapper.writeValueAsString(contentMap));
            String respJson = objectMapper.writeValueAsString(resp);
            WebSocketSession targetSession  = onlineUserManager.getSession(userId);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage(respJson));
                System.out.println("[WebSocketAPI] 推送未处理好友请求: fromUser=" + fromUser.getUsername());
            }
        }
    }
}
