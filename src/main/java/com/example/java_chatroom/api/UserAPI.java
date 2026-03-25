package com.example.java_chatroom.api;

import com.example.java_chatroom.model.User;
import com.example.java_chatroom.model.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserAPI {
    @Resource
    UserMapper userMapper;

    @PostMapping("/login")
    @ResponseBody
    public Object login(String username, String password, HttpServletRequest req) {
        // 1. 先去数据库中查看，username 能否找到对应的 user 对象
        //    如果能找到则看密码是否匹配
        User user = userMapper.selectByName(username);
        if(user == null || !user.getPassword().equals(password)) {
            System.out.println("登录失败！用户名或密码错误"+user);
            return new User();
        }
        // 2. 如果都匹配，登录成功! 创建会话！！
        HttpSession session = req.getSession(true);
        session.setAttribute("user", user);
        user.setPassword("");
        return user;
    }

    @PostMapping("register")
    @ResponseBody
    public Object register(String username,String password){
        User user = null;
        try {
            user = new User();
            user.setUsername(username);
            user.setPassword(password);
            int ret = userMapper.insert(user);
            System.out.println("注册成功："+ret);
            user.setPassword("");
        } catch (DuplicateKeyException e) {
            System.out.println("注册失败！username="+username);
            user =new User();
        }
        return user;
    }

    @GetMapping("/userInfo")
    @ResponseBody
    public Object getUserInfo(HttpServletRequest req) {
        // 1. 先从请求中获取到会话
        HttpSession session = req.getSession(false);
        if(session == null) {
            // 会话不存在，用户尚未登录，此时返回一个空对象
            System.out.println("[getUserInfo] 当前获取不到 session 对象!");
            return new User();
        }
        User user = (User) session.getAttribute("user");
        if(user == null) {
            System.out.println("[getUserInfo] 当前获取不到 User");
            return new User();
        }
        user.setPassword("");
        return user;
    }
}
