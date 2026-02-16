import { formatPrice } from '../../utils/format.js';

export default {
    name: 'ProductDetailModal',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        product: {
            type: Object,
            default: null
        },
        qty: {
            type: Number,
            default: 1
        }
    },
    emits: ['close', 'change-qty', 'update-qty', 'buy'],
    methods: {
        formatPrice,
        onQtyInput(event) {
            const value = Number(event.target.value);
            this.$emit('update-qty', Number.isNaN(value) ? 1 : value);
        }
    },
    template: `
        <div class="modal-overlay product-detail-modal" :class="{ show: visible }" @click.self="$emit('close')">
            <div class="modal">
                <div class="pd-header">
                    <div class="pd-image">&#128230;</div>
                    <div class="pd-info">
                        <h4>{{ product?.name || product?.productName || '-' }}</h4>
                        <div class="pd-price">&yen;{{ formatPrice(product?.price) }}</div>
                        <div class="pd-stock">库存: {{ product?.stock ?? '-' }}</div>
                    </div>
                </div>
                <div class="pd-desc">{{ product?.description || product?.desc || '暂无描述' }}</div>
                <div class="qty-row">
                    <label>购买数量：</label>
                    <div class="qty-control">
                        <button @click="$emit('change-qty', -1)">-</button>
                        <input type="number" :value="qty" min="1" @input="onQtyInput" />
                        <button @click="$emit('change-qty', 1)">+</button>
                    </div>
                </div>
                <div class="modal-actions">
                    <button class="btn btn-outline" @click="$emit('close')">取消</button>
                    <button class="btn btn-primary" @click="$emit('buy')">立即购买</button>
                </div>
            </div>
        </div>
    `
};
