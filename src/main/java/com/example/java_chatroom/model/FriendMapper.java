package com.example.java_chatroom.model;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FriendMapper {
    List<Friend> selectFriendList(int userId);

    List<Friend> selectFriendListBySearchName(String keyword,int currentUserId);

    int addFriend(Integer userId, Integer friendId);

    int checkIsFriend(Integer userId, Integer friendId);
}
