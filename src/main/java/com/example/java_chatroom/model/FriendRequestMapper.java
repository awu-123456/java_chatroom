package com.example.java_chatroom.model;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FriendRequestMapper {
    int insertFriendRequest(FriendRequest request);

    int hasPendingRequest(Integer fromUserId,Integer toUserId);

    int updateStatus(Integer requestId,Integer status);

    int deleteRequest(Integer fromUserId,Integer toUserId);

    List<FriendRequest> getPendingRequests(Integer toUserId);
}
