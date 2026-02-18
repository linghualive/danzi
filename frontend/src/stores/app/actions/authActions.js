export const authActions = {
    async doLogin() {
        this.loginError = '';
        if (!this.loginForm.username || !this.loginForm.password) {
            this.loginError = '请输入用户名和密码';
            return false;
        }

        this.loginLoading = true;
        try {
            const result = await this.api('POST', '/api/user/login', this.loginForm);
            if (result.code === 200 && result.data) {
                this.saveUser(result.data);
                this.addToast('登录成功', 'success');
                return true;
            }

            this.loginError = result.message || '登录失败';
            return false;
        } catch (error) {
            this.loginError = '网络错误，请检查后端服务';
            return false;
        } finally {
            this.loginLoading = false;
        }
    },

    async doRegister() {
        this.registerError = '';
        if (!this.registerForm.username || !this.registerForm.password || !this.registerForm.nickname) {
            this.registerError = '请填写所有字段';
            return false;
        }

        this.registerLoading = true;
        try {
            const registerResult = await this.api('POST', '/api/user/register', this.registerForm);
            if (registerResult.code !== 200) {
                this.registerError = registerResult.message || '注册失败';
                return false;
            }

            const loginResult = await this.api('POST', '/api/user/login', {
                username: this.registerForm.username,
                password: this.registerForm.password
            });

            if (loginResult.code === 200 && loginResult.data) {
                this.saveUser(loginResult.data);
                this.addToast('注册成功，已自动登录', 'success');
                return true;
            }

            this.registerError = '注册成功但登录失败，请手动登录';
            this.setAuthTab('login');
            return false;
        } catch (error) {
            this.registerError = '网络错误，请检查后端服务';
            return false;
        } finally {
            this.registerLoading = false;
        }
    }
};
