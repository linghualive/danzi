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
                :room-filter="store.roomFilter"
                :status-text="store.statusText"
                @set-room-filter="store.setRoomFilter"
                @navigate-room="goRoom"
            />
        </div>
    `
};
