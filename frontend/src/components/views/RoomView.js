import { formatPrice } from '../../utils/format.js';

const EMOJI_CATEGORIES = [
    {
        icon: '\u{1F600}',
        emojis: ['\u{1F600}','\u{1F602}','\u{1F923}','\u{1F60A}','\u{1F60D}','\u{1F970}','\u{1F618}','\u{1F60E}','\u{1F917}','\u{1F914}','\u{1F92D}','\u{1F631}','\u{1F62D}','\u{1F621}','\u{1F971}','\u{1F60F}','\u{1F913}','\u{1F929}','\u{1F973}','\u{1F97A}','\u{1F624}']
    },
    {
        icon: '\u{1F44D}',
        emojis: ['\u{1F44D}','\u{1F44F}','\u{1F44B}','\u{270C}\uFE0F','\u{1F91E}','\u{1F4AA}','\u{1F64F}','\u{1F91D}','\u{1F44C}','\u{1F918}','\u270A','\u{1F919}','\u{1F446}','\u{1F447}']
    },
    {
        icon: '\u2764\uFE0F',
        emojis: ['\u2764\uFE0F','\u{1F525}','\u{1F389}','\u{1F381}','\u{1F3C6}','\u2B50','\u{1F48E}','\u{1F4B0}','\u{1F384}','\u{1F388}','\u{1F380}','\u{1F4A5}','\u{1F308}','\u2728']
    },
    {
        icon: '\u{1F354}',
        emojis: ['\u{1F354}','\u{1F355}','\u{1F370}','\u{1F366}','\u{1F36D}','\u2615','\u{1F37B}','\u{1F349}','\u{1F353}','\u{1F34E}','\u{1F382}','\u{1F9C1}','\u{1F363}','\u{1F35C}']
    }
];

