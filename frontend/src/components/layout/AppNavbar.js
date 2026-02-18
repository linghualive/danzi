export default {
    name: 'AppNavbar',
    props: {
        displayNickname: {
            type: String,
            default: ''
        },
        isAdmin: {
            type: Boolean,
            default: false
        },
        isAnchor: {
            type: Boolean,
            default: false
        }
    },
    emits: [
        'navigate-rooms',
        'navigate-orders',
        'navigate-sold',
        'navigate-profile',
        'navigate-messages',
        'navigate-admin',
        'open-create-room',
        'logout'
    ],
    template: `
        <nav class="navbar">
            <div class="brand-block" @click="$emit('navigate-rooms')">
                <div class="logo">LIVE</div>
                <span class="brand-sub">直播电商</span>
            </div>
            <div class="nav-spacer"></div>
            <div class="nav-user">
                <span class="user-pill">{{ displayNickname }}</span>
                <button class="btn btn-outline btn-sm nav-btn" @click="$emit('navigate-profile')">个人主页</button>
                <button class="btn btn-outline btn-sm nav-btn" @click="$emit('navigate-messages')">私信</button>
                <button class="btn btn-outline btn-sm nav-btn" @click="$emit('navigate-orders')">我的订单</button>
                <button v-if="isAnchor" class="btn btn-outline btn-sm nav-btn" @click="$emit('navigate-sold')">我卖出的</button>
                <button v-if="isAnchor" class="btn btn-primary btn-sm nav-btn" @click="$emit('open-create-room')">开始直播</button>
                <button v-if="isAdmin" class="btn btn-outline btn-sm nav-btn" @click="$emit('navigate-admin')">管理后台</button>
                <button class="btn btn-danger btn-sm nav-btn" @click="$emit('logout')">
                    退出
                </button>
            </div>
        </nav>
    `
};
