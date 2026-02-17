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
        coverFileId: {
            type: [Number, null],
            default: null
        }
    },
    emits: ['close', 'update-title', 'upload-cover', 'create'],
    methods: {
        handleInput(event) {
            this.$emit('update-title', event.target.value.trim());
        },
        handleCoverFile(event) {
            const file = event.target.files && event.target.files[0];
            if (file) {
                this.$emit('upload-cover', file);
            }
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
                    <label>封面图片（可选）</label>
                    <input
                        type="file"
                        @change="handleCoverFile"
                    />
                    <div v-if="coverFileId" style="font-size:12px;color:#666;margin-top:6px;">
                        已上传文件ID: {{ coverFileId }}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="$emit('close')">取消</button>
                    <button class="btn btn-primary" @click="$emit('create')">创建</button>
                </div>
            </div>
        </div>
    `
};
