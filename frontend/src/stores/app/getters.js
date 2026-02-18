export const appGetters = {
    displayNickname(state) {
        if (!state.currentUser) {
            return '';
        }
        return state.currentUser.nickname || state.currentUser.username || '用户';
    },

    filteredRooms(state) {
        return state.rooms;
    },

    currentUserId(state) {
        return state.currentUser?.userId || state.currentUser?.id || 0;
    },

    isAdmin(state) {
        return (state.currentUser?.role || 0) === 2;
    },

    isAnchor(state) {
        return (state.currentUser?.role || 0) === 1;
    },

    isRoomOwner(state) {
        if (!state.currentUser || !state.currentRoom) {
            return false;
        }
        const uid = state.currentUser.userId || state.currentUser.id;
        return uid === state.currentRoom.userId;
    }
};
