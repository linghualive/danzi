import { onMounted, watch } from '../lib/vue.js';
import { useRoute } from '../lib/vue-router.js';

import { useAppStore } from '../stores/app.js';

export default {
    name: 'MessagesPage',
    setup() {
        const store = useAppStore();
        const route = useRoute();

        onMounted(async () => {
            store.cleanupRoom();
            await store.loadConversations();
            if (route.query.target) {
                store.selectConversation(Number(route.query.target));
            }
        });

        watch(() => route.query.target, (target) => {
            if (target) {
                store.selectConversation(Number(target));
            }
        });

        return {
            store
        };
    },
    template: `
        <div class="view active messages-page">
            <div class="page-heading">
                <h2>私信中心</h2>
                <p>与买家、卖家建立一对一沟通，订单沟通更直接。</p>
            </div>
            <div class="msg-layout">
                <div class="msg-sidebar">
                    <h3>会话</h3>
                    <div v-if="store.conversations.length === 0" class="msg-empty">暂无会话记录</div>
                    <div
                        v-for="item in store.conversations"
                        :key="item.targetUserId"
                        class="msg-conv-item"
                        :class="{ active: store.currentConversationTargetId === item.targetUserId }"
                        @click="store.selectConversation(item.targetUserId)"
                    >
                        <div class="msg-conv-name">{{ item.targetNickname || item.targetUserName }}</div>
                        <div class="msg-conv-last">{{ item.lastMessage }}</div>
                    </div>
                </div>
                <div class="msg-main">
                    <h3 v-if="store.currentConversationTargetId">与 {{ store.currentConversationName }} 的私信</h3>
                    <h3 v-else>请选择左侧会话</h3>
                    <div class="msg-thread">
                        <div v-if="store.currentConversationTargetId && store.currentConversationMessages.length === 0" class="msg-empty">
                            还没有消息，先发一句问候吧
                        </div>
                        <div
                            v-for="msg in store.currentConversationMessages"
                            :key="msg.id"
                            class="msg-bubble"
                            :class="{ mine: msg.senderId === (store.currentUser?.userId || store.currentUser?.id) }"
                        >
                            <div>{{ msg.content }}</div>
                        </div>
                    </div>
                    <div class="msg-send-row" v-if="store.currentConversationTargetId">
                        <input class="form-input" :value="store.newPrivateMessage" @input="store.newPrivateMessage = $event.target.value" @keydown.enter="store.sendPrivateMessage" />
                        <button class="btn btn-primary" @click="store.sendPrivateMessage">发送</button>
                    </div>
                </div>
            </div>
        </div>
    `
};
