/*
 * AcadOS UI helpers — shared by every page. Owner: Track C. Do not edit from other tracks.
 * Requires api.js to be loaded first.
 */
const ui = (() => {
    const BADGES = {
        ACTIVE: ['ok', 'เปิดสอน'],
        PUBLISHED: ['ok', 'เผยแพร่แล้ว'],
        APPROVED: ['ok', 'อนุมัติแล้ว'],
        DRAFT: ['warn', 'ฉบับร่าง'],
        PENDING: ['warn', 'รอตอบรับ'],
        ACCEPTED: ['info', 'รออนุมัติ'],
        REJECTED: ['danger', 'ปฏิเสธ'],
        CANCELLED: ['neutral', 'ยกเลิก']
    };

    function escapeHtml(value) {
        return String(value === null || value === undefined ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    /** type: success | error | warn | info */
    function toast(type, message) {
        let region = document.getElementById('toast-region');
        if (!region) {
            region = document.createElement('div');
            region.id = 'toast-region';
            region.className = 'toast-region';
            region.setAttribute('role', 'status');
            region.setAttribute('aria-live', 'polite');
            document.body.appendChild(region);
        }
        const item = document.createElement('div');
        item.className = 'toast toast--' + type;
        item.textContent = message;
        region.appendChild(item);
        setTimeout(() => item.remove(), type === 'error' ? 7000 : 4000);
    }

    /**
     * Resolves true when the user confirms. Use before every destructive action.
     * options: { confirmText, cancelText, danger }
     */
    function confirmDialog(message, options) {
        const opts = options || {};
        return new Promise((resolve) => {
            const dialog = document.createElement('dialog');
            dialog.className = 'modal';
            dialog.innerHTML =
                '<p>' + escapeHtml(message) + '</p>' +
                '<div class="modal__actions">' +
                '<button type="button" class="btn btn--secondary" value="cancel">' +
                escapeHtml(opts.cancelText || 'ยกเลิก') + '</button>' +
                '<button type="button" class="btn ' + (opts.danger === false ? '' : 'btn--danger') + '" value="confirm">' +
                escapeHtml(opts.confirmText || 'ยืนยัน') + '</button>' +
                '</div>';
            dialog.addEventListener('click', (event) => {
                if (event.target instanceof HTMLButtonElement) {
                    dialog.close(event.target.value);
                }
            });
            dialog.addEventListener('close', () => {
                dialog.remove();
                resolve(dialog.returnValue === 'confirm');
            });
            document.body.appendChild(dialog);
            dialog.showModal();
        });
    }

    function renderLoading(el, lines) {
        const count = lines || 4;
        let html = '<div aria-busy="true"><span class="sr-only">กำลังโหลดข้อมูล</span>';
        for (let i = 0; i < count; i++) {
            html += '<div class="skeleton"></div>';
        }
        el.innerHTML = html + '</div>';
    }

    function renderEmpty(el, message) {
        el.innerHTML = '<div class="state">' + escapeHtml(message) + '</div>';
    }

    /** err is the error object returned by api.js: { code, message }. */
    function renderError(el, err) {
        const code = (err && err.code) || 'ERROR';
        const message = (err && err.message) || 'เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง';
        el.innerHTML =
            '<div class="state state--error" role="alert">' +
            '<span class="state__code">' + escapeHtml(code) + '</span>' +
            escapeHtml(message) +
            '</div>';
    }

    /** Returns badge HTML for a status code such as ACTIVE, DRAFT, PENDING. */
    function badge(status) {
        const entry = BADGES[status] || ['neutral', status];
        return '<span class="badge badge--' + entry[0] + '">' + escapeHtml(entry[1]) + '</span>';
    }

    /** Accepts an ISO date or date-time string. Returns e.g. "10 ต.ค. 2569" (Buddhist calendar). */
    function formatThaiDate(value, withTime) {
        if (!value) {
            return '-';
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '-';
        }
        const options = { day: 'numeric', month: 'short', year: 'numeric' };
        if (withTime) {
            options.hour = '2-digit';
            options.minute = '2-digit';
        }
        return date.toLocaleString('th-TH', options);
    }

    /**
     * Renders previous/next controls for a PageResponse { page, size, totalElements, totalPages }.
     * onChange receives the zero-based page number to load.
     */
    function paginate(el, pageData, onChange) {
        const page = pageData.page || 0;
        const totalPages = pageData.totalPages || 0;
        if (totalPages <= 1) {
            el.innerHTML = '';
            return;
        }
        el.innerHTML =
            '<nav class="pagination" aria-label="เปลี่ยนหน้า">' +
            '<span class="pagination__info">หน้า <span class="num">' + (page + 1) + '</span> จาก ' +
            '<span class="num">' + totalPages + '</span> · ทั้งหมด <span class="num">' +
            (pageData.totalElements || 0) + '</span> รายการ</span>' +
            '<span class="toolbar">' +
            '<button type="button" class="btn btn--secondary btn--sm" data-page="' + (page - 1) + '"' +
            (page <= 0 ? ' disabled' : '') + '>ก่อนหน้า</button>' +
            '<button type="button" class="btn btn--secondary btn--sm" data-page="' + (page + 1) + '"' +
            (page >= totalPages - 1 ? ' disabled' : '') + '>ถัดไป</button>' +
            '</span></nav>';
        el.querySelectorAll('button[data-page]').forEach((button) => {
            button.addEventListener('click', () => onChange(Number(button.dataset.page)));
        });
    }

    function clearFieldErrors(form) {
        form.querySelectorAll('.field__error').forEach((node) => node.remove());
        form.querySelectorAll('.is-invalid').forEach((node) => {
            node.classList.remove('is-invalid');
            node.removeAttribute('aria-invalid');
        });
    }

    /** Shows server fieldErrors [{ field, message }] under the input whose name matches. */
    function showFieldErrors(form, fieldErrors) {
        clearFieldErrors(form);
        (fieldErrors || []).forEach((fieldError) => {
            const input = form.querySelector('[name="' + fieldError.field + '"]');
            if (!input) {
                return;
            }
            input.classList.add('is-invalid');
            input.setAttribute('aria-invalid', 'true');
            const message = document.createElement('div');
            message.className = 'field__error';
            message.textContent = fieldError.message;
            input.insertAdjacentElement('afterend', message);
        });
    }

    /** Disables a submit button while a request is in flight. */
    function setBusy(button, busy) {
        button.disabled = busy;
        button.setAttribute('aria-busy', busy ? 'true' : 'false');
    }

    // ----- App shell behaviour (sidebar drawer, active link, user name, unread count, logout) -----

    function initShell() {
        const menuButton = document.getElementById('menu-toggle');
        const backdrop = document.getElementById('sidebar-backdrop');
        const closeNav = () => {
            document.body.classList.remove('nav-open');
            if (menuButton) {
                menuButton.setAttribute('aria-expanded', 'false');
            }
        };
        if (menuButton) {
            menuButton.addEventListener('click', () => {
                const open = document.body.classList.toggle('nav-open');
                menuButton.setAttribute('aria-expanded', open ? 'true' : 'false');
            });
        }
        if (backdrop) {
            backdrop.addEventListener('click', closeNav);
        }
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                closeNav();
            }
        });

        const path = window.location.pathname;
        document.querySelectorAll('.sidebar__link').forEach((link) => {
            const href = link.getAttribute('href');
            if (href === path || (href !== '/' && path.startsWith(href + '/'))) {
                link.classList.add('is-active');
                link.setAttribute('aria-current', 'page');
            }
        });

        const logoutButton = document.getElementById('logout-button');
        if (logoutButton) {
            logoutButton.addEventListener('click', async () => {
                await api.post('/api/v1/auth/logout', null, { redirectOn401: false });
                window.location.assign('/login');
            });
        }

        const nameEl = document.getElementById('current-user-name');
        if (nameEl) {
            api.get('/api/v1/auth/me', { redirectOn401: false }).then((res) => {
                if (res.ok && res.data && res.data.fullName) {
                    nameEl.textContent = res.data.fullName;
                }
            });
        }

        refreshUnreadCount();
    }

    /** Call after marking notifications as read to update the bell in the topbar. */
    function refreshUnreadCount() {
        const countEl = document.getElementById('unread-count');
        if (!countEl) {
            return;
        }
        api.get('/api/v1/notifications/unread-count', { redirectOn401: false }).then((res) => {
            const count = res.ok && res.data ? Number(res.data.count) || 0 : 0;
            countEl.textContent = count > 99 ? '99+' : String(count);
            countEl.classList.toggle('hidden', count === 0);
        });
    }

    document.addEventListener('DOMContentLoaded', initShell);

    return {
        escapeHtml: escapeHtml,
        toast: toast,
        confirmDialog: confirmDialog,
        renderLoading: renderLoading,
        renderEmpty: renderEmpty,
        renderError: renderError,
        badge: badge,
        formatThaiDate: formatThaiDate,
        paginate: paginate,
        showFieldErrors: showFieldErrors,
        clearFieldErrors: clearFieldErrors,
        setBusy: setBusy,
        refreshUnreadCount: refreshUnreadCount
    };
})();
