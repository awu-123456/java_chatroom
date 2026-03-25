package com.example.java_chatroom.api;

import com.example.java_chatroom.model.Friend;
import com.example.java_chatroom.model.FriendMapper;
import com.example.java_chatroom.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class FriendAPI {
    @Autowired
    private FriendMapper friendMapper;

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
}
