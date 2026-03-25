package com.example.java_chatroom.api;

import com.example.java_chatroom.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MessageSessionAPI {
    @Autowired
    private MessageSessionMapper messageSessionMapper;

    @Autowired
    private MessageMapper messageMapper;

    @GetMapping("/sessionList")
    @ResponseBody
    public Object getMessageSessionList(HttpServletRequest req) {
        List<MessageSession>  messageSessionList = new ArrayList<>();
        HttpSession session = req.getSession(false);
        if(session == null) {
            System.out.println(" [getMessageSessionList] session 不存在");
            return messageSessionList;
        }
        User user = (User)session.getAttribute("user");
        if(user == null) {
            System.out.println(" [getMessageSessionList] user 不存在");
            return messageSessionList;
        }
        List<Integer> sessionIdList = messageSessionMapper.getSessionIdsByUserId(user.getUserId());
        for(int sessionId : sessionIdList) {
            MessageSession messageSession = new MessageSession();
            messageSession.setSessionId(sessionId);
            List<Friend> friends = messageSessionMapper.getFriendIdsBySessionId(sessionId, user.getUserId());
            messageSession.setFriends(friends);
            String lastMessage = messageMapper.getLastMessageBySessionId(sessionId);
            if(lastMessage == null) {
                messageSession.setLastMessage("");
            } else {
                messageSession.setLastMessage(lastMessage);
            }
            messageSessionList.add(messageSession);
        }
        return messageSessionList;
    }

    @PostMapping("/session")
    @ResponseBody
    @Transactional
    public Object addMessageSession(Integer toUserId, @SessionAttribute("user") User user) {
        Map<String,Integer> resp = new HashMap<>();

        MessageSession messageSession = new MessageSession();
        messageSessionMapper.addMessageSession(messageSession);

        MessageSessionUserItem item1 = new MessageSessionUserItem();
        item1.setSessionId(messageSession.getSessionId());
        item1.setUserId(user.getUserId());
        messageSessionMapper.addMessageSessionUser(item1);

        MessageSessionUserItem item2 = new MessageSessionUserItem();
        item2.setSessionId(messageSession.getSessionId());
        item2.setUserId(toUserId);
        messageSessionMapper.addMessageSessionUser(item2);

        System.out.println(" [addMessageSession] 新增会话成功,sessionId = " + messageSession.getSessionId() +
        "userId1 = " + user.getUserId() + "userID2 = " + toUserId);
        resp.put("sessionId", messageSession.getSessionId());
        return resp;
    }
}
