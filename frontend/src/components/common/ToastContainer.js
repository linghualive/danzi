export default {
    name: 'ToastContainer',
    props: {
        toasts: {
            type: Array,
            default: () => []
        }
    },
    template: `
        <div class="toast-container">
            <div
                v-for="item in toasts"
                :key="item.id"
                :class="['toast', 'toast-' + item.type]"
            >
                {{ item.message }}
            </div>
        </div>
    `
};
