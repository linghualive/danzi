export async function request({ baseUrl, token, method, path, body }) {
    const headers = {
        'Content-Type': 'application/json'
    };

    if (token) {
        headers.satoken = token;
    }

    const options = {
        method,
        headers
    };

    if (body !== undefined) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(baseUrl + path, options);
    const text = await response.text();

    if (!text) {
        return {
            code: response.status,
            message: response.ok ? 'success' : '请求失败'
        };
    }

    try {
        return JSON.parse(text);
    } catch (error) {
        return {
            code: response.status,
            message: text
        };
    }
}
