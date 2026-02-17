import { onMounted } from '../lib/vue.js';

import { useAppStore } from '../stores/app.js';

export default {
    name: 'AdminPage',
    setup() {
        const store = useAppStore();

        onMounted(() => {
            store.cleanupRoom();
            store.loadAdminUsers();
            store.loadAdminRooms();
        });

        return {
            store
        };
    },
    template: `
        <div class="view active admin-page">
            <div class="page-heading">
                <h2>管理后台</h2>
                <p>管理用户角色、处理直播间违规并进行风险控制。</p>
            </div>
            <div class="admin-grid">
                <div class="admin-panel">
                    <h3>用户管理</h3>
                    <div v-if="store.adminUsers.length === 0" class="admin-empty">暂无用户数据</div>
                    <div v-for="u in store.adminUsers" :key="u.id" class="admin-row">
                        <span class="admin-row-title">{{ u.id }} - {{ u.username }} ({{ u.nickname }})</span>
                        <select class="admin-role-select" :value="u.role" @change="store.updateUserRole(u.id, Number($event.target.value))">
                            <option :value="0">用户</option>
                            <option :value="1">主播</option>
                            <option :value="2">管理员</option>
                        </select>
                    </div>
                </div>
                <div class="admin-panel">
                    <h3>直播间管理</h3>
                    <div v-if="store.adminRooms.length === 0" class="admin-empty">暂无直播间数据</div>
                    <div v-for="r in store.adminRooms" :key="r.id" class="admin-row">
                        <span class="admin-row-title">#{{ r.id }} {{ r.title }} (状态: {{ store.statusText[r.status] || r.status }})</span>
                        <button class="btn btn-outline btn-sm" @click="store.warnRoom(r.id)">警告</button>
                        <button class="btn btn-danger btn-sm" @click="store.closeRoom(r.id)">关闭</button>
                    </div>
                </div>
            </div>
        </div>
    `
};