export default {
    name: 'RoomView',
    props: {
        currentRoom: {
            type: Object,
            default: null
        },
        isRoomOwner: {
            type: Boolean,
            default: false
        },
        currentUserId: {
            type: Number,
            default: 0
        },
        isAdmin: {
            type: Boolean,
            default: false
        },
        roomOwnerInfo: {
            type: Object,
            default: null
        },
        ownerFollowedByMe: {
            type: Boolean,
            default: false
        },
        statusText: {
            type: Array,
            default: () => []
        },
        productsExpanded: {
            type: Boolean,
            default: false
        },
        chatMessages: {
            type: Array,
            default: () => []
        },
        wsStatusText: {
            type: String,
            default: '\u672A\u8FDE\u63A5'
        },
        wsConnected: {
            type: Boolean,
            default: false
        },
        wsMsgType: {
            type: String,
            default: 'COMMENT'
        },
        msgInput: {
            type: String,
            default: ''
        },
        emojiPickerOpen: {
            type: Boolean,
            default: false
        },
        emojiCategory: {
            type: Number,
            default: 0
        },
        productsLoading: {
            type: Boolean,
            default: false
        },
        roomProducts: {
            type: Array,
            default: () => []
        },
        streamReady: {
            type: Boolean,
            default: false
        },
        streamError: {
            type: String,
            default: ''
        }
    },
    emits: [
        'ready',
        'navigate-rooms',
        'start-live',
        'stop-live',
        'copy-text',
        'toggle-products',
        'set-ws-msg-type',
        'update-msg-input',
        'send-message',
        'send-like',
        'retry-stream',
        'show-product-detail',
        'quick-buy',
        'open-add-product',
        'delete-product',
        'edit-product',
        'toggle-emoji-picker',
        'set-emoji-category',
        'insert-emoji',
        'follow-owner',
        'contact-owner'
    ],
    data() {
        return {
            isPlaying: false,
            isMuted: true,
            volume: 0.8,
            isFullscreen: false,
            controlsVisible: true,
            controlsTimer: null,
            fsChatOpen: true
        };
    },
    mounted() {
        this.emitReady();
        document.addEventListener('fullscreenchange', this.onFullscreenChange);
    },
    beforeUnmount() {
        document.removeEventListener('fullscreenchange', this.onFullscreenChange);
        if (this.controlsTimer) clearTimeout(this.controlsTimer);
    },
    computed: {
        currentEmojis() {
            const cat = EMOJI_CATEGORIES[this.emojiCategory];
            return cat ? cat.emojis : EMOJI_CATEGORIES[0].emojis;
        },
        emojiCategories() {
            return EMOJI_CATEGORIES;
        }
    },
    watch: {
        chatMessages: {
            handler() {
                this.$nextTick(() => {
                    const el = this.$refs.fsChatMessages;
                    if (el) el.scrollTop = el.scrollHeight;
                });
            },
            deep: true
        }
    },
    methods: {
        emitReady() {
            this.$emit('ready', {
                videoPlayer: this.$refs.videoPlayer,
                chatMessages: this.$refs.chatMessages,
                danmakuLayer: this.$refs.danmakuLayer
            });
        },
        formatPrice,
        onMsgInput(event) {
            this.$emit('update-msg-input', event.target.value);
        },
        togglePlay() {
            const video = this.$refs.videoPlayer;
            if (!video) return;
            if (video.paused) {
                video.play();
            } else {
                video.pause();
            }
        },
        toggleMute() {
            const video = this.$refs.videoPlayer;
            if (!video) return;
            this.isMuted = !this.isMuted;
            video.muted = this.isMuted;
        },
        setVolume(v) {
            const video = this.$refs.videoPlayer;
            if (!video) return;
            this.volume = parseFloat(v);
            video.volume = this.volume;
            if (this.volume > 0 && this.isMuted) {
                this.isMuted = false;
                video.muted = false;
            } else if (this.volume === 0) {
                this.isMuted = true;
                video.muted = true;
            }
        },
        toggleFullscreen() {
            const wrapper = this.$refs.videoWrapper;
            if (!wrapper) return;
            if (!document.fullscreenElement) {
                wrapper.requestFullscreen();
            } else {
                document.exitFullscreen();
            }
        },
        showControls() {
            this.controlsVisible = true;
            if (this.controlsTimer) clearTimeout(this.controlsTimer);
            this.controlsTimer = setTimeout(() => {
                this.controlsVisible = false;
            }, 3000);
        },
        onFullscreenChange() {
            this.isFullscreen = !!document.fullscreenElement;
            if (this.isFullscreen) {
                this.showControls();
            } else {
                this.controlsVisible = true;
                if (this.controlsTimer) clearTimeout(this.controlsTimer);
            }
        },
        canManageProduct(product) {
            return this.isAdmin || this.isRoomOwner || this.currentUserId === (product?.sellerId || 0);
        }
    },
    template: `
        <div class="room-detail">
            <div class="room-left">
                <div class="room-top-bar">
                    <span class="back-btn" @click="$emit('navigate-rooms')">&#9664; \u8FD4\u56DE</span>
                    <h2>{{ currentRoom?.title || '\u52A0\u8F7D\u4E2D...' }}</h2>
                    <span class="status-badge" :class="'status-' + (currentRoom?.status ?? 0)">
                        {{ statusText[currentRoom?.status ?? 0] || '\u672A\u77E5' }}
                    </span>
                    <span v-if="roomOwnerInfo" class="owner-tag">
                        \u4E3B\u64AD: <b>{{ roomOwnerInfo.nickname || roomOwnerInfo.username || '\u672A\u77E5' }}</b>
                    </span>
                    <button
                        v-if="roomOwnerInfo && !isRoomOwner"
                        class="btn btn-outline btn-sm"
                        @click="$emit('follow-owner')"
                    >
                        {{ ownerFollowedByMe ? '已关注' : '关注' }}
                    </button>
                    <button
                        v-if="roomOwnerInfo && !isRoomOwner"
                        class="btn btn-outline btn-sm"
                        @click="$emit('contact-owner')"
                    >私信</button>
                    <span>
                        <button
                            v-if="isRoomOwner && currentRoom?.status === 0"
                            class="btn btn-success btn-sm"
                            @click="$emit('start-live')"
                        >
                            \u5F00\u64AD
                        </button>
                        <button
                            v-if="isRoomOwner && currentRoom?.status === 1"
                            class="btn btn-danger btn-sm"
                            @click="$emit('stop-live')"
                        >
                            \u505C\u64AD
                        </button>
                    </span>
                </div>

                <div class="video-wrapper" ref="videoWrapper" :class="{ 'controls-hidden': !controlsVisible }" @mousemove="showControls">
                    <video ref="videoPlayer" autoplay muted playsinline v-show="currentRoom?.status === 1" @play="isPlaying = true" @pause="isPlaying = false"></video>
                    <div class="video-placeholder" v-show="currentRoom?.status !== 1">
                        <div class="icon">&#128250;</div>
                        <p v-if="currentRoom?.status === 2">\u76F4\u64AD\u5DF2\u7ED3\u675F</p>
                        <p v-else>\u4E3B\u64AD\u5C1A\u672A\u5F00\u64AD\uFF0C\u8BF7\u7A0D\u5019...</p>
                    </div>
                    <div class="danmaku-layer" ref="danmakuLayer"></div>

                    <div class="fullscreen-chat-bar">
                        <button
                            class="fs-msg-type-btn"
                            :class="{ active: wsMsgType === 'DANMAKU' }"
                            @click="$emit('set-ws-msg-type', wsMsgType === 'COMMENT' ? 'DANMAKU' : 'COMMENT')"
                        >{{ wsMsgType === 'COMMENT' ? '\u8BC4\u8BBA' : '\u5F39\u5E55' }}</button>
                        <input
                            class="fs-chat-input"
                            :value="msgInput"
                            placeholder="\u8BF4\u70B9\u4EC0\u4E48..."
                            @input="onMsgInput"
                            @keydown.enter="$emit('send-message')"
                        />
                        <button class="fs-send-btn" @click="$emit('send-message')">\u53D1\u9001</button>
                        <button class="fs-like-btn" @click="$emit('send-like')">&#9829;</button>
                    </div>

                    <div class="video-controls">
                        <button class="vc-play-btn" @click="togglePlay" :title="isPlaying ? '\u6682\u505C' : '\u64AD\u653E'">{{ isPlaying ? '\u23F8' : '\u25B6' }}</button>
                        <div class="vc-spacer"></div>
                        <div class="vc-volume">
                            <button class="vc-volume-btn" @click="toggleMute" :title="isMuted ? '\u53D6\u6D88\u9759\u97F3' : '\u9759\u97F3'">{{ isMuted ? '\uD83D\uDD07' : '\uD83D\uDD0A' }}</button>
                            <input type="range" class="vc-volume-slider" min="0" max="1" step="0.05" :value="isMuted ? 0 : volume" @input="setVolume($event.target.value)" />
                        </div>
                        <button class="vc-fullscreen-btn" @click="toggleFullscreen" :title="isFullscreen ? '\u9000\u51FA\u5168\u5C4F' : '\u5168\u5C4F'">{{ isFullscreen ? '\u2B73' : '\u2B72' }}</button>
                    </div>

                    <div class="products-bar" :class="{ expanded: productsExpanded }">
                        <button class="products-toggle-btn" @click="$emit('toggle-products')">
                            &#128230; \u5546\u54C1 ({{ roomProducts.length }})
                        </button>
                        <div class="products-strip" v-if="productsExpanded">
                            <div v-if="isRoomOwner" class="product-card product-card-add" @click="$emit('open-add-product')">
                                <div class="product-card-add-icon">+</div>
                                <div class="product-card-add-text">\u6DFB\u52A0\u5546\u54C1</div>
                            </div>
                            <div v-if="productsLoading" class="product-card product-card-placeholder">
                                <span class="loading-spinner"></span>
                            </div>
                            <template v-else-if="roomProducts.length === 0">
                                <div class="product-card product-card-placeholder">
                                    <span style="color:var(--text-muted);font-size:12px">\u6682\u65E0\u5546\u54C1</span>
                                </div>
                            </template>
                            <template v-else>
                                <div
                                    class="product-card"
                                    v-for="(product, index) in roomProducts"
                                    :key="product.id || index"
                                >
                                    <img v-if="product.imageUrl" class="product-card-thumb-img" :src="product.imageUrl" />
                                    <div v-else class="product-card-thumb">&#128230;</div>
                                    <div class="product-card-name">{{ product.name || product.productName || '-' }}</div>
                                    <div class="product-card-price">&yen;{{ formatPrice(product.price) }}</div>
                                    <div class="product-card-stock">\u5E93\u5B58: {{ product.stock ?? '-' }}</div>
                                    <div class="product-card-actions">
                                        <button class="btn btn-outline btn-sm" @click="$emit('show-product-detail', product)">\u8BE6\u60C5</button>
                                        <button v-if="!canManageProduct(product)" class="btn btn-primary btn-sm" @click="$emit('quick-buy', product.id)">\u8D2D\u4E70</button>
                                        <button v-if="canManageProduct(product)" class="btn btn-outline btn-sm" @click="$emit('edit-product', product)">\u7F16\u8F91</button>
                                        <button v-if="canManageProduct(product)" class="btn btn-danger btn-sm" @click="$emit('delete-product', product.id)">\u5220\u9664</button>
                                    </div>
                                </div>
                            </template>
                        </div>
                    </div>

                    <button class="fs-chat-toggle" v-show="isFullscreen && !fsChatOpen" @click="fsChatOpen = true" title="\u5C55\u5F00\u804A\u5929">&#128172;</button>
                    <div class="fs-chat-panel" v-show="isFullscreen && fsChatOpen">
                        <div class="fs-chat-panel-header">
                            <span>\u804A\u5929</span>
                            <button class="fs-chat-close" @click="fsChatOpen = false">&#10005;</button>
                        </div>
                        <div class="fs-chat-panel-messages" ref="fsChatMessages">
                            <div
                                v-for="(msg, index) in chatMessages"
                                :key="'fs-' + (msg.id || index) + '-' + (msg.timestamp || '')"
                                class="fs-chat-msg"
                                :class="[(msg.type || '').toLowerCase(), { system: msg.system }]"
                            >
                                <template v-if="msg.system">
                                    <span class="fs-msg-system">{{ msg.content }}</span>
                                </template>
                                <template v-else-if="msg.type === 'LIKE'">
                                    <span class="fs-msg-nick">{{ msg.nickname }}</span>
                                    <span class="fs-msg-like">&#9829; {{ msg.content }}</span>
                                </template>
                                <template v-else>
                                    <span class="fs-msg-nick">{{ msg.nickname }}:</span>
                                    <span class="fs-msg-content">{{ msg.content }}</span>
                                </template>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="obs-info" v-if="isRoomOwner">
                    <div>
                        <label>OBS \u670D\u52A1\u5668:</label>
                        <code @click="$emit('copy-text', 'rtmp://localhost:1935/live')">rtmp://localhost:1935/live</code>
                    </div>
                    <div>
                        <label>\u63A8\u6D41\u5BC6\u94A5:</label>
                        <code @click="$emit('copy-text', currentRoom?.streamKey || '-')">{{ currentRoom?.streamKey || '-' }}</code>
                    </div>
                    <div>
                        <label>\u62C9\u6D41\u5730\u5740:</label>
                        <code @click="$emit('copy-text', currentRoom?.pullUrl || '-')">{{ currentRoom?.pullUrl || '-' }}</code>
                    </div>
                </div>
            </div>

            <div class="room-right">
                <div class="right-panel-header">\u804A\u5929\u4E92\u52A8</div>

                <div class="right-panel active">
                    <div class="chat-messages" ref="chatMessages">
                        <div
                            v-for="(msg, index) in chatMessages"
                            :key="(msg.id || index) + '-' + (msg.timestamp || '')"
                            class="msg"
                            :class="[(msg.type || '').toLowerCase(), { system: msg.system }]"
                        >
                            <template v-if="msg.system">
                                {{ msg.content }}
                            </template>
                            <template v-else-if="msg.type === 'LIKE'">
                                <span class="nickname">{{ msg.nickname }}</span>
                                <span class="content">&#9829; {{ msg.content }}</span>
                            </template>
                            <template v-else>
                                <span class="nickname">{{ msg.nickname }}:</span>
                                <span class="content">{{ msg.content }}</span>
                            </template>
                        </div>
                    </div>

                    <div class="chat-input-area">
                        <div class="ws-status" :class="wsConnected ? 'connected' : 'disconnected'">{{ wsStatusText }}</div>
                        <div class="msg-type-row">
                            <button
                                class="msg-type-btn"
                                :class="{ active: wsMsgType === 'COMMENT' }"
                                @click="$emit('set-ws-msg-type', 'COMMENT')"
                            >
                                \u8BC4\u8BBA
                            </button>
                            <button
                                class="msg-type-btn"
                                :class="{ active: wsMsgType === 'DANMAKU' }"
                                @click="$emit('set-ws-msg-type', 'DANMAKU')"
                            >
                                \u5F39\u5E55
                            </button>
                        </div>
                        <div class="input-row">
                            <input
                                :value="msgInput"
                                placeholder="\u8BF4\u70B9\u4EC0\u4E48..."
                                @input="onMsgInput"
                                @keydown.enter="$emit('send-message')"
                            />
                            <div class="emoji-picker-wrap">
                                <button class="emoji-toggle-btn" @click.stop="$emit('toggle-emoji-picker')">\u{1F600}</button>
                                <div class="emoji-picker" v-if="emojiPickerOpen" @click.stop>
                                    <div class="emoji-picker-header">
                                        <button
                                            v-for="(cat, ci) in emojiCategories"
                                            :key="ci"
                                            class="emoji-cat-btn"
                                            :class="{ active: emojiCategory === ci }"
                                            @click="$emit('set-emoji-category', ci)"
                                        >{{ cat.icon }}</button>
                                    </div>
                                    <div class="emoji-grid">
                                        <button
                                            v-for="(em, ei) in currentEmojis"
                                            :key="ei"
                                            class="emoji-cell"
                                            @click="$emit('insert-emoji', em)"
                                        >{{ em }}</button>
                                    </div>
                                </div>
                            </div>
                            <button class="btn btn-primary" @click="$emit('send-message')">\u53D1\u9001</button>
                            <button class="btn-like-send" @click="$emit('send-like')">&#9829;</button>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    `
};
