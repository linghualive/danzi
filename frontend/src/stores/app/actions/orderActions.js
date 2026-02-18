export const orderActions = {
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
            let path = '/api/order/list';
            if (this.orderKeyword && this.orderKeyword.trim()) {
                path += `?keyword=${encodeURIComponent(this.orderKeyword.trim())}`;
            }
            const result = await this.api('GET', path);
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
            let path = '/api/order/sold';
            if (this.soldOrderKeyword && this.soldOrderKeyword.trim()) {
                path += `?keyword=${encodeURIComponent(this.soldOrderKeyword.trim())}`;
            }
            const result = await this.api('GET', path);
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

    searchOrders(keyword) {
        this.orderKeyword = keyword;
        if (this.orderSearchTimer) clearTimeout(this.orderSearchTimer);
        this.orderSearchTimer = setTimeout(() => this.loadOrders(), 400);
    },

    searchSoldOrders(keyword) {
        this.soldOrderKeyword = keyword;
        if (this.orderSearchTimer) clearTimeout(this.orderSearchTimer);
        this.orderSearchTimer = setTimeout(() => this.loadSoldOrders(), 400);
    },

    async loadLiveSummary() {
        if (!this.currentRoom) return;
        try {
            const sellerId = this.currentRoom.userId;
            const from = this.currentRoom.startedAt || '';
            const to = this.currentRoom.stoppedAt || new Date().toISOString();
            if (!from) return;
            const result = await this.api('GET', `/api/order/summary?sellerId=${sellerId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`);
            if (result.code === 200 && result.data) {
                this.liveSummary = result.data;
                this.liveSummaryVisible = true;
            }
        } catch (error) {
            console.warn('加载直播总结失败', error);
        }
    },

    closeLiveSummary() {
        this.liveSummaryVisible = false;
        this.liveSummary = null;
    }
};
