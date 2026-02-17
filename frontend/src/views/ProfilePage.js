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
                            <div class="profile-stat-item">
                                <span class="profile-stat-label">关注</span>
                                <b class="profile-stat-value">{{ store.followStats.followingCount || 0 }}</b>
                            </div>
                            <div class="profile-stat-item">
                                <span class="profile-stat-label">粉丝</span>
                                <b class="profile-stat-value">{{ store.followStats.followerCount || 0 }}</b>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="profile-actions">
                    <button class="btn btn-primary" @click="store.saveMyProfile">保存资料</button>
                </div>
            </div>
        </div>
    `
};
