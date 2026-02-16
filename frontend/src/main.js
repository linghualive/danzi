import { createApp } from './lib/vue.js';
import { createPinia } from './lib/pinia.js';

import App from './App.js';
import { useAppStore } from './stores/app.js';
import { createAppRouter } from './router/index.js';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const store = useAppStore(pinia);
store.init();

const router = createAppRouter(store);
app.use(router);

app.mount('#app');
