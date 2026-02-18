import { onMounted } from '../lib/vue.js';

import OrdersView from '../components/views/OrdersView.js';
import { useAppStore } from '../stores/app.js';

export default {
    name: 'OrdersPage',
    components: {
        OrdersView
    },
    setup() {
        const store = useAppStore();

        onMounted(() => {
            store.cleanupRoom();
            store.loadOrders();
        });

        return {
            store
        };
    },
    template: `
        <div class="view active">
            <orders-view
                :orders-loading="store.ordersLoading"
                :orders="store.orders"
                :order-status-text="store.orderStatusText"
                :keyword="store.orderKeyword"
                :full-media-url="store.fullMediaUrl"
                @update:keyword="store.searchOrders"
                @pay-order="store.payOrder"
                @cancel-order="store.cancelOrder"
                @request-refund="store.requestRefund"
                @contact-user="store.contactFromOrder"
            />
        </div>
    `
};
