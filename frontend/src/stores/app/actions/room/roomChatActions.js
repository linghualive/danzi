import { nextTick } from '../../../../lib/vue.js';

export const roomChatActions = {
    async loadHistoryMessages() {
        if (!this.currentRoomId) {
            return;
        }

        try {
            const result = await this.api('GET', `/api/live/room/${this.currentRoomId}/messages?page=0&size=50`);
            const list = result?.data?.content || [];
            if (result.code === 200 && list.length > 0) {
                this.chatMessages = [
                    { system: true, content: '欢迎来到直播间' },
                    ...list.slice().reverse().map((item) => ({
                        id: item.id,
                        type: item.type,
                        userId: item.userId,
                        nickname: item.nickname,
                        content: item.content,
                        timestamp: item.createdAt
                    }))
                ];
                this.scrollChatToBottom();
            }
        } catch (error) {
            console.error('加载历史消息失败', error);
        }
    },

    connectWebSocket() {
        if (!this.currentRoomId) {
            return;
        }

        if (this.wsReconnectTimer) {
            clearTimeout(this.wsReconnectTimer);
            this.wsReconnectTimer = null;
        }

        if (this.ws) {
            this.ws.onclose = null;
            this.ws.close();
            this.ws = null;
        }

        const wsBase = this.apiBase.replace(/^http/, 'ws');
        const wsUrl = `${wsBase}/ws/live/${this.currentRoomId}`;
        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
            this.wsConnected = true;
            this.wsStatusText = '已连接';
        };

        this.ws.onclose = () => {
            this.wsConnected = false;
            this.wsStatusText = '已断开，3秒后重连...';
            if (this.currentRoomId) {
                this.wsReconnectTimer = setTimeout(() => {
                    this.connectWebSocket();
                }, 3000);
            }
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);

                if (message.type === 'WARNING' && !this.isRoomOwner) {
                    return;
                }

                if (message.type === 'WARNING' && this.isRoomOwner) {
                    const warningId = Number(message.id || 0);
                    this.roomWarnings = [
                        {
                            id: warningId || Date.now(),
                            roomId: this.currentRoomId,
                            adminId: Number(message.userId || 0),
                            message: message.content || '',
                            createdAt: message.timestamp ? new Date(message.timestamp).toISOString() : ''
                        },
                        ...this.roomWarnings.filter((item) => item.id !== warningId)
                    ];
                    if (warningId) {
                        this.lastRoomWarningId = warningId;
                    }
                }

                this.chatMessages.push(message);
                if (message.type === 'WARNING' && this.isRoomOwner) {
                    this.addToast(`管理员警告：${message.content || ''}`, 'error');
                }
                if (message.type === 'DANMAKU') {
                    this.showDanmaku(message.content);
                }
                this.scrollChatToBottom();
            } catch (error) {
                console.warn('解析消息失败', error);
            }
        };
    },

    async scrollChatToBottom() {
        await nextTick();
        const container = this.roomViewRefs?.chatMessages;
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
        const fsContainer = document.querySelector('.fs-chat-panel-messages');
        if (fsContainer) {
            fsContainer.scrollTop = fsContainer.scrollHeight;
        }
    },

    updateMsgInput(value) {
        this.msgInput = value;
    },

    toggleEmojiPicker() {
        this.emojiPickerOpen = !this.emojiPickerOpen;
    },

    setEmojiCategory(index) {
        this.emojiCategory = index;
    },

    insertEmoji(emoji) {
        this.msgInput += emoji;
        this.emojiPickerOpen = false;
    },

    sendMessage() {
        this.emojiPickerOpen = false;
        if (!this.msgInput || !this.ws || this.ws.readyState !== WebSocket.OPEN) {
            return;
        }

        const payload = {
            type: this.wsMsgType,
            userId: this.currentUser?.userId || this.currentUser?.id || 0,
            nickname: this.displayNickname || '匿名',
            content: this.msgInput,
            roomId: this.currentRoomId
        };

        this.ws.send(JSON.stringify(payload));
        this.msgInput = '';
    },

    sendLike() {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            return;
        }

        const payload = {
            type: 'LIKE',
            userId: this.currentUser?.userId || this.currentUser?.id || 0,
            nickname: this.displayNickname || '匿名',
            content: '点了一个赞',
            roomId: this.currentRoomId
        };

        this.ws.send(JSON.stringify(payload));
    },

    showDanmaku(text) {
        const layer = this.roomViewRefs?.danmakuLayer;
        if (!layer) {
            return;
        }

        const item = document.createElement('div');
        item.className = 'danmaku-item';
        item.textContent = text;
        const trackCount = 8;
        this.danmakuTrack = (this.danmakuTrack + 1) % trackCount;
        item.style.top = `${this.danmakuTrack * 40 + 20}px`;
        const duration = 6 + Math.random() * 3;
        item.style.animationDuration = `${duration}s`;
        item.style.left = '100%';
        layer.appendChild(item);
        setTimeout(() => item.remove(), duration * 1000 + 500);
    }
};
