// ==================== 初始化标签切换 ====================
function initSwitchTab() {
    // 获取元素
    const tabSession = document.querySelector('.tab .tab-session');
    const tabFriend = document.querySelector('.tab .tab-friend');
    const sessionList = document.getElementById('session-list');
    const friendList = document.getElementById('friend-list');
    
    // 调试信息
    console.log('初始化标签切换...');
    console.log('tabSession:', tabSession);
    console.log('tabFriend:', tabFriend);
    console.log('sessionList:', sessionList);
    console.log('friendList:', friendList);
    
    // 检查元素是否存在
    if (!tabSession || !tabFriend || !sessionList || !friendList) {
        console.error('找不到必要的DOM元素！');
        return;
    }
    
    // 设置默认状态：显示会话列表，隐藏好友列表
    sessionList.classList.remove('hide');
    friendList.classList.add('hide');
    tabSession.classList.add('active');
    tabFriend.classList.remove('active');
    
    // 会话列表点击事件
    tabSession.onclick = function(e) {
        e.preventDefault();
        console.log('点击会话列表标签');
        
        // 切换列表显示
        sessionList.classList.remove('hide');
        friendList.classList.add('hide');
        
        // 更新激活样式
        tabSession.classList.add('active');
        tabFriend.classList.remove('active');
        
        // 添加点击动画
        this.style.transform = 'scale(0.95)';
        setTimeout(() => {
            this.style.transform = '';
        }, 150);
    };
    
    // 好友列表点击事件
    tabFriend.onclick = function(e) {
        e.preventDefault();
        console.log('点击好友列表标签');
        
        // 切换列表显示
        sessionList.classList.add('hide');
        friendList.classList.remove('hide');
        
        // 更新激活样式
        tabFriend.classList.add('active');
        tabSession.classList.remove('active');
        
        // 添加点击动画
        this.style.transform = 'scale(0.95)';
        setTimeout(() => {
            this.style.transform = '';
        }, 150);
    };
    
    console.log('标签切换初始化完成');
}


// 确保在 DOM 加载完成后执行
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function() {
        initSwitchTab();
        // 其他初始化可以在这里调用
    });
} else {
    initSwitchTab();
}

/////////////////////////////////////////////////////
// 操作 websocket
/////////////////////////////////////////////////////

// 创建 websocket 实例
let websocket = new WebSocket("ws://127.0.0.1:8080/WebSocketMessage");

websocket.onopen = function() {
    console.log("websocket 连接成功!");
}

websocket.onmessage = function(e) {
    console.log("websocket 收到消息! " + e.data);
    // 此时收到的 e.data 是个 json 字符串, 需要转成 js 对象
    let resp = JSON.parse(e.data);
    
    // 处理不同类型的消息
    if (resp.type == 'message') {
        // 处理消息响应
        handleMessage(resp);
    } else if (resp.type == 'friendRequest') {
        // 处理好友请求
        handleFriendRequest(resp);
    } else if (resp.type == 'friendRequestResponse') {
        // 处理好友请求响应
        handleFriendRequestResponse(resp);
    } else {
        console.log("resp.type 不符合要求!");
    }
}

websocket.onclose = function() {
    console.log("websocket 连接关闭!");
}

websocket.onerror = function() {
    console.log("websocket 连接异常!");
}

function handleMessage(resp) {
    // 把客户端收到的消息, 给展示出来
    let curSessionLi = findSessionLi(resp.sessionId);
    if (curSessionLi == null) {
        curSessionLi = document.createElement('li');
        curSessionLi.setAttribute('message-session-id', resp.sessionId);
        curSessionLi.innerHTML = '<h3>' + resp.fromName + '</h3>'
            + '<p></p>';
        curSessionLi.onclick = function() {
            clickSession(curSessionLi);
        }
    }
    let p = curSessionLi.querySelector('p');
    p.innerHTML = resp.content;
    if (p.innerHTML.length > 10) {
        p.innerHTML = p.innerHTML.substring(0, 10) + '...';
    }
    let sessionListUL = document.querySelector('#session-list');
    sessionListUL.insertBefore(curSessionLi, sessionListUL.children[0]);
    if (curSessionLi.className == 'selected') {
        let messageShowDiv = document.querySelector('.right .message-show');
        addMessage(messageShowDiv, resp);
        scrollBottom(messageShowDiv);
    }
}

