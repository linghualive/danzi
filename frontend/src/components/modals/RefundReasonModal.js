export default {
    name: 'RefundReasonModal',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        value: {
            type: String,
            default: ''
        }
    },
    emits: ['close', 'update-value', 'submit'],
    computed: {
        trimmedLength() {
            return (this.value || '').trim().length;
        }
    },
    methods: {
        onInput(event) {
            const nextValue = (event.target.value || '').slice(0, 120);
            this.$emit('update-value', nextValue);
        },
        submit() {
            if (!this.trimmedLength) {
                return;
            }
            this.$emit('submit');
        }
    },
    template: `
        <div class="modal-overlay" :class="{ show: visible }" @click.self="$emit('close')">
            <div class="modal refund-modal">
                <h3>申请退款</h3>
                <p class="refund-modal-tip">请填写退款理由，提交后将由卖家进行确认。</p>
                <div class="form-group">
                    <label>退款理由</label>
                    <textarea
                        class="form-input refund-reason-input"
                        rows="4"
                        :value="value"
                        placeholder="例如：商品描述不符、尺寸不合适、质量问题..."
                        @input="onInput"
                    ></textarea>
                    <div class="refund-reason-meta">{{ trimmedLength }}/120</div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="$emit('close')">取消</button>
                    <button class="btn btn-danger" :disabled="!trimmedLength" @click="submit">提交申请</button>
                </div>
            </div>
        </div>
    `
};
