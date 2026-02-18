import { onMounted } from '../lib/vue.js';

import { useAppStore } from '../stores/app.js';

export default {
    name: 'ProfilePage',
    data() {
        return {
            defaultAvatarUrl: './assets/default-avatar.svg',
            avatarFileName: ''
        };
    },
    setup() {
        const store = useAppStore();

        onMounted(() => {
            store.cleanupRoom();
            store.loadMyProfile();
            store.loadMyQualification();
        });

        return {
            store
        };
    },
    methods: {
        onAvatarError(event) {
            event.target.src = this.defaultAvatarUrl;
        },
        handleAvatarFile(event) {
            const file = event.target.files && event.target.files[0];
            this.avatarFileName = file ? file.name : '';
            this.store.uploadProfileAvatar(event);
        }
    },
    template: `
        <div class="view active profile-page">
            <div class="profile-card">
                <div class="page-heading">
                    <h2>个人主页</h2>
                    <p>完善昵称、简介和头像信息，展示更完整的个人形象。</p>
                </div>

                <div class="profile-grid">
                    <div class="profile-form-panel">
                        <div class="profile-row">
                            <label>昵称</label>
                            <input
                                class="form-input"
                                :value="store.profileForm.nickname"
                                @input="store.profileForm.nickname = $event.target.value"
                            />
                        </div>
                        <div class="profile-row">
                            <label>个人介绍</label>
                            <textarea
                                class="form-input"
                                rows="5"
                                :value="store.profileForm.bio"
                                @input="store.profileForm.bio = $event.target.value"
                            ></textarea>
                        </div>
                        <div class="profile-row">
                            <label>头像文件</label>
                            <label class="upload-picker">
                                <input class="upload-picker-input" type="file" accept="image/*" @change="handleAvatarFile($event)" />
                                <span class="upload-picker-btn">上传头像</span>
                                <span class="upload-picker-name">{{ avatarFileName || '支持 JPG/PNG，建议小于 2MB' }}</span>
                            </label>
                            <div v-if="store.profileForm.avatarFileId" class="upload-picker-meta">
                                上传成功，文件 ID: {{ store.profileForm.avatarFileId }}
                            </div>
                        </div>
                    </div>

                    <div class="profile-side-panel">
                        <div class="profile-avatar-wrap">
                            <img
                                :src="store.fullMediaUrl(store.profile.avatarUrl) || defaultAvatarUrl"
                                class="profile-avatar"
                                @error="onAvatarError"
                            />
                        </div>
                        <div class="profile-stats">
                            <div class="profile-stat-item" style="cursor:pointer" @click="store.showFollowList('following')">
                                <span class="profile-stat-label">关注</span>
                                <b class="profile-stat-value">{{ store.followStats.followingCount || 0 }}</b>
                            </div>
                            <div class="profile-stat-item" style="cursor:pointer" @click="store.showFollowList('followers')">
                                <span class="profile-stat-label">粉丝</span>
                                <b class="profile-stat-value">{{ store.followStats.followerCount || 0 }}</b>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="profile-actions">
                    <button class="btn btn-primary" @click="store.saveMyProfile">保存资料</button>
                    <button class="btn btn-outline" @click="$router.push('/messages')">私信中心</button>
                </div>

                <!-- F6B: Broadcast Qualification -->
                <div class="profile-form-panel" style="margin-top:24px">
                    <h3>开播资格申请</h3>
                    <div v-if="store.myQualification && store.myQualification.status === 0" style="margin-bottom:8px;padding:12px;border-radius:8px;background:rgba(255,180,0,0.1);border:1px solid var(--orange)">
                        <span style="color:var(--orange);font-weight:600">待审核</span>
                        <p style="margin:8px 0 0;color:var(--text-secondary)">您的开播资格申请已提交，请等待管理员审核。</p>
                    </div>
                    <div v-else-if="store.myQualification && store.myQualification.status === 1" style="margin-bottom:8px;padding:12px;border-radius:8px;background:rgba(0,200,100,0.1);border:1px solid var(--green)">
                        <span style="color:var(--green);font-weight:600">已通过</span>
                        <p style="margin:8px 0 0;color:var(--text-secondary)">您已获得开播资格，可以创建直播间开播了。</p>
                    </div>
                    <template v-else>
                        <div v-if="store.myQualification && store.myQualification.status === 2" style="margin-bottom:12px;padding:12px;border-radius:8px;background:rgba(255,80,80,0.1);border:1px solid var(--red)">
                            <span style="color:var(--red);font-weight:600">已拒绝</span> — {{ store.myQualification.rejectReason }}
                            <p style="margin:8px 0 0;color:var(--text-secondary)">您可以修改信息后重新提交申请。</p>
                        </div>
                        <p v-else style="margin-bottom:12px;color:var(--text-secondary)">要创建直播间开播，需要先提交开播资格申请并通过审核。</p>
                        <div class="profile-row">
                            <label>联系方式</label>
                            <input class="form-input" v-model="store.qualificationForm.contactInfo" placeholder="手机号/微信号" />
                        </div>
                        <div class="profile-row">
                            <label>营业执照</label>
                            <input class="form-input" v-model="store.qualificationForm.businessLicense" placeholder="营业执照编号" />
                        </div>
                        <div class="profile-row">
                            <label>个人信息</label>
                            <textarea class="form-input" rows="3" v-model="store.qualificationForm.personalInfo" placeholder="简要介绍"></textarea>
                        </div>
                        <button class="btn btn-primary" @click="store.submitQualification">{{ store.myQualification && store.myQualification.status === 2 ? '重新提交申请' : '提交申请' }}</button>
                    </template>
                </div>
            </div>

            <!-- Follow List Modal -->
            <div v-if="store.followListVisible" class="modal-overlay" @click.self="store.closeFollowList()">
                <div class="modal-content" style="max-width:420px">
                    <div class="modal-header">
                        <h3>{{ store.followListType === 'following' ? '关注列表' : '粉丝列表' }}</h3>
                        <button class="modal-close" @click="store.closeFollowList()">&times;</button>
                    </div>
                    <div class="modal-body">
                        <div v-if="store.followListUsers.length === 0" class="empty-state" style="padding:24px 0">
                            <p>暂无数据</p>
                        </div>
                        <div v-for="u in store.followListUsers" :key="u.userId" class="admin-row" style="display:flex;align-items:center;gap:10px;padding:8px 0">
                            <img v-if="u.avatarUrl" :src="store.fullMediaUrl(u.avatarUrl)" style="width:32px;height:32px;border-radius:50%;object-fit:cover" />
                            <span v-else style="width:32px;height:32px;border-radius:50%;background:var(--bg-elevated);display:inline-flex;align-items:center;justify-content:center">&#128100;</span>
                            <span style="flex:1">{{ u.nickname || u.username }}</span>
                            <button v-if="u.userId !== store.currentUserId" class="btn btn-outline btn-sm" @click="store.toggleFollowInList(u.userId)">
                                {{ u.followedByMe ? '已关注' : '关注' }}
                            </button>
                            <button v-if="u.userId !== store.currentUserId" class="btn btn-outline btn-sm" @click="store.contactFromOrder(u.userId); store.closeFollowList()">私信</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `
};
