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
            store.loadAdminQualifications();
        });

        return {
            store
        };
    },
    computed: {
        pendingQualCount() {
            return this.store.adminQualifications.filter(q => q.status === 0).length;
        }
    },
    template: `
        <div class="view active admin-page">
            <div class="page-heading">
                <h2>管理后台</h2>
                <p>管理用户角色、处理直播间违规并进行风险控制。</p>
            </div>
            <div class="admin-grid">
                <div class="admin-panel" :style="pendingQualCount > 0 ? 'border:1px solid var(--orange)' : ''">
                    <h3>资质审核 <span v-if="pendingQualCount > 0" style="display:inline-block;background:var(--orange);color:#fff;border-radius:10px;padding:1px 8px;font-size:13px;margin-left:6px">{{ pendingQualCount }} 待审</span></h3>
                    <div v-if="store.adminQualifications.length === 0" class="admin-empty">暂无资质审核申请</div>
                    <div v-for="q in store.adminQualifications" :key="q.id" class="admin-row" style="flex-direction:column;align-items:flex-start;gap:6px">
                        <div><b>用户ID:</b> {{ q.userId }} | <b>联系方式:</b> {{ q.contactInfo }} | <b>营业执照:</b> {{ q.businessLicense }}</div>
                        <div><b>个人信息:</b> {{ q.personalInfo }}</div>
                        <div v-if="q.status === 0" style="display:flex;gap:8px">
                            <button class="btn btn-success btn-sm" @click="store.reviewQualification(q.id, 1)">通过</button>
                            <button class="btn btn-danger btn-sm" @click="{ const r = window.prompt('拒绝原因：'); if(r) store.reviewQualification(q.id, 2, r) }">拒绝</button>
                        </div>
                        <div v-else>
                            <span v-if="q.status === 1" style="color:var(--green)">已通过</span>
                            <span v-else style="color:var(--red)">已拒绝: {{ q.rejectReason }}</span>
                        </div>
                    </div>
                </div>
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
                        <button
                            class="btn btn-sm"
                            :class="u.status === 1 ? 'btn-success' : 'btn-danger'"
                            @click="store.updateUserStatus(u.id, u.status === 1 ? 0 : 1)"
                        >{{ u.status === 1 ? '启用' : '禁用' }}</button>
                    </div>
                </div>
                <div class="admin-panel">
                    <h3>直播间管理</h3>
                    <div v-if="store.adminRooms.length === 0" class="admin-empty">暂无直播间数据</div>
                    <div v-for="r in store.adminRooms" :key="r.id" class="admin-row">
                        <span class="admin-row-title">#{{ r.id }} {{ r.title }} <span v-if="r.ownerNickname">(主播: {{ r.ownerNickname }})</span> (状态: {{ store.statusText[r.status] || r.status }})</span>
                        <button class="btn btn-outline btn-sm" @click="store.warnRoom(r.id)">警告</button>
                        <button class="btn btn-danger btn-sm" @click="store.closeRoom(r.id)">关闭</button>
                    </div>
                </div>
            </div>
        </div>
    `
};
