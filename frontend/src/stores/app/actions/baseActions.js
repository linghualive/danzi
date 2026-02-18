import { request } from '../../../services/api.js';

export const baseActions = {
    setRouter(router) {
        this._router = router;
    },

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

        if (this.currentUser?.token) {
            this.refreshCurrentUserProfile();
        }
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

    updateCurrentUserProfile(profile) {
        if (!this.currentUser || !profile) {
            return;
        }

        const nextUser = {
            ...this.currentUser,
            nickname: profile.nickname ?? this.currentUser.nickname,
            role: profile.role ?? this.currentUser.role
        };

        this.currentUser = nextUser;
        localStorage.setItem('lc_user', JSON.stringify(nextUser));
    },

    async refreshCurrentUserProfile() {
        if (!this.currentUser?.token) {
            return;
        }

        try {
            const result = await this.api('GET', '/api/user/profile/me');
            if (result.code === 200 && result.data) {
                this.updateCurrentUserProfile(result.data);
            }
        } catch (error) {
            // keep cached user when network is unavailable
        }
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

    fullMediaUrl(path) {
        if (!path) return '';
        if (/^https?:\/\//.test(path)) return path;
        if (path.startsWith('/')) return `${this.apiBase}${path}`;
        return path;
    },

    contactFromOrder(targetUserId) {
        if (!targetUserId && targetUserId !== 0) return;
        if (this._router) {
            this._router.push({ path: '/messages', query: { target: targetUserId } });
        } else {
            window.location.hash = `#/messages?target=${targetUserId}`;
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
    }
};
