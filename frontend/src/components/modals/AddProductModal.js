export default {
    name: 'AddProductModal',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        product: {
            type: Object,
            default: () => ({ name: '', description: '', price: '', stock: '' })
        },
        editing: {
            type: Boolean,
            default: false
        }
    },
    emits: ['close', 'update-field', 'upload-image', 'create'],
    data() {
        return {
            imageFileName: ''
        };
    },
    methods: {
        handleInput(field, event) {
            this.$emit('update-field', { field, value: event.target.value });
        },
        handleImage(event) {
            const file = event.target.files && event.target.files[0];
            if (file) {
                this.imageFileName = file.name;
                this.$emit('upload-image', file);
            }
        },
        clearAndClose() {
            this.imageFileName = '';
            this.$emit('close');
        }
    },
    template: `
        <div class="modal-overlay" :class="{ show: visible }" @click.self="clearAndClose">
            <div class="modal">
                <h3>{{ editing ? '编辑商品' : '添加商品' }}</h3>
                <div class="form-group">
                    <label>商品名称 <span style="color:#e74c3c">*</span></label>
                    <input
                        class="form-input"
                        :value="product.name"
                        placeholder="输入商品名称"
                        @input="handleInput('name', $event)"
                    />
                </div>
                <div class="form-group">
                    <label>商品描述</label>
                    <input
                        class="form-input"
                        :value="product.description"
                        placeholder="输入商品描述（可选）"
                        @input="handleInput('description', $event)"
                    />
                </div>
                <div class="form-group">
                    <label>价格（元） <span style="color:#e74c3c">*</span></label>
                    <input
                        class="form-input"
                        type="number"
                        min="0.01"
                        step="0.01"
                        :value="product.price"
                        placeholder="输入价格"
                        @input="handleInput('price', $event)"
                    />
                </div>
                <div class="form-group">
                    <label>库存 <span style="color:#e74c3c">*</span></label>
                    <input
                        class="form-input"
                        type="number"
                        min="1"
                        step="1"
                        :value="product.stock"
                        placeholder="输入库存数量"
                        @input="handleInput('stock', $event)"
                    />
                </div>
                <div class="form-group">
                    <label>商品图片（可选）</label>
                    <label class="upload-picker">
                        <input class="upload-picker-input" type="file" accept="image/*" @change="handleImage" />
                        <span class="upload-picker-btn">选择图片</span>
                        <span class="upload-picker-name">{{ imageFileName || '建议使用方图，展示效果更佳' }}</span>
                    </label>
                    <div v-if="product.imageFileId" class="upload-picker-meta">
                        上传成功，文件 ID: {{ product.imageFileId }}
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="clearAndClose">取消</button>
                    <button class="btn btn-primary" @click="$emit('create')">{{ editing ? '保存' : '添加' }}</button>
                </div>
            </div>
        </div>
    `
};
