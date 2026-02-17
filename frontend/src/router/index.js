import { createRouter, createWebHashHistory } from '../lib/vue-router.js';

import LoginPage from '../views/LoginPage.js';
import RoomsPage from '../views/RoomsPage.js';
import RoomPage from '../views/RoomPage.js';
import OrdersPage from '../views/OrdersPage.js';
import SoldOrdersPage from '../views/SoldOrdersPage.js';
import ProfilePage from '../views/ProfilePage.js';
import MessagesPage from '../views/MessagesPage.js';
import AdminPage from '../views/AdminPage.js';

export function createAppRouter(store) {
    const router = createRouter({
        history: createWebHashHistory(),
        routes: [
            {
                path: '/login',
                name: 'login',
                component: LoginPage
            },
            {
                path: '/rooms',
                name: 'rooms',
                component: RoomsPage
            },
            {
                path: '/room/:id',
                name: 'room',
                component: RoomPage
            },
            {
                path: '/orders',
                name: 'orders',
                component: OrdersPage
            },
            {
                path: '/sold-orders',
                name: 'soldOrders',
                component: SoldOrdersPage
            },
            {
                path: '/profile',
                name: 'profile',
                component: ProfilePage
            },
            {
                path: '/messages',
                name: 'messages',
                component: MessagesPage
            },
            {
                path: '/admin',
                name: 'admin',
                component: AdminPage
            },
            {
                path: '/',
                redirect: '/rooms'
            },
            {
                path: '/:pathMatch(.*)*',
                redirect: '/rooms'
            }
        ]
    });

    router.beforeEach((to) => {
        store.init();

        if (!store.currentUser && to.name !== 'login') {
            return { name: 'login' };
        }

        if (store.currentUser && to.name === 'login') {
            return { name: 'rooms' };
        }

        if (to.name === 'admin' && !store.isAdmin) {
            return { name: 'rooms' };
        }

        return true;
    });

    return router;
}
