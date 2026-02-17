import { onMounted } from '../lib/vue.js';

import { useAppStore } from '../stores/app.js';
import OrdersView from '../components/views/OrdersView.js';

export default {
    name: 'SoldOrdersPage',
    components: {
        OrdersView
    },
    setup() {
        const store = useAppStore();

        onMounted(() => {
            store.cleanupRoom();
            store.loadSoldOrders();
        });

        return {
            store
        };
    },
    template: `
        <div class="view active">
            <orders-view
                :orders-loading="store.soldOrdersLoading"
                :orders="store.soldOrders"
                :order-status-text="store.orderStatusText"
                :is-seller-view="true"
                @confirm-refund="store.confirmRefund"
                @contact-user="store.contactFromOrder"
            />
        </div>
    `
};
