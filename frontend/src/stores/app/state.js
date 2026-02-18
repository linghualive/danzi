export const createAppState = () => ({
    apiBase: (() => {
        const port = window.location.port;
        // Production: served by nginx (port 80/443), API proxied through same origin
        if (window.location.protocol !== 'file:' && (!port || port === '80' || port === '443')) {
            return window.location.origin;
        }
        // Dev mode: direct connection to gateway
        return `http://${window.location.hostname || 'localhost'}:9000`;
    })(),
    statusText: ['未开播', '直播中', '已结束', '管理员关闭'],
    orderStatusText: ['待支付', '已支付', '已取消', '退款申请中', '已退款'],

    initialized: false,
    currentUser: null,
    authTab: 'login',

    loginForm: {
        username: '',
        password: ''
    },
    registerForm: {
        username: '',
        password: '',
        nickname: ''
    },
    loginError: '',
    registerError: '',
    loginLoading: false,
    registerLoading: false,

    roomKeyword: '',
    rooms: [],
    roomsLoading: false,

    currentRoomId: null,
    currentRoom: null,
    lastRoomStatus: null,
    productsExpanded: false,
    chatMessages: [{ system: true, content: '欢迎来到直播间' }],
    msgInput: '',
    emojiPickerOpen: false,
    emojiCategory: 0,
    wsMsgType: 'COMMENT',
    wsStatusText: '未连接',
    wsConnected: false,
    ws: null,
    wsReconnectTimer: null,
    roomPollTimer: null,

    roomOwnerInfo: null,
    ownerFollowedByMe: false,
    roomWarnings: [],
    lastRoomWarningId: 0,
    roomProducts: [],
    productsLoading: false,

    flvPlayer: null,
    playingUrl: '',
    pendingPullUrl: '',
    playerStartedAt: 0,
    pullWatchdogTimer: null,
    streamReady: false,
    streamError: '',
    lastStreamErrorAt: 0,
    danmakuTrack: 0,
    roomViewRefs: null,

    orders: [],
    ordersLoading: false,
    soldOrders: [],
    soldOrdersLoading: false,
    refundModalVisible: false,
    refundTargetOrderId: null,
    refundReasonDraft: '',

    createRoomModalVisible: false,
    newRoomTitle: '',
    newRoomCoverFileId: null,

    roomEditTitle: '',
    roomEditCoverFileId: null,
    roomEditCoverUrl: '',
    roomEditDirty: false,
    roomEditSaving: false,
    roomEditModalVisible: false,

    addProductModalVisible: false,
    editingProductId: null,
    newProduct: { name: '', description: '', price: '', stock: '', imageFileId: null },

    productDetailModalVisible: false,
    currentProduct: null,
    productQty: 1,

    profile: {},
    profileForm: { nickname: '', bio: '', avatarFileId: null },
    followStats: { followingCount: 0, followerCount: 0, followedByMe: false },

    conversations: [],
    currentConversationTargetId: null,
    currentConversationName: '',
    currentConversationMessages: [],
    newPrivateMessage: '',

    orderKeyword: '',
    soldOrderKeyword: '',
    orderSearchTimer: null,

    followListVisible: false,
    followListType: 'following',
    followListTargetId: null,
    followListUsers: [],

    liveSummary: null,
    liveSummaryVisible: false,

    qualificationForm: { contactInfo: '', businessLicense: '', personalInfo: '' },
    myQualification: null,
    adminQualifications: [],

    adminUsers: [],
    adminRooms: [],

    toasts: [],

    _router: null
});
