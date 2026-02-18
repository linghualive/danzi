export const roomPlayerActions = {
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
    }
};
