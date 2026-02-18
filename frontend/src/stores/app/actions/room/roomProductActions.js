export const roomProductActions = {
    async loadRoomProducts(silent = false) {
        if (!this.currentRoomId) {
            return;
        }

        if (!silent) {
            this.productsLoading = true;
        }

        try {
            const roomResult = await this.api('GET', `/api/product/room/${this.currentRoomId}`);
            if (roomResult.code === 200 && Array.isArray(roomResult.data)) {
                this.roomProducts = roomResult.data.map((item) => ({
                    ...item,
                    imageUrl: this.fullMediaUrl(item.imageUrl)
                }));

                if (this.currentProduct?.id) {
                    const latestProduct = this.roomProducts.find((item) => item.id === this.currentProduct.id);
                    if (latestProduct) {
                        this.currentProduct = {
                            ...this.currentProduct,
                            stock: latestProduct.stock,
                            status: latestProduct.status,
                            imageUrl: latestProduct.imageUrl
                        };
                    }
                }
                return;
            }

            if (!silent) {
                this.roomProducts = [];
            }
        } catch (error) {
            if (!silent) {
                this.roomProducts = [];
            }
            console.warn('加载直播间商品失败', error);
        } finally {
            if (!silent) {
                this.productsLoading = false;
            }
        }
    },

    async showProductDetail(product) {
        this.currentProduct = product;
        this.productQty = 1;
        this.productDetailModalVisible = true;

        try {
            const result = await this.api('GET', `/api/product/${product.id}`);
            if (result.code === 200 && result.data) {
                this.currentProduct = {
                    ...result.data,
                    imageUrl: this.fullMediaUrl(result.data.imageUrl)
                };
            }
        } catch (error) {
            console.warn('获取商品详情失败', error);
        }
    },

    changeQty(delta) {
        let quantity = Number(this.productQty) || 1;
        quantity = Math.max(1, quantity + delta);
        if (this.currentProduct?.stock !== undefined && this.currentProduct?.stock !== null) {
            quantity = Math.min(quantity, this.currentProduct.stock);
        }
        this.productQty = quantity;
    },

    updateQty(value) {
        let quantity = Number(value) || 1;
        quantity = Math.max(1, quantity);
        if (this.currentProduct?.stock !== undefined && this.currentProduct?.stock !== null) {
            quantity = Math.min(quantity, this.currentProduct.stock);
        }
        this.productQty = quantity;
    },

    async buyFromDetail() {
        if (!this.currentProduct?.id) {
            return;
        }
        await this.createOrder(this.currentProduct.id, this.productQty || 1);
        this.productDetailModalVisible = false;
    },

    async quickBuy(productId) {
        await this.createOrder(productId, 1);
    }
};
