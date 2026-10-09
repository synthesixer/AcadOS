/*
 * AcadOS REST client — shared by every page. Owner: Track C. Do not edit from other tracks.
 *
 * Usage:
 *   const res = await api.get('/api/v1/courses?page=0&size=10');
 *   if (res.ok) { render(res.data); } else { ui.renderError(el, res.error); }
 *
 * Every call resolves (never rejects) to { ok, status, data, error }.
 * error follows ErrorResponse: { status, code, message, fieldErrors: [{ field, message }] }.
 */
const api = (() => {
    const LOGIN_URL = '/login';

    async function parseBody(response) {
        if (response.status === 204) {
            return null;
        }
        const text = await response.text();
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch (e) {
            return null;
        }
    }

    function toError(status, body) {
        return {
            status: status,
            code: (body && body.code) || 'HTTP_' + status,
            message: (body && body.message) || 'เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง',
            fieldErrors: (body && body.fieldErrors) || []
        };
    }

    /**
     * options.redirectOn401 = false keeps the user on the page when the session is missing
     * (used by background calls such as the unread notification count).
     */
    async function request(method, url, body, options) {
        const redirectOn401 = !options || options.redirectOn401 !== false;
        const init = {
            method: method,
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        };
        if (body !== undefined && body !== null) {
            init.headers['Content-Type'] = 'application/json';
            init.body = JSON.stringify(body);
        }

        let response;
        try {
            response = await fetch(url, init);
        } catch (e) {
            return {
                ok: false,
                status: 0,
                data: null,
                error: toError(0, { code: 'NETWORK_ERROR', message: 'เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ กรุณาตรวจสอบเครือข่าย' })
            };
        }

        const data = await parseBody(response);
        if (response.ok) {
            return { ok: true, status: response.status, data: data, error: null };
        }
        if (response.status === 401 && redirectOn401 && window.location.pathname !== LOGIN_URL) {
            window.location.assign(LOGIN_URL);
        }
        return { ok: false, status: response.status, data: null, error: toError(response.status, data) };
    }

    return {
        get: (url, options) => request('GET', url, null, options),
        post: (url, body, options) => request('POST', url, body, options),
        put: (url, body, options) => request('PUT', url, body, options),
        del: (url, options) => request('DELETE', url, null, options)
    };
})();