function findSessionLi(targetSessionId) {
    let sessionLis = document.querySelectorAll('#session-list li');
    for (let li of sessionLis) {
        let sessionId = li.getAttribute('message-session-id');
        if (sessionId == targetSessionId) {
            return li;
        }
    }
    return null;
}

/////////////////////////////////////////////////////
// 实现消息发送/接收逻辑
/////////////////////////////////////////////////////

function initSendButton() {
    let sendButton = document.querySelector('.right .ctrl button');
    let messageInput = document.querySelector('.right .message-input');
    
    if (sendButton) {
        sendButton.onclick = function() {
            if (!messageInput.value) {
                return;
            }
            let selectedLi = document.querySelector('#session-list .selected');
            if (selectedLi == null) {
                showToast('请先选择一个会话');
                return;
            }
            let sessionId = selectedLi.getAttribute('message-session-id');
            let req = {
                type: 'message',
                sessionId: sessionId,
                content: messageInput.value
            };
            req = JSON.stringify(req);
            console.log("[websocket] send: " + req);
            websocket.send(req);
            messageInput.value = '';
        };
    }
}

// 延迟初始化发送按钮，确保 DOM 已加载
setTimeout(() => {
    initSendButton();
}, 100);

/////////////////////////////////////////////////////
// 从服务器获取到用户登录数据
/////////////////////////////////////////////////////

function getUserInfo() {
    $.ajax({
        type: 'get',
        url: 'userInfo',
        success: function(body) {
            if (body.userId && body.userId > 0) {
                let userDiv = document.querySelector('.main .left .user');
                userDiv.innerHTML = body.username;
                userDiv.setAttribute("user-id", body.userId);
            } else {
                alert("当前用户未登录!");
                location.assign('login.html');
            }
        }
    });
}

getUserInfo();

function getFriendList() {
    $.ajax({
        type: 'get',
        url: 'friendList',
        success: function(body) {
            let friendListUL = document.querySelector('#friend-list');
            friendListUL.innerHTML = '';
            for (let friend of body) {
                let li = document.createElement('li');
                li.innerHTML = '<h4>' + friend.friendName + '</h4>';
                li.setAttribute('friend-id', friend.friendId);
                friendListUL.appendChild(li);
                li.onclick = function() {
                    clickFriend(friend);
                }
            }
        },
        error: function() {
            console.log('获取好友列表失败!');
        }
    });
}

getFriendList();

function getSessionList() {
    $.ajax({
        type: 'get',
        url: 'sessionList',
        success: function(body) {
            let sessionListUL = document.querySelector('#session-list');
            sessionListUL.innerHTML = '';
            for (let session of body) {
                let lastMessage = session.lastMessage || '';
                if (lastMessage.length > 10) {
                    lastMessage = lastMessage.substring(0, 10) + '...';
                }
                let li = document.createElement('li');
                li.setAttribute('message-session-id', session.sessionId);
                li.innerHTML = '<h3>' + session.friends[0].friendName + '</h3>' 
                    + '<p>' + lastMessage + '</p>';
                sessionListUL.appendChild(li);
                li.onclick = function() {
                    clickSession(li);
                }
            }
        }
    });
}

getSessionList();

function clickSession(currentLi) {
    let allLis = document.querySelectorAll('#session-list>li');
    activeSession(allLis, currentLi);
    let sessionId = currentLi.getAttribute("message-session-id");
    getHistoryMessage(sessionId);
}

function activeSession(allLis, currentLi) {
    for (let li of allLis) {
        if (li == currentLi) {
            li.className = 'selected';
        } else {
            li.className = '';
        }
    }
}

