import { onBeforeUnmount, onMounted, watch } from '../lib/vue.js';
import { useRoute, useRouter } from '../lib/vue-router.js';

import RoomView from '../components/views/RoomView.js';
import { useAppStore } from '../stores/app.js';

export default {
    name: 'RoomPage',
    components: {
        RoomView
    },
    setup() {
        const route = useRoute();
        const router = useRouter();
        const store = useAppStore();

        const enterByRoute = async () => {
            const roomId = Number(route.params.id);
            if (!roomId) {
                router.replace('/rooms');
                return;
            }

            if (store.currentRoomId !== roomId) {
                store.cleanupRoom();
                await store.enterRoom(roomId);
            }
        };

        const closeEmojiOnOutsideClick = () => {
            if (store.emojiPickerOpen) {
                store.emojiPickerOpen = false;
            }
        };

        onMounted(() => {
            enterByRoute();
            document.addEventListener('click', closeEmojiOnOutsideClick);
        });

        watch(() => route.params.id, () => {
            enterByRoute();
        });

        watch(
            () => [store.currentRoom?.status, store.currentRoom?.pullUrl, !!store.roomViewRefs?.videoPlayer],
            () => {
                store.syncPlayer();
            }
        );

        onBeforeUnmount(() => {
            document.removeEventListener('click', closeEmojiOnOutsideClick);
            store.cleanupRoom();
            store.roomViewRefs = null;
        });

        return {
            store,
            goRooms: () => router.push('/rooms')
        };
    },
    template: `
        <div class="view active">
            <room-view
                :current-room="store.currentRoom"
                :is-room-owner="store.isRoomOwner"
                :current-user-id="store.currentUserId"
                :is-admin="store.isAdmin"
                :room-owner-info="store.roomOwnerInfo"
                :owner-followed-by-me="store.ownerFollowedByMe"
                :room-warnings="store.roomWarnings"
                :status-text="store.statusText"
                :room-edit-title="store.roomEditTitle"
                :room-edit-cover-url="store.roomEditCoverUrl"
                :room-edit-dirty="store.roomEditDirty"
                :room-edit-saving="store.roomEditSaving"
                :room-edit-modal-visible="store.roomEditModalVisible"
                :products-expanded="store.productsExpanded"
                :chat-messages="store.chatMessages"
                :ws-status-text="store.wsStatusText"
                :ws-connected="store.wsConnected"
                :ws-msg-type="store.wsMsgType"
                :msg-input="store.msgInput"
                :emoji-picker-open="store.emojiPickerOpen"
                :emoji-category="store.emojiCategory"
                :products-loading="store.productsLoading"
                :room-products="store.roomProducts"
                :stream-ready="store.streamReady"
                :stream-error="store.streamError"
                :live-summary="store.liveSummary"
                :live-summary-visible="store.liveSummaryVisible"
                :full-media-url="store.fullMediaUrl"
                @ready="store.onRoomViewReady"
                @navigate-rooms="goRooms"
                @start-live="store.doStartLive"
                @stop-live="store.doStopLive"
                @open-room-edit-modal="store.openRoomEditModal"
                @close-room-edit-modal="store.closeRoomEditModal"
                @update-room-edit-title="store.updateRoomEditTitle"
                @upload-room-edit-cover="store.uploadRoomEditCover"
                @reset-room-edit="store.resetRoomEditDraft"
                @save-room-edit="store.saveRoomBasic"
                @copy-text="store.copyText"
                @toggle-products="store.productsExpanded = !store.productsExpanded"
                @set-ws-msg-type="store.wsMsgType = $event"
                @update-msg-input="store.updateMsgInput"
                @send-message="store.sendMessage"
                @send-like="store.sendLike"
                @retry-stream="store.syncPlayer"
                @show-product-detail="store.showProductDetail"
                @quick-buy="store.quickBuy"
                @open-add-product="store.openAddProductModal"
                @delete-product="store.doDeleteProduct"
                @edit-product="store.openEditProductModal"
                @toggle-emoji-picker="store.toggleEmojiPicker"
                @set-emoji-category="store.setEmojiCategory"
                @insert-emoji="store.insertEmoji"
                @follow-owner="store.toggleFollowOwner"
                @contact-owner="store.contactRoomOwner"
                @close-summary="store.closeLiveSummary"
            />
        </div>
    `
};
