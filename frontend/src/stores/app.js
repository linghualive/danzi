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
        statusText: ['未开播', '直播中', '已结束'],
        orderStatusText: ['待支付', '已支付', '已取消'],

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

        roomFilter: null,
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

        createRoomModalVisible: false,
        newRoomTitle: '',
        newRoomCover: '',

        addProductModalVisible: false,
        editingProductId: null,
        newProduct: { name: '', description: '', price: '', stock: '' },

        productDetailModalVisible: false,
        currentProduct: null,
        productQty: 1,

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

        setRoomFilter(status) {
            this.roomFilter = status;
            this.loadRooms();
        },

        async loadRooms() {
            this.roomsLoading = true;
            try {
                let path = '/api/live/room/list?size=50';
                if (this.roomFilter !== null) {
                    path += `&status=${this.roomFilter}`;
                }
                const result = await this.api('GET', path);
                if (result.code === 200) {
                    const data = result.data;
                    this.rooms = Array.isArray(data) ? data : (data?.content || data?.records || []);
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
            this.newRoomCover = '';
            this.createRoomModalVisible = true;
        },

        async doCreateRoom() {
            if (!this.newRoomTitle) {
                this.addToast('请输入直播间标题', 'error');
                return false;
            }

            try {
                const body = { title: this.newRoomTitle };
                if (this.newRoomCover) {
                    body.cover = this.newRoomCover;
                }
                const result = await this.api('POST', '/api/live/room', body);
                if (result.code === 200) {
                    this.createRoomModalVisible = false;
                    this.addToast('直播间创建成功', 'success');
                    await this.loadRooms();
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
            this.loadRoomProducts();

            this.roomPollTimer = setInterval(() => {
                this.loadRoomDetail();
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

                this.currentRoom = result.data;
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
                const result = await this.api('GET', `/api/user/${userId}`);
                if (result.code === 200 && result.data) {
                    this.roomOwnerInfo = result.data;
                }
            } catch (error) {
                console.warn('加载主播信息失败', error);
            }
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

        async loadRoomProducts() {
            if (!this.currentRoomId) {
                return;
            }

            this.productsLoading = true;
            let products = [];

            try {
                const roomResult = await this.api('GET', `/api/product/room/${this.currentRoomId}`);
                if (roomResult.code === 200 && Array.isArray(roomResult.data) && roomResult.data.length > 0) {
                    products = roomResult.data;
                }
            } catch (error) {
                console.warn('加载直播间商品失败', error);
            }

            if (products.length === 0 && !this.isRoomOwner) {
                try {
                    const result = await this.api('GET', '/api/product/list');
                    if (result.code === 200) {
                        const list = Array.isArray(result.data) ? result.data : (result.data?.records || result.data?.content || []);
                        products = shuffle(list).slice(0, 6);
                    }
                } catch (error) {
                    console.warn('加载商品列表失败', error);
                }
            }

            this.roomProducts = products;
            this.productsLoading = false;
        },

        async showProductDetail(product) {
            this.currentProduct = product;
            this.productQty = 1;
            this.productDetailModalVisible = true;

            try {
                const result = await this.api('GET', `/api/product/${product.id}`);
                if (result.code === 200 && result.data) {
                    this.currentProduct = result.data;
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
                    this.orders = Array.isArray(result.data)
                        ? result.data
                        : (result.data?.records || result.data?.content || []);
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

        openAddProductModal() {
            this.editingProductId = null;
            this.newProduct = { name: '', description: '', price: '', stock: '' };
            this.addProductModalVisible = true;
        },

        openEditProductModal(product) {
            this.editingProductId = product.id;
            this.newProduct = {
                name: product.name || product.productName || '',
                description: product.description || '',
                price: product.price != null ? String(product.price) : '',
                stock: product.stock != null ? String(product.stock) : ''
            };
            this.addProductModalVisible = true;
        },

        updateNewProductField({ field, value }) {
            this.newProduct[field] = value;
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
