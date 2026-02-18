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
    methods: {
        getAnchorName(room) {
            return room?.anchorName || room?.ownerNickname || room?.nickname || room?.username || '未知主播';
        },
        getViewerCount(room) {
            const count = Number(room?.viewerCount);
            return Number.isFinite(count) && count >= 0 ? count : 0;
        }
    },
    template: `
        <div class="rooms-page">
            <div class="rooms-header">
                <div class="rooms-title-group">
                    <h2>直播间大厅</h2>
                    <p class="rooms-subtitle">沉浸式直播，滑入即看，像刷短视频一样逛直播间</p>
                </div>
                <div class="filter-group">
                    <input
                        class="form-input"
                        :value="roomKeyword"
                        placeholder="搜索直播间标题"
                        @input="$emit('update-room-keyword', $event.target.value)"
                        @keydown.enter="$emit('search')"
                    />
                    <button class="btn btn-primary btn-sm rooms-search-btn" @click="$emit('search')">搜索</button>
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
                    <div class="room-cover-wrap">
                        <img v-if="room.coverUrl" :src="room.coverUrl" class="room-cover" />
                        <div v-else class="room-cover room-cover-empty">&#128250;</div>
                        <div class="room-cover-overlay">
                            <span class="live-chip" :class="{ offline: (room.status ?? 0) !== 1 }">
                                {{ statusText[room.status ?? 0] || '未知状态' }}
                            </span>
                            <span class="room-id-chip">#{{ room.id }}</span>
                        </div>
                    </div>
                    <div class="card-title">{{ room.title }}</div>
                    <div class="card-meta">
                        <span class="anchor-name">
                            主播 {{ getAnchorName(room) }}
                        </span>
                        <span class="viewer-fake">当前观看 {{ getViewerCount(room) }}</span>
                    </div>
                    <div class="room-card-foot">
                        <span class="room-status-inline">{{ statusText[room.status ?? 0] || '未知状态' }}</span>
                        <button class="room-enter-btn">进入直播</button>
                    </div>
                </div>
            </div>
        </div>
    `
};
