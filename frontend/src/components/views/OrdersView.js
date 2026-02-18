import { formatPrice } from '../../utils/format.js';

export default {
    name: 'OrdersView',
    props: {
        ordersLoading: {
            type: Boolean,
            default: false
        },
        orders: {
            type: Array,
            default: () => []
        },
        orderStatusText: {
            type: Array,
            default: () => []
        },
        isSellerView: {
            type: Boolean,
            default: false
        },
        keyword: {
            type: String,
            default: ''
        },
        fullMediaUrl: {
            type: Function,
            default: (v) => v || ''
        }
    },
    emits: ['pay-order', 'cancel-order', 'request-refund', 'confirm-refund', 'contact-user', 'update:keyword'],
    data() {
        return {
            nowTick: Date.now(),
            timer: null
        };
    },
    mounted() {
        this.timer = setInterval(() => {
            this.nowTick = Date.now();
        }, 1000);
    },
    beforeUnmount() {
        if (this.timer) {
            clearInterval(this.timer);
            this.timer = null;
        }
    },
    methods: {
        normalizeOrderItems(order) {
            const items = order.items || order.orderItems || [];
            return Array.isArray(items) ? items : [];
        },
        formatPrice,
        formatTime(value) {
            if (!value) return '-';
            const date = new Date(value);
            if (Number.isNaN(date.getTime())) return String(value);
            return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
        },
        remainText(order) {
            if (order.status !== 0 || !order.expireAt) return '';
            const remainMs = new Date(order.expireAt).getTime() - this.nowTick;
            if (remainMs <= 0) return '已超时';
            const remainSec = Math.floor(remainMs / 1000);
            const m = Math.floor(remainSec / 60);
            const s = remainSec % 60;
            return `${m}分${String(s).padStart(2, '0')}秒后超时`;
        }
    },
    template: `
        <div class="orders-page">
            <h2>{{ isSellerView ? '我卖出的订单' : '我的订单' }}</h2>
            <div class="order-search-bar">
                <input
                    class="form-input"
                    :value="keyword"
                    @input="$emit('update:keyword', $event.target.value)"
                    placeholder="搜索订单号/商品名..."
                    style="max-width:320px"
                />
            </div>
            <div v-if="ordersLoading" class="page-loading">
                <span class="loading-spinner"></span> 加载订单...
            </div>
            <div v-else-if="orders.length === 0" class="empty-state">
                <div class="icon">&#128230;</div>
                <p>暂无订单</p>
            </div>
            <div v-else>
                <div class="order-card" v-for="order in orders" :key="order.id">
                    <div class="order-header">
                        <span>订单号: <span class="order-no">{{ order.orderNo || order.id }}</span></span>
                        <span class="order-status" :class="'order-status-' + (order.status ?? 0)">
                            {{ orderStatusText[order.status ?? 0] || '未知' }}
                        </span>
                        <span>下单时间: {{ formatTime(order.createdAt || order.createTime) }}</span>
                    </div>
                    <div class="order-header">
                        <span>买家: {{ order.buyerName || order.buyerId || '-' }}</span>
                        <span>卖家: {{ order.sellerName || order.sellerId || '-' }}</span>
                        <span v-if="order.status === 0">{{ remainText(order) }}</span>
                    </div>
                    <div class="order-body">
                        <div class="order-items">
                            <div
                                class="order-item-row"
                                v-for="item in normalizeOrderItems(order)"
                                :key="item.id || (item.productId + '-' + item.quantity)"
                            >
                                <img v-if="item.productImage" class="order-item-img" :src="fullMediaUrl(item.productImage)" />
                                <span v-else class="order-item-img-placeholder">&#128230;</span>
                                <span class="item-name">{{ item.productName || item.name || '商品' }}</span>
                                <span class="item-qty">x{{ item.quantity || 1 }}</span>
                                <span class="item-price">&yen;{{ formatPrice(item.price || item.amount || 0) }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="order-footer">
                        <div class="order-total">
                            合计: <span>&yen;{{ formatPrice(order.totalAmount || order.amount || 0) }}</span>
                        </div>
                        <div class="order-actions" v-if="order.status === 0">
                            <button v-if="!isSellerView" class="btn btn-success btn-sm" @click="$emit('pay-order', order.id)">支付</button>
                            <button v-if="!isSellerView" class="btn btn-outline btn-sm" @click="$emit('cancel-order', order.id)">取消</button>
                            <button class="btn btn-outline btn-sm" @click="$emit('contact-user', isSellerView ? order.buyerId : order.sellerId)">联系{{ isSellerView ? '买家' : '卖家' }}</button>
                        </div>
                        <div class="order-actions" v-else>
                            <button class="btn btn-outline btn-sm" @click="$emit('contact-user', isSellerView ? order.buyerId : order.sellerId)">联系{{ isSellerView ? '买家' : '卖家' }}</button>
                            <button v-if="!isSellerView && order.status === 1" class="btn btn-danger btn-sm" @click="$emit('request-refund', order.id)">申请退款</button>
                            <button v-if="isSellerView && order.status === 3" class="btn btn-danger btn-sm" @click="$emit('confirm-refund', order.id)">确认退款</button>
                        </div>
                    </div>
                    <div v-if="order.refundReason" class="order-header">
                        <span>退款理由: {{ order.refundReason }}</span>
                    </div>
                </div>
            </div>
        </div>
    `
};
