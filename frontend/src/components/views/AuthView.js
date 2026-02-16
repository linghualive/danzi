export default {
    name: 'AuthView',
    props: {
        authTab: {
            type: String,
            default: 'login'
        },
        loginForm: {
            type: Object,
            required: true
        },
        registerForm: {
            type: Object,
            required: true
        },
        loginError: {
            type: String,
            default: ''
        },
        registerError: {
            type: String,
            default: ''
        },
        loginLoading: {
            type: Boolean,
            default: false
        },
        registerLoading: {
            type: Boolean,
            default: false
        }
    },
    emits: [
        'set-auth-tab',
        'update-login-field',
        'update-register-field',
        'login',
        'register'
    ],
    methods: {
        updateLoginField(field, event) {
            this.$emit('update-login-field', {
                field,
                value: event.target.value.trim()
            });
        },
        updateRegisterField(field, event) {
            this.$emit('update-register-field', {
                field,
                value: event.target.value.trim()
            });
        }
    },
    template: `
        <div class="auth-wrapper">
            <div class="auth-card">
                <h2>Live Commerce</h2>
                <div class="auth-tabs">
                    <div
                        class="auth-tab"
                        :class="{ active: authTab === 'login' }"
                        @click="$emit('set-auth-tab', 'login')"
                    >
                        登录
                    </div>
                    <div
                        class="auth-tab"
                        :class="{ active: authTab === 'register' }"
                        @click="$emit('set-auth-tab', 'register')"
                    >
                        注册
                    </div>
                </div>

                <div v-show="authTab === 'login'">
                    <div class="form-group">
                        <label>用户名</label>
                        <input
                            class="form-input"
                            :value="loginForm.username"
                            placeholder="请输入用户名"
                            @input="updateLoginField('username', $event)"
                        />
                    </div>
                    <div class="form-group">
                        <label>密码</label>
                        <input
                            class="form-input"
                            type="password"
                            :value="loginForm.password"
                            placeholder="请输入密码"
                            @input="updateLoginField('password', $event)"
                            @keydown.enter="$emit('login')"
                        />
                    </div>
                    <div class="auth-error">{{ loginError }}</div>
                    <button class="btn btn-primary" :disabled="loginLoading" @click="$emit('login')">
                        {{ loginLoading ? '登录中...' : '登录' }}
                    </button>
                </div>

                <div v-show="authTab === 'register'">
                    <div class="form-group">
                        <label>用户名</label>
                        <input
                            class="form-input"
                            :value="registerForm.username"
                            placeholder="请输入用户名"
                            @input="updateRegisterField('username', $event)"
                        />
                    </div>
                    <div class="form-group">
                        <label>密码</label>
                        <input
                            class="form-input"
                            type="password"
                            :value="registerForm.password"
                            placeholder="请输入密码"
                            @input="updateRegisterField('password', $event)"
                        />
                    </div>
                    <div class="form-group">
                        <label>昵称</label>
                        <input
                            class="form-input"
                            :value="registerForm.nickname"
                            placeholder="请输入昵称"
                            @input="updateRegisterField('nickname', $event)"
                            @keydown.enter="$emit('register')"
                        />
                    </div>
                    <div class="auth-error">{{ registerError }}</div>
                    <button class="btn btn-primary" :disabled="registerLoading" @click="$emit('register')">
                        {{ registerLoading ? '注册中...' : '注册' }}
                    </button>
                </div>
            </div>
        </div>
    `
};
