export default {
    name: 'RoomsView',
    props: {
        roomsLoading: {
            type: Boolean,
            default: false
        },
        filteredRooms: {
            type: Array,
            default: () => []
        },
        roomFilter: {
            type: [Number, null],
            default: null
        },
        statusText: {
            type: Array,
            default: () => []
        }
    },
    emits: ['set-room-filter', 'navigate-room'],
    template: `
        <div class="rooms-page">
            <div class="rooms-header">
                <h2>直播间大厅</h2>
                <div class="filter-group">
                    <button
                        class="filter-btn"
                        :class="{ active: roomFilter === null }"
                        @click="$emit('set-room-filter', null)"
                    >
                        全部
                    </button>
                    <button
                        class="filter-btn"
                        :class="{ active: roomFilter === 1 }"
                        @click="$emit('set-room-filter', 1)"
                    >
                        直播中
                    </button>
                    <button
                        class="filter-btn"
                        :class="{ active: roomFilter === 0 }"
                        @click="$emit('set-room-filter', 0)"
                    >
                        未开播
                    </button>
                    <button
                        class="filter-btn"
                        :class="{ active: roomFilter === 2 }"
                        @click="$emit('set-room-filter', 2)"
                    >
                        已结束
                    </button>
                </div>
            </div>

            <div v-if="roomsLoading" class="page-loading">
                <span class="loading-spinner"></span> 加载中...
            </div>
            <div v-else-if="filteredRooms.length === 0" class="empty-state">
                <div class="icon">&#128250;</div>
                <p>暂无直播间</p>
            </div>
            <div v-else class="rooms-grid">
                <div
                    v-for="room in filteredRooms"
                    :key="room.id"
                    class="room-card"
                    @click="$emit('navigate-room', room.id)"
                >
                    <div class="card-title">{{ room.title }}</div>
                    <div class="card-meta">
                        <span class="status-badge" :class="'status-' + room.status">
                            {{ statusText[room.status] || '未知' }}
                        </span>
                        <span>ID: {{ room.id }}</span>
                        <span v-if="room.anchorName">主播: {{ room.anchorName }}</span>
                    </div>
                </div>
            </div>
        </div>
    `
};