function getHistoryMessage(sessionId) {
    console.log("获取历史消息 sessionId=" + sessionId);
    let titleSpan = document.querySelector('.right .title span');
    let messageShowDiv = document.querySelector('.right .message-show');
    messageShowDiv.innerHTML = '';
    
    let selectedH3 = document.querySelector('#session-list .selected>h3');
    if (selectedH3 && titleSpan) {
        titleSpan.innerHTML = selectedH3.innerHTML;
    }
    
    $.ajax({
        type: 'get',
        url: 'message?sessionId=' + sessionId,
        success: function(body) {
            for (let message of body) {
                addMessage(messageShowDiv, message);
            }
            scrollBottom(messageShowDiv);
        }
    });
}

function addMessage(messageShowDiv, message) {
    let messageDiv = document.createElement('div');
    let selfUsername = document.querySelector('.left .user').innerHTML;
    if (selfUsername == message.fromName) {
        messageDiv.className = 'message message-right';
    } else {
        messageDiv.className = 'message message-left';
    }
    messageDiv.innerHTML = '<div class="box">' 
        + '<h4>' + escapeHtml(message.fromName) + '</h4>'
        + '<p>' + escapeHtml(message.content) + '</p>'
        + '</div>';
    messageShowDiv.appendChild(messageDiv);
}

function scrollBottom(elem) {
    let clientHeight = elem.clientHeight;
    let scrollHeight = elem.scrollHeight;
    elem.scrollTop = scrollHeight - clientHeight;
}

function clickFriend(friend) {
    let sessionLi = findSessionByName(friend.friendName);
    let sessionListUL = document.querySelector('#session-list');
    if (sessionLi) {
        sessionListUL.insertBefore(sessionLi, sessionListUL.children[0]);
        sessionLi.click();
    } else {
        sessionLi = document.createElement('li');
        sessionLi.innerHTML = '<h3>' + escapeHtml(friend.friendName) + '</h3>' + '<p></p>';
        sessionListUL.insertBefore(sessionLi, sessionListUL.children[0]);
        sessionLi.onclick = function() {
            clickSession(sessionLi);
        }
        sessionLi.click();
        createSession(friend.friendId, sessionLi);
    }
    let tabSession = document.querySelector('.tab .tab-session');
    if (tabSession) {
        tabSession.click();
    }
}

function findSessionByName(username) {
    let sessionLis = document.querySelectorAll('#session-list>li');
    for (let sessionLi of sessionLis) {
        let h3 = sessionLi.querySelector('h3');
        if (h3 && h3.innerHTML == username) {
            return sessionLi;
        }
    }
    return null;
}

function createSession(friendId, sessionLi) {
    $.ajax({
        type: 'post',
        url: 'session?toUserId=' + friendId,
        success: function(body) {
            console.log("会话创建成功! sessionId = " + body.sessionId);
            sessionLi.setAttribute('message-session-id', body.sessionId);
        }, 
        error: function() {
            console.log('会话创建失败!');
        }
    });
}

// ==================== 添加好友功能 ====================

const addFriendBtn = document.getElementById('addFriendBtn');
const addFriendModal = document.getElementById('addFriendModal');
const closeModalBtn = document.getElementById('closeModalBtn');
const cancelModalBtn = document.getElementById('cancelModalBtn');
const searchUsernameInput = document.getElementById('searchUsername');
const searchResultDiv = document.getElementById('searchResult');

let searchTimer = null;

if (addFriendBtn) {
    addFriendBtn.onclick = function() {
        if (addFriendModal) {
            addFriendModal.style.display = 'flex';
            if (searchUsernameInput) {
                searchUsernameInput.value = '';
            }
            if (searchResultDiv) {
                searchResultDiv.innerHTML = '';
            }
            if (searchUsernameInput) {
                searchUsernameInput.focus();
            }
        }
    };
}

function closeModal() {
    if (addFriendModal) {
        addFriendModal.style.display = 'none';
    }
}

