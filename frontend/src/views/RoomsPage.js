import { onMounted } from '../lib/vue.js';
import { useRouter } from '../lib/vue-router.js';

import RoomsView from '../components/views/RoomsView.js';
import { useAppStore } from '../stores/app.js';

export default {
    name: 'RoomsPage',
    components: {
        RoomsView
    },
    setup() {
        const router = useRouter();
        const store = useAppStore();

        onMounted(() => {
            store.cleanupRoom();
            store.loadRooms();
        });

        const goRoom = (roomId) => {
            router.push(`/room/${roomId}`);
        };

        return {
            store,
            goRoom
        };
    },
    template: `
        <div class="view active">
            <rooms-view
                :rooms-loading="store.roomsLoading"
                :filtered-rooms="store.filteredRooms"
                :room-keyword="store.roomKeyword"
                :status-text="store.statusText"
                @update-room-keyword="store.roomKeyword = $event"
                @search="store.loadRooms"
                @navigate-room="goRoom"
            />
        </div>
    `
};
