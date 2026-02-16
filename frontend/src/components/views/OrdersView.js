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
        }
    },
    emits: ['pay-order', 'cancel-order'],
    methods: {
        normalizeOrderItems(order) {
            const items = order.items || order.orderItems || [];
            return Array.isArray(items) ? items : [];
        },
        formatPrice
    },
    template: `
        <div class="orders-page">
            <h2>我的订单</h2>
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
                        <span v-if="order.createTime">{{ order.createTime }}</span>
                    </div>
                    <div class="order-body">
                        <div class="order-items">
                            <div
                                class="order-item-row"
                                v-for="item in normalizeOrderItems(order)"
                                :key="item.id || (item.productId + '-' + item.quantity)"
                            >
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
                            <button class="btn btn-success btn-sm" @click="$emit('pay-order', order.id)">支付</button>
                            <button class="btn btn-outline btn-sm" @click="$emit('cancel-order', order.id)">取消</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `
};
