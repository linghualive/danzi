export const roomLifecycleActions = {
    navigateToRoom(roomId) {
        if (!roomId) {
            return;
        }
        const path = `/room/${roomId}`;
        if (this._router) {
            this._router.push(path);
        } else {
            window.location.hash = `#${path}`;
        }
    },

    async loadMyRoom() {
        try {
            const result = await this.api('GET', '/api/live/room/my');
            if (result.code === 200 && result.data) {
                return {
                    ...result.data,
                    coverUrl: this.fullMediaUrl(result.data.coverUrl)
                };
            }
        } catch (error) {
            // ignore and fallback
        }
        return null;
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

    async openCreateRoomModal() {
        const myRoom = await this.loadMyRoom();
        if (myRoom?.id) {
            this.createRoomModalVisible = false;
            this.navigateToRoom(myRoom.id);
            return;
        }

        this.newRoomTitle = '';
        this.newRoomCoverFileId = null;
        this.createRoomModalVisible = true;
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
                this.navigateToRoom(result.data.id);
                return true;
            }

            if (result.code === 6003) {
                this.createRoomModalVisible = false;
                this.addToast('需要先通过开播资格审核，正在跳转到申请页面...', 'error');
                setTimeout(() => {
                    window.location.hash = '#/profile';
                }, 500);
                return false;
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
        this.lastRoomStatus = null;
        this.roomEditTitle = '';
        this.roomEditCoverFileId = null;
        this.roomEditCoverUrl = '';
        this.roomEditDirty = false;
        this.roomEditSaving = false;
        this.roomEditModalVisible = false;
        this.productsExpanded = false;
        this.roomWarnings = [];
        this.lastRoomWarningId = 0;
        this.wsMsgType = 'COMMENT';
        this.msgInput = '';
        this.chatMessages = [{ system: true, content: '欢迎来到直播间' }];
        this.wsStatusText = '连接中...';
        this.wsConnected = false;
        this.streamReady = false;
        this.streamError = '';

        await this.loadRoomDetail();
        this.loadRoomOwnerInfo();
        await this.loadRoomWarnings();
        await this.loadHistoryMessages();
        this.connectWebSocket();
        await this.loadRoomProducts();

        this.roomPollTimer = setInterval(() => {
            this.loadRoomDetail();
            this.loadRoomWarnings(true);
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
        this.lastRoomStatus = null;
        this.roomOwnerInfo = null;
        this.ownerFollowedByMe = false;
        this.roomWarnings = [];
        this.lastRoomWarningId = 0;
        this.roomProducts = [];
        this.roomEditTitle = '';
        this.roomEditCoverFileId = null;
        this.roomEditCoverUrl = '';
        this.roomEditDirty = false;
        this.roomEditSaving = false;
        this.roomEditModalVisible = false;
        this.pendingPullUrl = '';
        this.wsStatusText = '未连接';
        this.wsConnected = false;
    },

    async loadRoomDetail() {
        if (!this.currentRoomId) {
            return;
        }

        try {
            const previousStatus = this.currentRoom?.status;
            const result = await this.api('GET', `/api/live/room/${this.currentRoomId}`);
            if (result.code !== 200 || !result.data) {
                return;
            }

            this.currentRoom = {
                ...result.data,
                coverUrl: this.fullMediaUrl(result.data.coverUrl)
            };
            this.syncRoomEditDraft();
            this.lastRoomStatus = this.currentRoom.status;
            this.syncPlayer();

            if (this.isRoomOwner && previousStatus === 1 && this.currentRoom.status === 2 && !this.liveSummaryVisible) {
                this.loadLiveSummary();
            }
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

    syncRoomEditDraft(force = false) {
        if (!this.currentRoom || !this.isRoomOwner) {
            return;
        }

        if (!force && this.roomEditDirty) {
            return;
        }

        this.roomEditTitle = this.currentRoom.title || '';
        this.roomEditCoverFileId = null;
        this.roomEditCoverUrl = this.currentRoom.coverUrl || '';
        this.roomEditDirty = false;
    },

    updateRoomEditTitle(value) {
        this.roomEditTitle = value;
        this.roomEditDirty = true;
    },

    async uploadRoomEditCover(file) {
        if (!file) {
            return;
        }
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
            if (result.code === 200 && result.data?.id) {
                this.roomEditCoverFileId = result.data.id;
                this.roomEditCoverUrl = this.fullMediaUrl(`/api/base/media/public/${result.data.id}`);
                this.roomEditDirty = true;
                this.addToast('封面上传成功', 'success');
            } else {
                this.addToast(result.message || '封面上传失败', 'error');
            }
        } catch (error) {
            this.addToast('封面上传失败', 'error');
        }
    },

    resetRoomEditDraft() {
        this.syncRoomEditDraft(true);
    },

    openRoomEditModal() {
        if (!this.currentRoom || !this.isRoomOwner) {
            return;
        }
        if (this.currentRoom.status === 1 || this.currentRoom.status === 3) {
            return;
        }
        this.syncRoomEditDraft(true);
        this.roomEditModalVisible = true;
    },

    closeRoomEditModal() {
        this.roomEditModalVisible = false;
    },

    async saveRoomBasic() {
        if (!this.currentRoomId || !this.currentRoom || !this.isRoomOwner) {
            return false;
        }
        if (this.currentRoom.status === 1) {
            this.addToast('直播中不可修改直播信息', 'error');
            return false;
        }

        const title = (this.roomEditTitle || '').trim();
        if (!title) {
            this.addToast('请输入直播间标题', 'error');
            return false;
        }

        this.roomEditSaving = true;
        try {
            const body = { title };
            if (this.roomEditCoverFileId) {
                body.coverFileId = this.roomEditCoverFileId;
            }
            const result = await this.api('PUT', `/api/live/room/${this.currentRoomId}/basic`, body);
            if (result.code === 200 && result.data) {
                this.currentRoom = {
                    ...result.data,
                    coverUrl: this.fullMediaUrl(result.data.coverUrl)
                };
                this.syncRoomEditDraft(true);
                this.roomEditModalVisible = false;
                await this.loadRooms();
                this.addToast('直播信息已更新', 'success');
                return true;
            }
            this.addToast(result.message || '保存失败', 'error');
            return false;
        } catch (error) {
            this.addToast('网络错误', 'error');
            return false;
        } finally {
            this.roomEditSaving = false;
        }
    },

    async loadRoomWarnings(silent = false) {
        if (!this.currentRoomId || !this.isRoomOwner) {
            return;
        }

        try {
            const result = await this.api('GET', `/api/live/room/${this.currentRoomId}/warnings?size=20`);
            if (result.code !== 200) {
                if (!silent) {
                    this.roomWarnings = [];
                }
                return;
            }

            const warnings = Array.isArray(result.data) ? result.data : [];
            this.roomWarnings = warnings;

            const latest = warnings[0];
            if (!latest) {
                return;
            }

            if (this.lastRoomWarningId === 0) {
                this.lastRoomWarningId = latest.id;
                return;
            }

            if (latest.id !== this.lastRoomWarningId) {
                this.lastRoomWarningId = latest.id;
                this.addToast(`管理员警告：${latest.message}`, 'error');
            }
        } catch (error) {
            if (!silent) {
                this.roomWarnings = [];
            }
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
                this.loadLiveSummary();
            } else {
                this.addToast(result.message || '停播失败', 'error');
            }
        } catch (error) {
            this.addToast('网络错误', 'error');
        }
    }
};
