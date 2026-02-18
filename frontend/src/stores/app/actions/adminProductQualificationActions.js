export const adminProductQualificationActions = {
    async loadAdminUsers() {
        if (!this.isAdmin) return;
        try {
            const result = await this.api('GET', '/api/admin/users?size=50');
            if (result.code === 200) {
                this.adminUsers = result.data?.content || [];
            }
        } catch (error) {
            this.adminUsers = [];
        }
    },

    async updateUserRole(userId, role) {
        try {
            const result = await this.api('PUT', `/api/admin/user/${userId}/role`, { role });
            if (result.code === 200) {
                this.addToast('角色已更新', 'success');
                this.loadAdminUsers();
            } else {
                this.addToast(result.message || '更新失败', 'error');
            }
        } catch (error) {
            this.addToast('更新失败', 'error');
        }
    },

    async loadAdminRooms() {
        if (!this.isAdmin) return;
        try {
            const result = await this.api('GET', '/api/admin/rooms?size=50');
            if (result.code === 200) {
                this.adminRooms = result.data?.content || [];
            }
        } catch (error) {
            this.adminRooms = [];
        }
    },

    async warnRoom(roomId) {
        const message = window.prompt('请输入警告内容：');
        if (!message) return;
        try {
            const result = await this.api('POST', `/api/admin/room/${roomId}/warn`, { message });
            if (result.code === 200) {
                this.addToast('警告已发送', 'success');
            } else {
                this.addToast(result.message || '警告失败', 'error');
            }
        } catch (error) {
            this.addToast('警告失败', 'error');
        }
    },

    async closeRoom(roomId) {
        const reason = window.prompt('请输入关闭原因：');
        if (!reason) return;
        try {
            const result = await this.api('PUT', `/api/admin/room/${roomId}/close`, { reason });
            if (result.code === 200) {
                this.addToast('直播间已关闭', 'success');
                this.loadAdminRooms();
            } else {
                this.addToast(result.message || '关闭失败', 'error');
            }
        } catch (error) {
            this.addToast('关闭失败', 'error');
        }
    },

    async unbanRoom(roomId) {
        if (!roomId) return;
        try {
            const result = await this.api('PUT', `/api/admin/room/${roomId}/unban`);
            if (result.code === 200) {
                this.addToast('直播间已解禁', 'success');
                this.loadAdminRooms();
            } else {
                this.addToast(result.message || '解禁失败', 'error');
            }
        } catch (error) {
            this.addToast('解禁失败', 'error');
        }
    },

    openAddProductModal() {
        this.editingProductId = null;
        this.newProduct = { name: '', description: '', price: '', stock: '', imageFileId: null };
        this.addProductModalVisible = true;
    },

    openEditProductModal(product) {
        this.editingProductId = product.id;
        this.newProduct = {
            name: product.name || product.productName || '',
            description: product.description || '',
            price: product.price != null ? String(product.price) : '',
            stock: product.stock != null ? String(product.stock) : '',
            imageFileId: product.imageFileId || null
        };
        this.addProductModalVisible = true;
    },

    updateNewProductField({ field, value }) {
        this.newProduct[field] = value;
    },

    async uploadProductImage(file) {
        try {
            const form = new FormData();
            form.append('file', file);
            const response = await fetch(`${this.apiBase}/api/product/media/upload`, {
                method: 'POST',
                headers: {
                    satoken: this.currentUser?.token || ''
                },
                body: form
            });
            const result = await response.json();
            if (result.code === 200) {
                this.newProduct.imageFileId = result.data.id;
                this.addToast('商品图片上传成功', 'success');
            } else {
                this.addToast(result.message || '商品图片上传失败', 'error');
            }
        } catch (error) {
            this.addToast('商品图片上传失败', 'error');
        }
    },

    async doSaveProduct() {
        if (!this.newProduct.name) {
            this.addToast('请输入商品名称', 'error');
            return false;
        }
        if (!this.newProduct.price || Number(this.newProduct.price) <= 0) {
            this.addToast('请输入有效价格', 'error');
            return false;
        }
        if (!this.newProduct.stock || Number(this.newProduct.stock) <= 0) {
            this.addToast('请输入有效库存', 'error');
            return false;
        }

        const isEditing = !!this.editingProductId;

        try {
            const body = {
                name: this.newProduct.name.trim(),
                price: Number(this.newProduct.price),
                stock: Number(this.newProduct.stock)
            };
            if (this.newProduct.description) {
                body.description = this.newProduct.description.trim();
            }
            if (!isEditing) {
                body.roomId = this.currentRoomId;
            }
            if (this.newProduct.imageFileId) {
                body.imageFileId = this.newProduct.imageFileId;
            }

            const method = isEditing ? 'PUT' : 'POST';
            const path = isEditing ? `/api/product/${this.editingProductId}` : '/api/product';
            const result = await this.api(method, path, body);
            if (result.code === 200) {
                this.addProductModalVisible = false;
                this.addToast(isEditing ? '商品已更新' : '商品添加成功', 'success');
                await this.loadRoomProducts();
                return true;
            }

            this.addToast(result.message || (isEditing ? '更新失败' : '添加失败'), 'error');
            return false;
        } catch (error) {
            this.addToast('网络错误', 'error');
            return false;
        }
    },

    async updateUserStatus(userId, status) {
        try {
            const result = await this.api('PUT', `/api/admin/user/${userId}/status`, { status });
            if (result.code === 200) {
                this.addToast(status === 1 ? '用户已禁用' : '用户已启用', 'success');
                this.loadAdminUsers();
            } else {
                this.addToast(result.message || '操作失败', 'error');
            }
        } catch (error) {
            this.addToast('操作失败', 'error');
        }
    },

    async submitQualification() {
        try {
            const result = await this.api('POST', '/api/broadcast/qualification', this.qualificationForm);
            if (result.code === 200) {
                this.myQualification = result.data;
                this.qualificationForm = { contactInfo: '', businessLicense: '', personalInfo: '' };
                this.addToast('申请已提交，请等待管理员审核', 'success');
            } else {
                this.addToast(result.message || '提交失败', 'error');
            }
        } catch (error) {
            this.addToast('提交失败', 'error');
        }
    },

    async loadMyQualification() {
        try {
            const result = await this.api('GET', '/api/broadcast/qualification/me');
            if (result.code === 200) {
                this.myQualification = result.data;
            }
        } catch (error) {
            // may not have applied yet
        }
    },

    async loadAdminQualifications() {
        if (!this.isAdmin) return;
        try {
            const result = await this.api('GET', '/api/admin/qualifications');
            if (result.code === 200) {
                this.adminQualifications = Array.isArray(result.data) ? result.data : [];
            }
        } catch (error) {
            this.adminQualifications = [];
        }
    },

    async reviewQualification(id, status, rejectReason) {
        try {
            const result = await this.api('PUT', `/api/admin/qualification/${id}/review`, { status, rejectReason: rejectReason || '' });
            if (result.code === 200) {
                this.addToast(status === 1 ? '已通过' : '已拒绝', 'success');
                this.loadAdminQualifications();
            } else {
                this.addToast(result.message || '审核失败', 'error');
            }
        } catch (error) {
            this.addToast('审核失败', 'error');
        }
    },

    async doDeleteProduct(productId) {
        try {
            const result = await this.api('DELETE', `/api/product/${productId}`);
            if (result.code === 200) {
                this.addToast('商品已删除', 'success');
                await this.loadRoomProducts();
            } else {
                this.addToast(result.message || '删除失败', 'error');
            }
        } catch (error) {
            this.addToast('网络错误', 'error');
        }
    }
};
