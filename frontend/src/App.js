import { useRouter } from './lib/vue-router.js';

import ToastContainer from './components/common/ToastContainer.js';
import AppNavbar from './components/layout/AppNavbar.js';
import AddProductModal from './components/modals/AddProductModal.js';
import CreateRoomModal from './components/modals/CreateRoomModal.js';
import ProductDetailModal from './components/modals/ProductDetailModal.js';

import { useAppStore } from './stores/app.js';

export default {
    name: 'App',
    components: {
        ToastContainer,
        AppNavbar,
        AddProductModal,
        CreateRoomModal,
        ProductDetailModal
    },
    setup() {
        const store = useAppStore();
        const router = useRouter();

        const logout = () => {
            store.logout();
            router.push('/login');
        };

        const gotoRooms = () => {
            router.push('/rooms');
        };

        const gotoOrders = () => {
            router.push('/orders');
        };

        return {
            store,
            logout,
            gotoRooms,
            gotoOrders
        };
    },
    template: `
        <div>
            <toast-container :toasts="store.toasts" />

            <app-navbar
                v-if="store.currentUser"
                :display-nickname="store.displayNickname"
                @navigate-rooms="gotoRooms"
                @navigate-orders="gotoOrders"
                @open-create-room="store.openCreateRoomModal"
                @logout="logout"
            />

            <router-view />

            <create-room-modal
                :visible="store.createRoomModalVisible"
                :title="store.newRoomTitle"
                :cover="store.newRoomCover"
                @close="store.createRoomModalVisible = false"
                @update-title="store.newRoomTitle = $event"
                @update-cover="store.newRoomCover = $event"
                @create="store.doCreateRoom"
            />

            <add-product-modal
                :visible="store.addProductModalVisible"
                :product="store.newProduct"
                :editing="!!store.editingProductId"
                @close="store.addProductModalVisible = false"
                @update-field="store.updateNewProductField"
                @create="store.doSaveProduct"
            />

            <product-detail-modal
                :visible="store.productDetailModalVisible"
                :product="store.currentProduct"
                :qty="store.productQty"
                @close="store.productDetailModalVisible = false"
                @change-qty="store.changeQty"
                @update-qty="store.updateQty"
                @buy="store.buyFromDetail"
            />
        </div>
    `
};
