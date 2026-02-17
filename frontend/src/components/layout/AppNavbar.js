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
            <div class="logo" @click="$emit('navigate-rooms')">Live Commerce</div>
            <div class="nav-spacer"></div>
            <div class="nav-user">
                <span class="user-nickname">{{ displayNickname }}</span>
                <button class="btn btn-outline btn-sm" @click="$emit('navigate-profile')">个人主页</button>
                <button class="btn btn-outline btn-sm" @click="$emit('navigate-messages')">私信</button>
                <button class="btn btn-outline btn-sm" @click="$emit('navigate-orders')">我的订单</button>
                <button class="btn btn-outline btn-sm" @click="$emit('navigate-sold')">我卖出的</button>
                <button class="btn btn-outline btn-sm" @click="$emit('open-create-room')">创建直播间</button>
                <button v-if="isAdmin" class="btn btn-outline btn-sm" @click="$emit('navigate-admin')">管理后台</button>
                <button class="btn btn-danger btn-sm" @click="$emit('logout')">
                    退出
                </button>
            </div>
        </nav>
    `
};
