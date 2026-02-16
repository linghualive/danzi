export default {
    name: 'CreateRoomModal',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        title: {
            type: String,
            default: ''
        },
        cover: {
            type: String,
            default: ''
        }
    },
    emits: ['close', 'update-title', 'update-cover', 'create'],
    methods: {
        handleInput(event) {
            this.$emit('update-title', event.target.value.trim());
        },
        handleCoverInput(event) {
            this.$emit('update-cover', event.target.value.trim());
        }
    },
    template: `
        <div class="modal-overlay" :class="{ show: visible }" @click.self="$emit('close')">
            <div class="modal">
                <h3>创建直播间</h3>
                <div class="form-group">
                    <label>直播间标题</label>
                    <input
                        class="form-input"
                        :value="title"
                        placeholder="输入直播间标题"
                        @input="handleInput"
                    />
                </div>
                <div class="form-group">
                    <label>封面图片 URL（可选）</label>
                    <input
                        class="form-input"
                        :value="cover"
                        placeholder="输入封面图片地址"
                        @input="handleCoverInput"
                    />
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="$emit('close')">取消</button>
                    <button class="btn btn-primary" @click="$emit('create')">创建</button>
                </div>
            </div>
        </div>
    `
};