if (closeModalBtn) {
    closeModalBtn.onclick = closeModal;
}
if (cancelModalBtn) {
    cancelModalBtn.onclick = closeModal;
}

if (addFriendModal) {
    addFriendModal.onclick = function(e) {
        if (e.target === addFriendModal) {
            closeModal();
        }
    };
}

if (searchUsernameInput) {
    searchUsernameInput.oninput = function() {
        const keyword = this.value.trim();
        if (searchTimer) {
            clearTimeout(searchTimer);
        }
        if (keyword === '') {
            if (searchResultDiv) {
                searchResultDiv.innerHTML = '';
            }
            return;
        }
        searchTimer = setTimeout(() => {
            searchUsers(keyword);
        }, 500);
    };
}

function searchUsers(keyword) {
    $.ajax({
        type: 'get',
        url: '/searchUsers',
        data: { keyword: keyword },
        dataType: 'json',
        success: function(users) {
            displaySearchResults(users);
        },
        error: function() {
            if (searchResultDiv) {
                searchResultDiv.innerHTML = '<div class="empty-result">搜索失败，请稍后重试</div>';
            }
        }
    });
}

function displaySearchResults(users) {
    if (!users || users.length === 0) {
        if (searchResultDiv) {
            searchResultDiv.innerHTML = '<div class="empty-result">未找到相关用户</div>';
        }
        return;
    }
    
    const currentUserId = document.querySelector('.left .user')?.getAttribute('user-id');
    let html = '';
    for (let user of users) {
        if (user.userId == currentUserId) {
            continue;
        }
        html += `
            <div class="search-result-item" data-user-id="${user.userId}" data-username="${escapeHtml(user.username)}">
                <div class="user-info">
                    <div class="avatar">${escapeHtml(user.username.charAt(0).toUpperCase())}</div>
                    <span class="username">${escapeHtml(user.username)}</span>
                </div>
                <button class="add-user-btn" onclick="sendAddFriendRequest('${user.userId}', '${escapeHtml(user.username)}')">添加</button>
            </div>
        `;
    }
    
    if (html === '') {
        html = '<div class="empty-result">未找到可添加的用户</div>';
    }
    
    if (searchResultDiv) {
        searchResultDiv.innerHTML = html;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

function sendAddFriendRequest(toUserId, toUsername) {
    $.ajax({
        type: 'post',
        url: '/addFriendRequest',
        data: { toUserId: toUserId },
        dataType: 'json',
        success: function(response) {
            if (response.success) {
                showToast(`已向 ${toUsername} 发送好友请求`);
                closeModal();
            } else {
                showToast(response.message || '添加好友失败');
            }
        },
        error: function() {
            showToast('发送请求失败，请稍后重试');
        }
    });
}

function handleFriendRequest(resp) {
    const result = confirm(`${resp.fromUsername} 请求添加您为好友，是否同意？`);
    const response = {
        type: 'friendRequestResponse',
        requestId: resp.requestId,
        fromUserId: resp.fromUserId,
        fromUsername: resp.fromUsername,
        agreed: result
    };
    websocket.send(JSON.stringify(response));
    if (result) {
        showToast(`您已同意添加 ${resp.fromUsername} 为好友`);
        getFriendList();
    } else {
        showToast(`您已拒绝添加 ${resp.fromUsername} 为好友`);
    }
}

function handleFriendRequestResponse(resp) {
    if (resp.agreed) {
        showToast(`${resp.fromUsername} 同意了您的好友请求！`);
        getFriendList();
        getSessionList();
    } else {
        showToast(`${resp.fromUsername} 拒绝了您的好友请求`);
    }
}

function showToast(message) {
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.remove();
    }, 3000);
}

// 支持回车发送消息
document.addEventListener('DOMContentLoaded', function() {
    const messageInput = document.querySelector('.right .message-input');
    if (messageInput) {
        messageInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const sendBtn = document.querySelector('.right .ctrl button');
                if (sendBtn) {
                    sendBtn.click();
                }
            }
        });
    }
});