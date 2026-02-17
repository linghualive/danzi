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
        roomKeyword: {
            type: String,
            default: ''
        },
        statusText: {
            type: Array,
            default: () => []
        }
    },
    emits: ['update-room-keyword', 'search', 'navigate-room'],
    template: `
        <div class="rooms-page">
            <div class="rooms-header">
                <h2>直播间大厅</h2>
                <div class="filter-group">
                    <input
                        class="form-input"
                        :value="roomKeyword"
                        placeholder="搜索直播间标题"
                        @input="$emit('update-room-keyword', $event.target.value)"
                        @keydown.enter="$emit('search')"
                    />
                    <button class="btn btn-primary btn-sm" @click="$emit('search')">搜索</button>
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
                    <img v-if="room.coverUrl" :src="room.coverUrl" class="room-cover" />
                    <div class="card-title">{{ room.title }}</div>
                    <div class="card-meta">
                        <span class="status-badge status-1">直播中</span>
                        <span>ID: {{ room.id }}</span>
                        <span v-if="room.anchorName">主播: {{ room.anchorName }}</span>
                    </div>
                </div>
            </div>
        </div>
    `
};
