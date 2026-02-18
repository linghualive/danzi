export const profileSocialActions = {
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
                this.updateCurrentUserProfile(profileResult.data);
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
                this.updateCurrentUserProfile(result.data);
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

    async showFollowList(type, targetId) {
        this.followListType = type;
        this.followListTargetId = targetId || this.currentUserId;
        this.followListVisible = true;
        await this.loadFollowList();
    },

    async loadFollowList() {
        const targetId = this.followListTargetId || this.currentUserId;
        const endpoint = this.followListType === 'following'
            ? `/api/user/follow/following/${targetId}`
            : `/api/user/follow/followers/${targetId}`;
        try {
            const result = await this.api('GET', endpoint);
            if (result.code === 200) {
                this.followListUsers = (Array.isArray(result.data) ? result.data : []).map((user) => ({
                    ...user,
                    living: !!user.living,
                    liveRoomId: user.liveRoomId || null
                }));
            }
        } catch (error) {
            this.followListUsers = [];
        }
    },

    async toggleFollowInList(userId) {
        const user = this.followListUsers.find((u) => u.userId === userId);
        if (!user) return;
        try {
            if (user.followedByMe) {
                const result = await this.api('DELETE', `/api/user/follow/${userId}`);
                if (result.code === 200) {
                    user.followedByMe = false;
                    this.addToast('已取消关注', 'info');
                }
            } else {
                const result = await this.api('POST', `/api/user/follow/${userId}`);
                if (result.code === 200) {
                    user.followedByMe = true;
                    this.addToast('关注成功', 'success');
                } else {
                    this.addToast(result.message || '关注失败', 'error');
                }
            }
        } catch (error) {
            this.addToast('操作失败', 'error');
        }
    },

    closeFollowList() {
        this.followListVisible = false;
        this.followListUsers = [];
    },

    goToFollowLiveRoom(user) {
        if (!user?.living || !user?.liveRoomId) {
            this.addToast('该主播当前未开播', 'info');
            return;
        }
        this.closeFollowList();
        const path = `/room/${user.liveRoomId}`;
        if (this._router) {
            this._router.push(path);
        } else {
            window.location.hash = `#${path}`;
        }
    }
};
