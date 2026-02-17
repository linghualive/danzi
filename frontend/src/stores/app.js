import { nextTick } from '../lib/vue.js';
import { defineStore } from '../lib/pinia.js';

import { request } from '../services/api.js';
import { shuffle } from '../utils/format.js';

export const useAppStore = defineStore('app', {
    state: () => ({
        apiBase: (() => {
            const port = window.location.port;
            // Production: served by nginx (port 80/443), API proxied through same origin
            if (window.location.protocol !== 'file:' && (!port || port === '80' || port === '443')) {
                return window.location.origin;
            }
            // Dev mode: direct connection to gateway
            return `http://${window.location.hostname || 'localhost'}:9000`;
        })(),
        statusText: ['未开播', '直播中', '已结束', '管理员关闭'],
        orderStatusText: ['待支付', '已支付', '已取消', '退款申请中', '已退款'],

        initialized: false,
        currentUser: null,
        authTab: 'login',

        loginForm: {
            username: '',
            password: ''
        },
        registerForm: {
            username: '',
            password: '',
            nickname: ''
        },
        loginError: '',
        registerError: '',
        loginLoading: false,
        registerLoading: false,

        roomKeyword: '',
        rooms: [],
        roomsLoading: false,

        currentRoomId: null,
        currentRoom: null,
        productsExpanded: false,
        chatMessages: [{ system: true, content: '欢迎来到直播间' }],
        msgInput: '',
        emojiPickerOpen: false,
        emojiCategory: 0,
        wsMsgType: 'COMMENT',
        wsStatusText: '未连接',
        wsConnected: false,
        ws: null,
        wsReconnectTimer: null,
        roomPollTimer: null,

        roomOwnerInfo: null,
        ownerFollowedByMe: false,
        roomProducts: [],
        productsLoading: false,

        flvPlayer: null,
        playingUrl: '',
        pendingPullUrl: '',
        playerStartedAt: 0,
        pullWatchdogTimer: null,
        streamReady: false,
        streamError: '',
        lastStreamErrorAt: 0,
        danmakuTrack: 0,
        roomViewRefs: null,

        orders: [],
        ordersLoading: false,
        soldOrders: [],
        soldOrdersLoading: false,
        refundModalVisible: false,
        refundTargetOrderId: null,
        refundReasonDraft: '',

        createRoomModalVisible: false,
        newRoomTitle: '',
        newRoomCoverFileId: null,

        addProductModalVisible: false,
        editingProductId: null,
        newProduct: { name: '', description: '', price: '', stock: '', imageFileId: null },

        productDetailModalVisible: false,
        currentProduct: null,
        productQty: 1,

        profile: {},
        profileForm: { nickname: '', bio: '', avatarFileId: null },
        followStats: { followingCount: 0, followerCount: 0, followedByMe: false },

        conversations: [],
        currentConversationTargetId: null,
        currentConversationName: '',
        currentConversationMessages: [],
        newPrivateMessage: '',

        adminUsers: [],
        adminRooms: [],

        toasts: []
    }),

    getters: {
        displayNickname(state) {
            if (!state.currentUser) {
                return '';
            }
            return state.currentUser.nickname || state.currentUser.username || '用户';
        },

        filteredRooms(state) {
            return state.rooms;
        },

        currentUserId(state) {
            return state.currentUser?.userId || state.currentUser?.id || 0;
        },

        isAdmin(state) {
            return (state.currentUser?.role || 0) === 2;
        },

        isRoomOwner(state) {
            if (!state.currentUser || !state.currentRoom) {
                return false;
            }
            const uid = state.currentUser.userId || state.currentUser.id;
            return uid === state.currentRoom.userId;
        }
    },

    actions: {
        init() {
            if (this.initialized) {
                return;
            }

            const saved = localStorage.getItem('lc_user');
            if (saved) {
                try {
                    this.currentUser = JSON.parse(saved);
                } catch (error) {
                    localStorage.removeItem('lc_user');
                }
            }

            this.initialized = true;
        },

        async api(method, path, body) {
            return request({
                baseUrl: this.apiBase,
                token: this.currentUser?.token,
                method,
                path,
                body
            });
        },

        addToast(message, type = 'info') {
            const id = Date.now() + Math.random();
            this.toasts.push({ id, message, type });
            setTimeout(() => {
                this.toasts = this.toasts.filter((item) => item.id !== id);
            }, 3000);
        },

        setAuthTab(tab) {
            this.authTab = tab;
            this.loginError = '';
            this.registerError = '';
        },

        updateLoginField({ field, value }) {
            this.loginForm[field] = value;
        },

        updateRegisterField({ field, value }) {
            this.registerForm[field] = value;
        },

        saveUser(data) {
            this.currentUser = data;
            localStorage.setItem('lc_user', JSON.stringify(data));
        },

        logout() {
            this.cleanupRoom();
            this.currentUser = null;
            this.createRoomModalVisible = false;
            this.addProductModalVisible = false;
            this.productDetailModalVisible = false;
            this.closeRefundModal();
            localStorage.removeItem('lc_user');
            this.addToast('已退出登录', 'info');
        },

        async doLogin() {
            this.loginError = '';
            if (!this.loginForm.username || !this.loginForm.password) {
                this.loginError = '请输入用户名和密码';
                return false;
            }

            this.loginLoading = true;
            try {
                const result = await this.api('POST', '/api/user/login', this.loginForm);
                if (result.code === 200 && result.data) {
                    this.saveUser(result.data);
                    this.addToast('登录成功', 'success');
                    return true;
                }

                this.loginError = result.message || '登录失败';
                return false;
            } catch (error) {
                this.loginError = '网络错误，请检查后端服务';
                return false;
            } finally {
                this.loginLoading = false;
            }
        },

        async doRegister() {
            this.registerError = '';
            if (!this.registerForm.username || !this.registerForm.password || !this.registerForm.nickname) {
                this.registerError = '请填写所有字段';
                return false;
            }

            this.registerLoading = true;
            try {
                const registerResult = await this.api('POST', '/api/user/register', this.registerForm);
                if (registerResult.code !== 200) {
                    this.registerError = registerResult.message || '注册失败';
                    return false;
                }

                const loginResult = await this.api('POST', '/api/user/login', {
                    username: this.registerForm.username,
                    password: this.registerForm.password
                });

                if (loginResult.code === 200 && loginResult.data) {
                    this.saveUser(loginResult.data);
                    this.addToast('注册成功，已自动登录', 'success');
                    return true;
                }

                this.registerError = '注册成功但登录失败，请手动登录';
                this.setAuthTab('login');
                return false;
            } catch (error) {
                this.registerError = '网络错误，请检查后端服务';
                return false;
            } finally {
                this.registerLoading = false;
            }
        },

        async loadRooms() {
            this.roomsLoading = true;
            try {
                let path = '/api/live/room/list?size=50';
                if (this.roomKeyword && this.roomKeyword.trim()) {
                    path += `&keyword=${encodeURIComponent(this.roomKeyword.trim())}`;
                }
                const result = await this.api('GET', path);
                if (result.code === 200) {
                    const data = result.data;
                    this.rooms = (Array.isArray(data) ? data : (data?.content || data?.records || [])).map((item) => ({
                        ...item,
                        coverUrl: this.fullMediaUrl(item.coverUrl)
                    }));
                } else {
                    this.rooms = [];
                    this.addToast(result.message || '直播间加载失败', 'error');
                }
            } catch (error) {
                this.rooms = [];
                this.addToast('网络错误，请检查后端服务', 'error');
            } finally {
                this.roomsLoading = false;
            }
        },

        openCreateRoomModal() {
            this.newRoomTitle = '';
            this.newRoomCoverFileId = null;
            this.createRoomModalVisible = true;
        },

        fullMediaUrl(path) {
            if (!path) return '';
            if (/^https?:\/\//.test(path)) return path;
            if (path.startsWith('/')) return `${this.apiBase}${path}`;
            return path;
        },

        async uploadRoomCover(file) {
            try {
                const form = new FormData();
                form.append('file', file);
                const response = await fetch(`${this.apiBase}/api/base/media/upload`, {
                    method: 'POST',
                    headers: {
                        satoken: this.currentUser?.token || ''
                    },
                    body: form
                });
                const result = await response.json();
                if (result.code === 200) {
                    this.newRoomCoverFileId = result.data.id;
                    this.addToast('封面上传成功', 'success');
                } else {
                    this.addToast(result.message || '封面上传失败', 'error');
                }
            } catch (error) {
                this.addToast('封面上传失败', 'error');
            }
        },

        async doCreateRoom() {
            if (!this.newRoomTitle) {
                this.addToast('请输入直播间标题', 'error');
                return false;
            }

            try {
                const body = { title: this.newRoomTitle };
                if (this.newRoomCoverFileId) {
                    body.coverFileId = this.newRoomCoverFileId;
                }
                const result = await this.api('POST', '/api/live/room', body);
                if (result.code === 200 && result.data) {
                    this.createRoomModalVisible = false;
                    this.addToast('直播间已就绪', 'success');
                    await this.loadRooms();
                    window.location.hash = `#/room/${result.data.id}`;
                    return true;
                }

                this.addToast(result.message || '创建失败', 'error');
                return false;
            } catch (error) {
                this.addToast('网络错误', 'error');
                return false;
            }
        },

        async enterRoom(roomId) {
            this.currentRoomId = roomId;
            this.currentRoom = null;
            this.productsExpanded = false;
            this.wsMsgType = 'COMMENT';
            this.msgInput = '';
            this.chatMessages = [{ system: true, content: '欢迎来到直播间' }];
            this.wsStatusText = '连接中...';
            this.wsConnected = false;
            this.streamReady = false;
            this.streamError = '';

            await this.loadRoomDetail();
            this.loadRoomOwnerInfo();
            await this.loadHistoryMessages();
            this.connectWebSocket();
            await this.loadRoomProducts();

            this.roomPollTimer = setInterval(() => {
                this.loadRoomDetail();
                this.loadRoomProducts(true);
            }, 5000);
        },

        onRoomViewReady(refs) {
            this.roomViewRefs = refs;
            if (this.pendingPullUrl) {
                const queuedUrl = this.pendingPullUrl;
                this.pendingPullUrl = '';
                this.startPlayer(queuedUrl);
                return;
            }
            this.syncPlayer();
        },

        cleanupRoom() {
            if (this.roomPollTimer) {
                clearInterval(this.roomPollTimer);
                this.roomPollTimer = null;
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

            this.stopPlayer();
            this.productsExpanded = false;
            this.currentRoomId = null;
            this.currentRoom = null;
            this.roomOwnerInfo = null;
            this.ownerFollowedByMe = false;
            this.roomProducts = [];
            this.pendingPullUrl = '';
            this.wsStatusText = '未连接';
            this.wsConnected = false;
        },

        async loadRoomDetail() {
            if (!this.currentRoomId) {
                return;
            }

            try {
                const result = await this.api('GET', `/api/live/room/${this.currentRoomId}`);
                if (result.code !== 200 || !result.data) {
                    return;
                }

                this.currentRoom = {
                    ...result.data,
                    coverUrl: this.fullMediaUrl(result.data.coverUrl)
                };
                this.syncPlayer();
            } catch (error) {
                console.error('加载房间失败', error);
            }
        },

        async loadRoomOwnerInfo() {
            const userId = this.currentRoom?.userId;
            if (!userId) {
                return;
            }

            try {
                const result = await this.api('GET', `/api/user/profile/${userId}`);
                if (result.code === 200 && result.data) {
                    this.roomOwnerInfo = result.data;
                }
                if (this.currentUserId && userId !== this.currentUserId) {
                    const statsResult = await this.api('GET', `/api/user/follow/${userId}/stats`);
                    if (statsResult.code === 200 && statsResult.data) {
                        this.ownerFollowedByMe = !!statsResult.data.followedByMe;
                    }
                }
            } catch (error) {
                console.warn('加载主播信息失败', error);
            }
        },

        async toggleFollowOwner() {
            const targetId = this.roomOwnerInfo?.userId;
            if (!targetId) return;

            try {
                if (this.ownerFollowedByMe) {
                    const result = await this.api('DELETE', `/api/user/follow/${targetId}`);
                    if (result.code === 200) {
                        this.ownerFollowedByMe = false;
                        this.addToast('已取消关注', 'info');
                    }
                } else {
                    const result = await this.api('POST', `/api/user/follow/${targetId}`);
                    if (result.code === 200) {
                        this.ownerFollowedByMe = true;
                        this.addToast('关注成功', 'success');
                    } else {
                        this.addToast(result.message || '关注失败', 'error');
                    }
                }
            } catch (error) {
                this.addToast('关注操作失败', 'error');
            }
        },

        contactRoomOwner() {
            const targetId = this.roomOwnerInfo?.userId;
            if (!targetId) return;
            this.contactFromOrder(targetId);
        },

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

        async doStartLive() {
            if (!this.currentRoomId) {
                return;
            }
            try {
                const result = await this.api('PUT', `/api/live/room/${this.currentRoomId}/start`);
                if (result.code === 200) {
                    this.addToast('已开播，请在 OBS 开始推流后观看画面', 'success');
                    await this.loadRoomDetail();
                } else {
                    this.addToast(result.message || '开播失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        async doStopLive() {
            if (!this.currentRoomId) {
                return;
            }
            try {
                const result = await this.api('PUT', `/api/live/room/${this.currentRoomId}/stop`);
                if (result.code === 200) {
                    this.addToast('已停播', 'success');
                    await this.loadRoomDetail();
                } else {
                    this.addToast(result.message || '停播失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        syncPlayer() {
            const room = this.currentRoom;
            if (!room || room.status !== 1) {
                this.stopPlayer();
                return;
            }

            const rawPullUrl = room.pullUrl || (room.streamKey ? `http://localhost:8080/live/${room.streamKey}.flv` : '');
            if (!rawPullUrl) {
                this.stopPlayer();
                return;
            }

            const pullUrl = this.normalizePullUrl(rawPullUrl);

            if (!this.roomViewRefs?.videoPlayer) {
                this.pendingPullUrl = pullUrl;
                return;
            }

            if (this.playingUrl === pullUrl && this.flvPlayer) {
                return;
            }

            this.startPlayer(pullUrl);
        },

        normalizePullUrl(url) {
            if (!url) {
                return '';
            }
            try {
                const parsed = new URL(url, window.location.origin);
                const currentHost = window.location.hostname;
                const isLocalHost = parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1';
                const currentIsLocal = currentHost === 'localhost' || currentHost === '127.0.0.1';
                if (isLocalHost && !currentIsLocal && currentHost) {
                    parsed.hostname = currentHost;
                }
                return parsed.toString();
            } catch (error) {
                return url;
            }
        },

        startPlayer(url) {
            if (!this.roomViewRefs?.videoPlayer) {
                this.pendingPullUrl = url;
                return;
            }

            if (window.location.protocol === 'https:' && url.startsWith('http:')) {
                this.streamReady = false;
                this.streamError = '当前页面为 HTTPS，HTTP-FLV 会被浏览器拦截';
                this.addToast('当前页面是 HTTPS，浏览器会拦截 HTTP-FLV，请改用 HTTP 访问页面', 'error');
                return;
            }

            if (!window.flvjs || !window.flvjs.isSupported()) {
                this.streamReady = false;
                this.streamError = '浏览器不支持 FLV 播放，请使用 Chrome/Edge';
                this.addToast('浏览器不支持 FLV 播放，请使用 Chrome/Edge', 'error');
                return;
            }

            this.stopPlayer();
            this.pendingPullUrl = '';
            this.streamReady = false;
            this.streamError = '已开播，等待推流接入...';
            this.playerStartedAt = Date.now();

            const videoEl = this.roomViewRefs.videoPlayer;
            const markReady = () => {
                this.streamReady = true;
                this.streamError = '';
                if (this.pullWatchdogTimer) {
                    clearTimeout(this.pullWatchdogTimer);
                    this.pullWatchdogTimer = null;
                }
            };
            videoEl.addEventListener('loadeddata', markReady, { once: true });
            videoEl.addEventListener('canplay', markReady, { once: true });
            videoEl.addEventListener('playing', markReady, { once: true });

            const player = window.flvjs.createPlayer({
                type: 'flv',
                isLive: true,
                url: url
            }, {
                enableWorker: false,
                enableStashBuffer: false,
                stashInitialSize: 128,
                lazyLoad: false
            });
            try {
                player.attachMediaElement(this.roomViewRefs.videoPlayer);
                player.load();
                player.play().catch(() => {});
                player.on(window.flvjs.Events.ERROR, (errorType, errorDetail) => {
                    this.streamReady = false;
                    this.streamError = '未检测到推流，请检查 OBS 推流地址和推流密钥';
                    const now = Date.now();
                    if (this.currentRoom?.status === 1 && now - this.lastStreamErrorAt > 8000) {
                        this.addToast(`拉流失败: ${errorType || 'UNKNOWN'} ${errorDetail || ''}`.trim(), 'error');
                        this.lastStreamErrorAt = now;
                    }
                    setTimeout(() => {
                        this.stopPlayer();
                        this.loadRoomDetail();
                    }, 3000);
                });
                this.flvPlayer = player;
                this.playingUrl = url;
                this.pullWatchdogTimer = setTimeout(() => {
                    if (!this.streamReady && this.currentRoom?.status === 1 && this.playingUrl === url) {
                        this.stopPlayer();
                        this.startPlayer(url);
                    }
                }, 8000);
            } catch (error) {
                console.error('启动播放器失败', error);
                this.streamError = '播放器初始化失败，请刷新页面重试';
                this.addToast('播放器初始化失败', 'error');
                try {
                    player.destroy();
                } catch (e) {
                    // ignore
                }
            }
        },

        stopPlayer() {
            if (this.pullWatchdogTimer) {
                clearTimeout(this.pullWatchdogTimer);
                this.pullWatchdogTimer = null;
            }

            if (!this.flvPlayer) {
                this.playingUrl = '';
                this.playerStartedAt = 0;
                return;
            }

            try {
                this.flvPlayer.pause();
                this.flvPlayer.unload();
                this.flvPlayer.detachMediaElement();
                this.flvPlayer.destroy();
            } catch (error) {
                console.warn('停止播放器失败', error);
            }

            this.flvPlayer = null;
            this.playingUrl = '';
            this.playerStartedAt = 0;
            this.streamReady = false;
            this.streamError = '';
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
                    this.chatMessages.push(message);
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
        },

        async loadRoomProducts(silent = false) {
            if (!this.currentRoomId) {
                return;
            }

            if (!silent) {
                this.productsLoading = true;
            }

            try {
                const roomResult = await this.api('GET', `/api/product/room/${this.currentRoomId}`);
                if (roomResult.code === 200 && Array.isArray(roomResult.data)) {
                    this.roomProducts = roomResult.data.map((item) => ({
                        ...item,
                        imageUrl: this.fullMediaUrl(item.imageUrl)
                    }));

                    if (this.currentProduct?.id) {
                        const latestProduct = this.roomProducts.find((item) => item.id === this.currentProduct.id);
                        if (latestProduct) {
                            this.currentProduct = {
                                ...this.currentProduct,
                                stock: latestProduct.stock,
                                status: latestProduct.status,
                                imageUrl: latestProduct.imageUrl
                            };
                        }
                    }
                    return;
                }

                if (!silent) {
                    this.roomProducts = [];
                }
            } catch (error) {
                if (!silent) {
                    this.roomProducts = [];
                }
                console.warn('加载直播间商品失败', error);
            } finally {
                if (!silent) {
                    this.productsLoading = false;
                }
            }
        },

        async showProductDetail(product) {
            this.currentProduct = product;
            this.productQty = 1;
            this.productDetailModalVisible = true;

            try {
                const result = await this.api('GET', `/api/product/${product.id}`);
                if (result.code === 200 && result.data) {
                    this.currentProduct = {
                        ...result.data,
                        imageUrl: this.fullMediaUrl(result.data.imageUrl)
                    };
                }
            } catch (error) {
                console.warn('获取商品详情失败', error);
            }
        },

        changeQty(delta) {
            let quantity = Number(this.productQty) || 1;
            quantity = Math.max(1, quantity + delta);
            if (this.currentProduct?.stock !== undefined && this.currentProduct?.stock !== null) {
                quantity = Math.min(quantity, this.currentProduct.stock);
            }
            this.productQty = quantity;
        },

        updateQty(value) {
            let quantity = Number(value) || 1;
            quantity = Math.max(1, quantity);
            if (this.currentProduct?.stock !== undefined && this.currentProduct?.stock !== null) {
                quantity = Math.min(quantity, this.currentProduct.stock);
            }
            this.productQty = quantity;
        },

        async buyFromDetail() {
            if (!this.currentProduct?.id) {
                return;
            }
            await this.createOrder(this.currentProduct.id, this.productQty || 1);
            this.productDetailModalVisible = false;
        },

        async quickBuy(productId) {
            await this.createOrder(productId, 1);
        },

        async createOrder(productId, quantity) {
            try {
                const result = await this.api('POST', '/api/order', {
                    items: [{ productId, quantity }]
                });

                if (result.code === 200) {
                    this.addToast('下单成功', 'success');
                    this.loadRoomProducts(true);
                } else {
                    this.addToast(result.message || '下单失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        async loadOrders() {
            this.ordersLoading = true;
            try {
                const result = await this.api('GET', '/api/order/list');
                if (result.code === 200) {
                    this.orders = (Array.isArray(result.data)
                        ? result.data
                        : (result.data?.records || result.data?.content || [])).map((item) => ({
                        ...item,
                        role: 'buyer'
                    }));
                } else {
                    this.orders = [];
                    this.addToast(result.message || '订单加载失败', 'error');
                }
            } catch (error) {
                this.orders = [];
                this.addToast('网络错误，请检查后端服务', 'error');
            } finally {
                this.ordersLoading = false;
            }
        },

        async payOrder(orderId) {
            try {
                const result = await this.api('PUT', `/api/order/${orderId}/pay`);
                if (result.code === 200) {
                    this.addToast('支付成功', 'success');
                    this.loadOrders();
                } else {
                    this.addToast(result.message || '支付失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        async cancelOrder(orderId) {
            try {
                const result = await this.api('PUT', `/api/order/${orderId}/cancel`);
                if (result.code === 200) {
                    this.addToast('订单已取消', 'info');
                    this.loadOrders();
                } else {
                    this.addToast(result.message || '取消失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        requestRefund(orderId) {
            if (!orderId) {
                return;
            }
            this.refundTargetOrderId = orderId;
            this.refundReasonDraft = '';
            this.refundModalVisible = true;
        },

        closeRefundModal() {
            this.refundModalVisible = false;
            this.refundTargetOrderId = null;
            this.refundReasonDraft = '';
        },

        async submitRefundRequest() {
            const orderId = this.refundTargetOrderId;
            const reason = this.refundReasonDraft.trim();
            if (!orderId) {
                return;
            }
            if (!reason) {
                this.addToast('请输入退款理由', 'error');
                return;
            }
            try {
                const result = await this.api('PUT', `/api/order/${orderId}/refund-request`, { reason });
                if (result.code === 200) {
                    this.closeRefundModal();
                    this.addToast('退款申请已提交', 'success');
                    this.loadOrders();
                } else {
                    this.addToast(result.message || '退款申请失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        async loadSoldOrders() {
            this.soldOrdersLoading = true;
            try {
                const result = await this.api('GET', '/api/order/sold');
                if (result.code === 200) {
                    this.soldOrders = (Array.isArray(result.data)
                        ? result.data
                        : (result.data?.records || result.data?.content || [])).map((item) => ({
                        ...item,
                        role: 'seller'
                    }));
                } else {
                    this.soldOrders = [];
                    this.addToast(result.message || '卖出订单加载失败', 'error');
                }
            } catch (error) {
                this.soldOrders = [];
                this.addToast('网络错误', 'error');
            } finally {
                this.soldOrdersLoading = false;
            }
        },

        async confirmRefund(orderId) {
            if (!orderId) {
                return;
            }
            try {
                const result = await this.api('PUT', `/api/order/${orderId}/refund-confirm`);
                if (result.code === 200) {
                    this.addToast('退款已确认', 'success');
                    this.loadSoldOrders();
                    this.loadOrders();
                } else {
                    this.addToast(result.message || '确认退款失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        },

        contactFromOrder(targetUserId) {
            if (!targetUserId) return;
            window.location.hash = `#/messages?target=${targetUserId}`;
        },

        async copyText(value) {
            const text = String(value ?? '');
            if (!text) {
                return;
            }

            try {
                await navigator.clipboard.writeText(text);
                this.addToast('已复制', 'success');
            } catch (error) {
                this.addToast('复制失败', 'error');
            }
        },

        async loadMyProfile() {
            try {
                const [profileResult, statsResult] = await Promise.all([
                    this.api('GET', '/api/user/profile/me'),
                    this.api('GET', `/api/user/follow/${this.currentUserId}/stats`)
                ]);
                if (profileResult.code === 200 && profileResult.data) {
                    this.profile = profileResult.data;
                    this.profileForm.nickname = profileResult.data.nickname || '';
                    this.profileForm.bio = profileResult.data.bio || '';
                }
                if (statsResult.code === 200 && statsResult.data) {
                    this.followStats = statsResult.data;
                }
            } catch (error) {
                this.addToast('加载个人资料失败', 'error');
            }
        },

        async uploadProfileAvatar(event) {
            const file = event.target.files && event.target.files[0];
            if (!file) return;

            try {
                const form = new FormData();
                form.append('file', file);
                const response = await fetch(`${this.apiBase}/api/base/media/upload`, {
                    method: 'POST',
                    headers: {
                        satoken: this.currentUser?.token || ''
                    },
                    body: form
                });
                const result = await response.json();
                if (result.code === 200) {
                    this.profileForm.avatarFileId = result.data.id;
                    this.addToast('头像上传成功', 'success');
                } else {
                    this.addToast(result.message || '头像上传失败', 'error');
                }
            } catch (error) {
                this.addToast('头像上传失败', 'error');
            }
        },

        async saveMyProfile() {
            try {
                const body = {
                    nickname: this.profileForm.nickname,
                    bio: this.profileForm.bio
                };
                if (this.profileForm.avatarFileId) {
                    body.avatarFileId = this.profileForm.avatarFileId;
                }
                const result = await this.api('PUT', '/api/user/profile/me', body);
                if (result.code === 200 && result.data) {
                    this.profile = result.data;
                    if (this.currentUser) {
                        this.currentUser.nickname = result.data.nickname;
                        localStorage.setItem('lc_user', JSON.stringify(this.currentUser));
                    }
                    this.addToast('资料已保存', 'success');
                } else {
                    this.addToast(result.message || '保存失败', 'error');
                }
            } catch (error) {
                this.addToast('保存失败', 'error');
            }
        },

        async loadConversations() {
            try {
                const result = await this.api('GET', '/api/user/message/conversations');
                if (result.code === 200) {
                    this.conversations = Array.isArray(result.data) ? result.data : [];
                } else {
                    this.conversations = [];
                }
            } catch (error) {
                this.conversations = [];
            }
        },

        async selectConversation(targetUserId) {
            if (!targetUserId) return;
            this.currentConversationTargetId = targetUserId;
            const selected = this.conversations.find((item) => item.targetUserId === targetUserId);
            this.currentConversationName = selected?.targetNickname || selected?.targetUserName || `用户${targetUserId}`;
            try {
                const result = await this.api('GET', `/api/user/message/conversation/${targetUserId}`);
                if (result.code === 200) {
                    this.currentConversationMessages = Array.isArray(result.data) ? result.data : [];
                }
            } catch (error) {
                this.currentConversationMessages = [];
            }
        },

        async sendPrivateMessage() {
            if (!this.currentConversationTargetId || !this.newPrivateMessage.trim()) {
                return;
            }
            try {
                const result = await this.api('POST', '/api/user/message/send', {
                    receiverId: this.currentConversationTargetId,
                    content: this.newPrivateMessage.trim()
                });
                if (result.code === 200) {
                    this.newPrivateMessage = '';
                    await this.selectConversation(this.currentConversationTargetId);
                    await this.loadConversations();
                } else {
                    this.addToast(result.message || '发送失败', 'error');
                }
            } catch (error) {
                this.addToast('发送失败', 'error');
            }
        },

        async loadAdminUsers() {
            if (!this.isAdmin) return;
            try {
                const result = await this.api('GET', '/api/admin/users?size=50');
                if (result.code === 200) {
                    this.adminUsers = result.data?.content || [];
                }
            } catch (error) {
                this.adminUsers = [];
            }
        },

        async updateUserRole(userId, role) {
            try {
                const result = await this.api('PUT', `/api/admin/user/${userId}/role`, { role });
                if (result.code === 200) {
                    this.addToast('角色已更新', 'success');
                    this.loadAdminUsers();
                } else {
                    this.addToast(result.message || '更新失败', 'error');
                }
            } catch (error) {
                this.addToast('更新失败', 'error');
            }
        },

        async loadAdminRooms() {
            if (!this.isAdmin) return;
            try {
                const result = await this.api('GET', '/api/admin/rooms?size=50');
                if (result.code === 200) {
                    this.adminRooms = result.data?.content || [];
                }
            } catch (error) {
                this.adminRooms = [];
            }
        },

        async warnRoom(roomId) {
            const message = window.prompt('请输入警告内容：');
            if (!message) return;
            try {
                const result = await this.api('POST', `/api/admin/room/${roomId}/warn`, { message });
                if (result.code === 200) {
                    this.addToast('警告已发送', 'success');
                } else {
                    this.addToast(result.message || '警告失败', 'error');
                }
            } catch (error) {
                this.addToast('警告失败', 'error');
            }
        },

        async closeRoom(roomId) {
            const reason = window.prompt('请输入关闭原因：');
            if (!reason) return;
            try {
                const result = await this.api('PUT', `/api/admin/room/${roomId}/close`, { reason });
                if (result.code === 200) {
                    this.addToast('直播间已关闭', 'success');
                    this.loadAdminRooms();
                } else {
                    this.addToast(result.message || '关闭失败', 'error');
                }
            } catch (error) {
                this.addToast('关闭失败', 'error');
            }
        },

        openAddProductModal() {
            this.editingProductId = null;
            this.newProduct = { name: '', description: '', price: '', stock: '', imageFileId: null };
            this.addProductModalVisible = true;
        },

        openEditProductModal(product) {
            this.editingProductId = product.id;
            this.newProduct = {
                name: product.name || product.productName || '',
                description: product.description || '',
                price: product.price != null ? String(product.price) : '',
                stock: product.stock != null ? String(product.stock) : '',
                imageFileId: product.imageFileId || null
            };
            this.addProductModalVisible = true;
        },

        updateNewProductField({ field, value }) {
            this.newProduct[field] = value;
        },

        async uploadProductImage(file) {
            try {
                const form = new FormData();
                form.append('file', file);
                const response = await fetch(`${this.apiBase}/api/product/media/upload`, {
                    method: 'POST',
                    headers: {
                        satoken: this.currentUser?.token || ''
                    },
                    body: form
                });
                const result = await response.json();
                if (result.code === 200) {
                    this.newProduct.imageFileId = result.data.id;
                    this.addToast('商品图片上传成功', 'success');
                } else {
                    this.addToast(result.message || '商品图片上传失败', 'error');
                }
            } catch (error) {
                this.addToast('商品图片上传失败', 'error');
            }
        },

        async doSaveProduct() {
            if (!this.newProduct.name) {
                this.addToast('请输入商品名称', 'error');
                return false;
            }
            if (!this.newProduct.price || Number(this.newProduct.price) <= 0) {
                this.addToast('请输入有效价格', 'error');
                return false;
            }
            if (!this.newProduct.stock || Number(this.newProduct.stock) <= 0) {
                this.addToast('请输入有效库存', 'error');
                return false;
            }

            const isEditing = !!this.editingProductId;

            try {
                const body = {
                    name: this.newProduct.name.trim(),
                    price: Number(this.newProduct.price),
                    stock: Number(this.newProduct.stock)
                };
                if (this.newProduct.description) {
                    body.description = this.newProduct.description.trim();
                }
                if (!isEditing) {
                    body.roomId = this.currentRoomId;
                }
                if (this.newProduct.imageFileId) {
                    body.imageFileId = this.newProduct.imageFileId;
                }

                const method = isEditing ? 'PUT' : 'POST';
                const path = isEditing ? `/api/product/${this.editingProductId}` : '/api/product';
                const result = await this.api(method, path, body);
                if (result.code === 200) {
                    this.addProductModalVisible = false;
                    this.addToast(isEditing ? '商品已更新' : '商品添加成功', 'success');
                    await this.loadRoomProducts();
                    return true;
                }

                this.addToast(result.message || (isEditing ? '更新失败' : '添加失败'), 'error');
                return false;
            } catch (error) {
                this.addToast('网络错误', 'error');
                return false;
            }
        },

        async doDeleteProduct(productId) {
            try {
                const result = await this.api('DELETE', `/api/product/${productId}`);
                if (result.code === 200) {
                    this.addToast('商品已删除', 'success');
                    await this.loadRoomProducts();
                } else {
                    this.addToast(result.message || '删除失败', 'error');
                }
            } catch (error) {
                this.addToast('网络错误', 'error');
            }
        }
    }
});
