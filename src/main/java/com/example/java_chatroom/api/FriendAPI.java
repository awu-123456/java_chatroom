package com.example.java_chatroom.api;

import com.example.java_chatroom.model.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FriendAPI {
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private FriendRequestMapper friendRequestMapper;
    @Autowired
    WebSocketAPI webSocketAPI = new WebSocketAPI();

    @GetMapping("/friendList")
    @ResponseBody
    public Object getFriendList(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if(session == null) {
            System.out.println(" [getFriendList] session 不存在");
            return new ArrayList<Friend>();
        }
        User user = (User) session.getAttribute("user");
        if(user == null) {
            System.out.println(" [getFriendList ] user 不存在");
            return new ArrayList<Friend>();
        }
        List<Friend> friendList = friendMapper.selectFriendList(user.getUserId());
        return friendList;
    }

    @GetMapping("/searchUsers")
    @ResponseBody
    public Object getSearchUsers(String keyword, HttpServletRequest req) {
        if(keyword == null || keyword.equals("")) {
            System.out.println("[getSearchUsers ] keyword 为空");
            return new ArrayList<Friend>();
        }
        HttpSession session = req.getSession(false);
        if(session == null) {
            System.out.println("[getSearchUsers] session 不存在");
            return new ArrayList<Friend>();
        }
        User user = (User) session.getAttribute("user");
        if(user == null) {
            System.out.println("[getSearchUsers] user 不存在");
            return new ArrayList<Friend>();
        }
        List<Friend> searchUserList = friendMapper.selectFriendListBySearchName(keyword, user.getUserId());
        return searchUserList;
    }

    @PostMapping("/addFriendRequest")
    @ResponseBody
    public Map<String, Object> addFriendRequest(Integer toUserId, HttpServletRequest req) throws IOException {
        Map<String, Object> result  = new HashMap<>();
        HttpSession session = req.getSession(false);
        if(session == null) {
            System.out.println("[addFriendRequest] session 不存在");
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        User user = (User) session.getAttribute("user");
        if(user == null) {
            System.out.println("[addFriendRequest] user 不存在");
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }
        Integer fromUserId = user.getUserId();
        int isFriend = friendMapper.checkIsFriend(fromUserId, toUserId);
        if(isFriend > 0) {
            result.put("success", false);
            result.put("message", "已经是好友了");
            return result;
        }
        friendRequestMapper.deleteRequest(fromUserId, toUserId);
        friendRequestMapper.deleteRequest(toUserId, fromUserId);
        FriendRequest  friendRequest = new FriendRequest();
        friendRequest.setFromUserId(fromUserId);
        friendRequest.setToUserId(toUserId);
        friendRequest.setStatus(0);
        friendRequestMapper.insertFriendRequest(friendRequest);
        webSocketAPI.sendFriendRequest(toUserId,fromUserId, user.getUsername(), friendRequest.getRequestId());
        result.put("success", true);
        result.put("message", "好友请求已发送");
        return result;
    }
}
