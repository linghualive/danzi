import { defineStore } from '../lib/pinia.js';

import { createAppState } from './app/state.js';
import { appGetters } from './app/getters.js';
import { baseActions } from './app/actions/baseActions.js';
import { authActions } from './app/actions/authActions.js';
import { roomActions } from './app/actions/roomActions.js';
import { orderActions } from './app/actions/orderActions.js';
import { profileSocialActions } from './app/actions/profileSocialActions.js';
import { adminProductQualificationActions } from './app/actions/adminProductQualificationActions.js';

export const useAppStore = defineStore('app', {
    state: createAppState,
    getters: appGetters,
    actions: {
        ...baseActions,
        ...authActions,
        ...roomActions,
        ...orderActions,
        ...profileSocialActions,
        ...adminProductQualificationActions
    }
});
