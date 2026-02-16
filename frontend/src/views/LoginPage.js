import { useRouter } from '../lib/vue-router.js';

import AuthView from '../components/views/AuthView.js';
import { useAppStore } from '../stores/app.js';

export default {
    name: 'LoginPage',
    components: {
        AuthView
    },
    setup() {
        const router = useRouter();
        const store = useAppStore();

        store.cleanupRoom();

        const handleLogin = async () => {
            const ok = await store.doLogin();
            if (ok) {
                router.push('/rooms');
            }
        };

        const handleRegister = async () => {
            const ok = await store.doRegister();
            if (ok) {
                router.push('/rooms');
            }
        };

        return {
            store,
            handleLogin,
            handleRegister
        };
    },
    template: `
        <div class="view active">
            <auth-view
                :auth-tab="store.authTab"
                :login-form="store.loginForm"
                :register-form="store.registerForm"
                :login-error="store.loginError"
                :register-error="store.registerError"
                :login-loading="store.loginLoading"
                :register-loading="store.registerLoading"
                @set-auth-tab="store.setAuthTab"
                @update-login-field="store.updateLoginField"
                @update-register-field="store.updateRegisterField"
                @login="handleLogin"
                @register="handleRegister"
            />
        </div>
    `
};
