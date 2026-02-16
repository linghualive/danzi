import { createRouter, createWebHashHistory } from '../lib/vue-router.js';

import LoginPage from '../views/LoginPage.js';
import RoomsPage from '../views/RoomsPage.js';
import RoomPage from '../views/RoomPage.js';
import OrdersPage from '../views/OrdersPage.js';

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

        return true;
    });

    return router;
}
