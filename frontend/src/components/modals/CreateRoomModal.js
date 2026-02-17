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
    data() {
        return {
            coverFileName: ''
        };
    },
    emits: ['close', 'update-title', 'upload-cover', 'create'],
    methods: {
        handleInput(event) {
            this.$emit('update-title', event.target.value.trim());
        },
        handleCoverFile(event) {
            const file = event.target.files && event.target.files[0];
            if (file) {
                this.coverFileName = file.name;
                this.$emit('upload-cover', file);
            }
        },
        clearAndClose() {
            this.coverFileName = '';
            this.$emit('close');
        }
    },
    template: `
        <div class="modal-overlay" :class="{ show: visible }" @click.self="clearAndClose">
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
                    <label class="upload-picker">
                        <input class="upload-picker-input" type="file" accept="image/*" @change="handleCoverFile" />
                        <span class="upload-picker-btn">选择封面</span>
                        <span class="upload-picker-name">{{ coverFileName || '支持 JPG/PNG，建议 16:9' }}</span>
                    </label>
                    <div v-if="coverFileId" class="upload-picker-meta">
                        上传成功，文件 ID: {{ coverFileId }}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="clearAndClose">取消</button>
                    <button class="btn btn-primary" @click="$emit('create')">创建</button>
                </div>
            </div>
        </div>
    `
};
