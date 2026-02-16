export default {
    name: 'AppNavbar',
    props: {
        displayNickname: {
            type: String,
            default: ''
        }
    },
    emits: ['navigate-rooms', 'navigate-orders', 'open-create-room', 'logout'],
    template: `
        <nav class="navbar">
            <div class="logo" @click="$emit('navigate-rooms')">Live Commerce</div>
            <div class="nav-spacer"></div>
            <div class="nav-user">
                <span class="user-nickname">{{ displayNickname }}</span>
                <button class="btn btn-outline btn-sm" @click="$emit('navigate-orders')">我的订单</button>
                <button class="btn btn-outline btn-sm" @click="$emit('open-create-room')">创建直播间</button>
                <button class="btn btn-danger btn-sm" @click="$emit('logout')">
                    退出
                </button>
            </div>
        </nav>
    `
};
