package com.example.java_chatroom.model;


import lombok.Data;

import java.util.Date;

@Data
public class FriendRequest {
    private int requestId;
    private int fromUserId;
    private int toUserId;
    private int status;
    private Date createTime;
}
