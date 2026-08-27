(function () {
    "use strict";

    const storageKeys = {
        accessToken: "accessToken",
        refreshToken: "refreshToken",
        currentUser: "currentUser"
    };
    const elements = {
        alertBox: document.getElementById("alertBox"),
        connectionStatus: document.getElementById("connectionStatus"),
        loginCard: document.getElementById("loginCard"),
        sessionCard: document.getElementById("sessionCard"),
        loginForm: document.getElementById("loginForm"),
        loginButton: document.getElementById("loginButton"),
        conversationForm: document.getElementById("conversationForm"),
        receiverId: document.getElementById("receiverId"),
        openConversationButton: document.getElementById("openConversationButton"),
        currentUserName: document.getElementById("currentUserName"),
        currentUserEmail: document.getElementById("currentUserEmail"),
        currentUserId: document.getElementById("currentUserId"),
        logoutButton: document.getElementById("logoutButton"),
        conversationTitle: document.getElementById("conversationTitle"),
        conversationSubtitle: document.getElementById("conversationSubtitle"),
        messageList: document.getElementById("messageList"),
        messageForm: document.getElementById("messageForm"),
        messageContent: document.getElementById("messageContent"),
        sendButton: document.getElementById("sendButton")
    };

    let socket = null;
    let stompBuffer = "";
    let currentUser = null;
    let activeReceiverId = null;

    function getAccessToken() {
        return sessionStorage.getItem(storageKeys.accessToken);
    }

    function getCurrentUser() {
        try {
            const rawUser = sessionStorage.getItem(storageKeys.currentUser);
            return rawUser ? JSON.parse(rawUser) : null;
        } catch (error) {
            return null;
        }
    }

    function showAlert(message) {
        elements.alertBox.textContent = message;
        elements.alertBox.hidden = false;
    }

    function clearAlert() {
        elements.alertBox.textContent = "";
        elements.alertBox.hidden = true;
    }

    function setConnectionStatus(state, label) {
        elements.connectionStatus.className = "connection-status " + state;
        elements.connectionStatus.lastElementChild.textContent = label;
    }

    function setComposerEnabled(enabled) {
        const canSend = enabled && activeReceiverId !== null;
        elements.messageContent.disabled = !canSend;
        elements.sendButton.disabled = !canSend;
    }

    function saveLoginData(loginData) {
        sessionStorage.setItem(storageKeys.accessToken, loginData.accessToken);
        if (loginData.refreshToken) {
            sessionStorage.setItem(storageKeys.refreshToken, loginData.refreshToken);
        }
        sessionStorage.setItem(storageKeys.currentUser, JSON.stringify({
            id: loginData.id,
            name: loginData.name,
            email: loginData.email,
            roles: loginData.roles || []
        }));
    }

    function clearSession() {
        Object.values(storageKeys).forEach(function (key) {
            sessionStorage.removeItem(key);
        });
        currentUser = null;
        activeReceiverId = null;
    }

    function renderSession() {
        const isLoggedIn = Boolean(currentUser && getAccessToken());
        elements.loginCard.hidden = isLoggedIn;
        elements.sessionCard.hidden = !isLoggedIn;
        elements.receiverId.disabled = !isLoggedIn;
        elements.openConversationButton.disabled = !isLoggedIn;

        if (!isLoggedIn) {
            setComposerEnabled(false);
            return;
        }

        elements.currentUserName.textContent = currentUser.name || "Người dùng";
        elements.currentUserEmail.textContent = currentUser.email || "";
        elements.currentUserId.textContent = currentUser.id || "—";
    }

    function websocketUrl() {
        const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
        return protocol + "//" + window.location.host + "/ws";
    }

    function escapeHeaderValue(value) {
        return String(value).replace(/\\/g, "\\\\").replace(/\n/g, "\\n").replace(/:/g, "\\c");
    }

    function sendFrame(command, headers, body) {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            throw new Error("Kết nối realtime chưa sẵn sàng.");
        }
        const headerLines = Object.entries(headers || {}).map(function (entry) {
            return escapeHeaderValue(entry[0]) + ":" + escapeHeaderValue(entry[1]);
        });
        socket.send(command + "\n" + headerLines.join("\n") + "\n\n" + (body || "") + "\0");
    }

    function connect() {
        const token = getAccessToken();
        if (!token) {
            return;
        }

        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
            return;
        }

        clearAlert();
        setConnectionStatus("connecting", "Đang kết nối...");
        socket = new WebSocket(websocketUrl());

        socket.onopen = function () {
            sendFrame("CONNECT", {
                "accept-version": "1.2",
                "heart-beat": "10000,10000",
                "Authorization": "Bearer " + token
            });
        };

        socket.onmessage = function (event) {
            stompBuffer += event.data;
            const frames = stompBuffer.split("\0");
            stompBuffer = frames.pop();
            frames.forEach(handleFrame);
        };

        socket.onerror = function () {
            showAlert("Không thể kết nối WebSocket. Hãy kiểm tra server đang chạy và access token còn hiệu lực.");
            setConnectionStatus("error", "Lỗi kết nối");
        };

        socket.onclose = function () {
            setComposerEnabled(false);
            if (getAccessToken()) {
                setConnectionStatus("disconnected", "Đã ngắt kết nối");
            }
        };
    }

    function handleFrame(rawFrame) {
        const frame = rawFrame.replace(/^\n+/, "");
        if (!frame) {
            return;
        }
        const separatorIndex = frame.indexOf("\n\n");
        const headerText = separatorIndex >= 0 ? frame.slice(0, separatorIndex) : frame;
        const body = separatorIndex >= 0 ? frame.slice(separatorIndex + 2) : "";
        const lines = headerText.split("\n");
        const command = lines.shift();

        if (command === "CONNECTED") {
            setConnectionStatus("connected", "Đã kết nối realtime");
            sendFrame("SUBSCRIBE", {
                id: "chat-messages",
                destination: "/user/queue/messages",
                ack: "auto"
            });
            setComposerEnabled(true);
            return;
        }

        if (command === "MESSAGE") {
            try {
                const message = JSON.parse(body);
                if (isMessageForActiveConversation(message)) {
                    appendMessage(message);
                }
            } catch (error) {
                showAlert("Không thể đọc tin nhắn realtime nhận được.");
            }
            return;
        }

        if (command === "ERROR") {
            showAlert(body || "Server từ chối kết nối hoặc tin nhắn.");
            setConnectionStatus("error", "Kết nối bị từ chối");
            setComposerEnabled(false);
        }
    }

    function isMessageForActiveConversation(message) {
        if (!currentUser || activeReceiverId === null) {
            return false;
        }
        return (message.senderId === currentUser.id && message.receiverId === activeReceiverId)
            || (message.senderId === activeReceiverId && message.receiverId === currentUser.id);
    }

    function formatTime(value) {
        if (!value) {
            return "Vừa xong";
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? "Vừa xong" : date.toLocaleTimeString("vi-VN", {
            hour: "2-digit",
            minute: "2-digit"
        });
    }

    function appendMessage(message) {
        if (elements.messageList.querySelector(".message-empty")) {
            elements.messageList.innerHTML = "";
        }
        if (message.id && elements.messageList.querySelector('[data-message-id="' + message.id + '"]')) {
            return;
        }

        const isOwnMessage = currentUser && message.senderId === currentUser.id;
        const row = document.createElement("article");
        row.className = "message-row " + (isOwnMessage ? "own" : "other");
        if (message.id) {
            row.dataset.messageId = message.id;
        }

        const meta = document.createElement("span");
        meta.className = "message-meta";
        meta.textContent = (isOwnMessage ? "Bạn" : (message.senderName || "Người dùng")) + " · " + formatTime(message.createdAt);

        const bubble = document.createElement("div");
        bubble.className = "message-bubble";
        bubble.textContent = message.content;
        row.append(meta, bubble);
        elements.messageList.appendChild(row);
        elements.messageList.scrollTop = elements.messageList.scrollHeight;
    }

    function renderEmptyConversation(message) {
        elements.messageList.innerHTML = "";
        const empty = document.createElement("div");
        empty.className = "message-empty";
        empty.innerHTML = "<strong>Chưa có tin nhắn</strong><span>Hãy gửi lời chào đầu tiên để bắt đầu cuộc trò chuyện.</span>";
        if (message) {
            empty.lastElementChild.textContent = message;
        }
        elements.messageList.appendChild(empty);
    }

    async function openConversation(receiverId) {
        if (!currentUser || receiverId === currentUser.id) {
            throw new Error("Hãy nhập ID của một người dùng khác.");
        }

        activeReceiverId = receiverId;
        elements.conversationTitle.textContent = "Hội thoại với user #" + receiverId;
        elements.conversationSubtitle.textContent = "Đang tải lịch sử tin nhắn...";
        renderEmptyConversation("Đang tải lịch sử tin nhắn...");

        const response = await fetch("/api/chats/" + receiverId + "/messages", {
            headers: { Authorization: "Bearer " + getAccessToken() }
        });
        const responseBody = await response.json().catch(function () { return null; });
        if (!response.ok) {
            activeReceiverId = null;
            throw new Error(responseBody && responseBody.message ? responseBody.message : "Không thể tải lịch sử chat.");
        }

        const messages = Array.isArray(responseBody && responseBody.data) ? responseBody.data : [];
        elements.messageList.innerHTML = "";
        if (messages.length === 0) {
            renderEmptyConversation();
        } else {
            messages.forEach(appendMessage);
        }
        elements.conversationSubtitle.textContent = "Tin nhắn mới sẽ xuất hiện ngay khi được gửi.";
        setComposerEnabled(socket && socket.readyState === WebSocket.OPEN);
        elements.messageContent.focus();
    }

    async function login(event) {
        event.preventDefault();
        clearAlert();
        const email = elements.loginForm.email.value.trim();
        const password = elements.loginForm.password.value;
        elements.loginButton.disabled = true;
        elements.loginButton.textContent = "Đang đăng nhập...";

        try {
            const response = await fetch("/api/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email, password: password })
            });
            const responseBody = await response.json().catch(function () { return null; });
            if (!response.ok || !responseBody || !responseBody.data || !responseBody.data.accessToken) {
                throw new Error(responseBody && responseBody.message ? responseBody.message : "Không thể đăng nhập.");
            }
            saveLoginData(responseBody.data);
            currentUser = getCurrentUser();
            renderSession();
            connect();
            elements.loginForm.reset();
        } catch (error) {
            showAlert(error.message || "Không thể đăng nhập.");
        } finally {
            elements.loginButton.disabled = false;
            elements.loginButton.textContent = "Đăng nhập & kết nối";
        }
    }

    function logout() {
        if (socket) {
            socket.close();
            socket = null;
        }
        clearSession();
        renderSession();
        elements.receiverId.value = "";
        elements.conversationTitle.textContent = "Chưa chọn người nhận";
        elements.conversationSubtitle.textContent = "Đăng nhập và nhập ID người nhận để xem lịch sử chat.";
        renderEmptyConversation("Tin nhắn sẽ xuất hiện ở đây sau khi bạn mở một cuộc hội thoại.");
        setConnectionStatus("disconnected", "Chưa kết nối");
        clearAlert();
    }

    elements.loginForm.addEventListener("submit", login);
    elements.conversationForm.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearAlert();
        try {
            await openConversation(Number(elements.receiverId.value));
        } catch (error) {
            showAlert(error.message || "Không thể mở cuộc hội thoại.");
            setComposerEnabled(false);
        }
    });
    elements.messageForm.addEventListener("submit", function (event) {
        event.preventDefault();
        const content = elements.messageContent.value.trim();
        if (!content || activeReceiverId === null) {
            return;
        }
        try {
            sendFrame("SEND", {
                destination: "/app/chat.send",
                "content-type": "application/json"
            }, JSON.stringify({ receiverId: activeReceiverId, content: content }));
            elements.messageContent.value = "";
        } catch (error) {
            showAlert(error.message || "Không thể gửi tin nhắn.");
        }
    });
    elements.logoutButton.addEventListener("click", logout);

    currentUser = getCurrentUser();
    renderSession();
    if (currentUser && getAccessToken()) {
        connect();
    }
}());
