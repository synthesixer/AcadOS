/*
 * AcadOS UI helpers — shared by every page. Owner: Track C.
 * Academic Operations Design System
 * Requires api.js to be loaded first.
 */
const ui = (() => {
    const BADGES = {
        ACTIVE: ['ok', '●', 'ACTIVE · เปิดสอน'],
        PUBLISHED: ['published', '✓', 'PUBLISHED · เผยแพร่แล้ว'],
        APPROVED: ['ok', '✓', 'APPROVED · อนุมัติแล้ว'],
        DRAFT: ['draft', '✎', 'DRAFT · รอตรวจ'],
        PENDING: ['pending', '⏳', 'PENDING · รอการตอบรับ'],
        ACCEPTED: ['info', '⇄', 'ACCEPTED · รอผู้ดูแลอนุมัติ'],
        REJECTED: ['danger', '✕', 'REJECTED · ปฏิเสธ'],
        CANCELLED: ['cancelled', '⊘', 'CANCELLED · ยกเลิกแล้ว'],
        CONFLICT: ['danger', '⚠', 'CONFLICT · เวลาชนกัน']
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
     * Resolves true when the user confirms. Use before destructive action fallback.
     * options: { confirmText, cancelText, danger }
     */
    function confirmDialog(message, options) {
        const opts = options || {};
        return new Promise((resolve) => {
            const dialog = document.createElement('dialog');
            dialog.className = 'card modal';
            dialog.style.maxWidth = '420px';
            dialog.innerHTML =
                '<h3 style="margin-bottom:12px; color:var(--ink-navy);">' + escapeHtml(opts.title || 'ยืนยันดำเนินการ') + '</h3>' +
                '<p style="color:var(--ink-text); margin-bottom:20px;">' + escapeHtml(message) + '</p>' +
                '<div style="display:flex; justify-content:flex-end; gap:8px;">' +
                '<button type="button" class="btn btn--secondary" value="cancel">' +
                escapeHtml(opts.cancelText || 'ยกเลิก') + '</button>' +
                '<button type="button" class="btn ' + (opts.danger === false ? 'btn--primary' : 'btn--danger') + '" value="confirm">' +
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

    /**
     * Decision Sheet (Signature AcadOS Impact Panel).
     * Slide-in impact confirmation drawer for impactful tasks (Publish, Discard, Cancel Section, Approve Swap).
     * options: { title, desc, impacts: [...], confirmText, confirmClass, onConfirm }
     */
    function openDecisionSheet(options) {
        const backdrop = document.getElementById('decision-backdrop');
        const sheet = document.getElementById('decision-sheet');
        if (!sheet || !backdrop) {
            return confirmDialog((options.title || '') + ': ' + (options.desc || ''), {
                title: options.title,
                confirmText: options.confirmText,
                danger: options.confirmClass === 'btn--danger'
            }).then(confirmed => {
                if (confirmed && options.onConfirm) options.onConfirm();
            });
        }

        const titleEl = document.getElementById('decision-sheet-title');
        const descEl = document.getElementById('decision-sheet-desc');
        const listEl = document.getElementById('decision-sheet-impacts');
        const confirmBtn = document.getElementById('decision-sheet-confirm');
        const cancelBtn = document.getElementById('decision-sheet-cancel');
        const closeBtn = document.getElementById('decision-sheet-close');

        if (titleEl) titleEl.textContent = options.title || 'ยืนยันการตัดสินใจ';
        if (descEl) descEl.textContent = options.desc || 'กรุณาตรวจสอบผลกระทบก่อนยืนยันดำเนินการ';
        if (listEl) {
            listEl.innerHTML = '';
            (options.impacts || ['ข้อมูลจะได้รับการบันทึกและประมวลผลทันที']).forEach(item => {
                const li = document.createElement('li');
                li.textContent = item;
                listEl.appendChild(li);
            });
        }
        if (confirmBtn) {
            confirmBtn.textContent = options.confirmText || 'ยืนยันดำเนินการ';
            confirmBtn.className = 'btn ' + (options.confirmClass || 'btn--primary');
        }

        const close = () => {
            sheet.classList.remove('is-open');
            backdrop.classList.remove('is-open');
            sheet.setAttribute('aria-hidden', 'true');
        };

        const handleConfirm = async () => {
            setBusy(confirmBtn, true);
            try {
                if (options.onConfirm) {
                    await options.onConfirm();
                }
                close();
            } catch (err) {
                toast('error', err.message || 'การดำเนินการล้มเหลว');
            } finally {
                setBusy(confirmBtn, false);
            }
        };

        backdrop.onclick = close;
        if (closeBtn) closeBtn.onclick = close;
        if (cancelBtn) cancelBtn.onclick = close;
        if (confirmBtn) confirmBtn.onclick = handleConfirm;

        backdrop.classList.add('is-open');
        sheet.classList.add('is-open');
        sheet.setAttribute('aria-hidden', 'false');
    }

    function renderLoading(el, lines) {
        const count = lines || 3;
        let html = '<div aria-busy="true" style="display:flex; flex-direction:column; gap:8px;"><span class="sr-only">กำลังโหลดข้อมูล</span>';
        for (let i = 0; i < count; i++) {
            html += '<div style="height:20px; background:#E2E8F0; border-radius:4px; opacity:0.6; animation:pulse 1.2s infinite ease-in-out;"></div>';
        }
        el.innerHTML = html + '</div>';
    }

    function renderEmpty(el, message) {
        el.innerHTML = '<div style="padding:28px; text-align:center; color:var(--ink-muted); font-size:0.92rem;">' + escapeHtml(message) + '</div>';
    }

    /** err is the error object returned by api.js: { code, message }. */
    function renderError(el, err) {
        const code = (err && err.code) || 'ERROR';
        const message = (err && err.message) || 'เกิดข้อผิดพลาด กรุณาลองใหม่อีกครั้ง';
        el.innerHTML =
            '<div style="padding:14px 18px; border-radius:8px; background:var(--danger-bg); color:var(--danger-text); border:1px solid #FECACA; display:flex; gap:10px; align-items:center;" role="alert">' +
            '<span class="mono" style="font-weight:700;">' + escapeHtml(code) + ':</span> ' +
            escapeHtml(message) +
            '</div>';
    }

    /** Returns badge HTML for a status code such as ACTIVE, DRAFT, PENDING, PUBLISHED. */
    function badge(status) {
        const entry = BADGES[status] || ['neutral', '●', status];
        return '<span class="badge badge--' + entry[0] + '" role="status">' +
               '<span class="badge__icon" aria-hidden="true" style="margin-right:4px;">' + entry[1] + '</span>' +
               '<span>' + escapeHtml(entry[2]) + '</span></span>';
    }

    /** Accepts an ISO date or date-time string. Returns e.g. "10 ต.ค. 2569" (Buddhist calendar). */
    function formatThaiDate(value, withTime) {
        if (!value) return '-';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return '-';
        const options = { day: 'numeric', month: 'short', year: 'numeric' };
        if (withTime) {
            options.hour = '2-digit';
            options.minute = '2-digit';
        }
        return date.toLocaleString('th-TH', options);
    }

    function paginate(el, pageData, onChange) {
        const page = pageData.page || 0;
        const totalPages = pageData.totalPages || 0;
        if (totalPages <= 1) {
            el.innerHTML = '';
            return;
        }
        el.innerHTML =
            '<nav style="display:flex; justify-content:space-between; align-items:center; padding:12px 0;" aria-label="เปลี่ยนหน้า">' +
            '<span class="muted" style="font-size:0.85rem;">หน้า <span class="num">' + (page + 1) + '</span> จาก ' +
            '<span class="num">' + totalPages + '</span> · ทั้งหมด <span class="num">' +
            (pageData.totalElements || 0) + '</span> รายการ</span>' +
            '<div style="display:flex; gap:8px;">' +
            '<button type="button" class="btn btn--secondary btn--sm" data-page="' + (page - 1) + '"' +
            (page <= 0 ? ' disabled' : '') + '>ก่อนหน้า</button>' +
            '<button type="button" class="btn btn--secondary btn--sm" data-page="' + (page + 1) + '"' +
            (page >= totalPages - 1 ? ' disabled' : '') + '>ถัดไป</button>' +
            '</div></nav>';
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

    function showFieldErrors(form, fieldErrors) {
        clearFieldErrors(form);
        (fieldErrors || []).forEach((fieldError) => {
            const input = form.querySelector('[name="' + fieldError.field + '"]');
            if (!input) return;
            input.classList.add('is-invalid');
            input.setAttribute('aria-invalid', 'true');
            const message = document.createElement('div');
            message.className = 'field__error';
            message.style.color = 'var(--danger-red)';
            message.style.fontSize = '0.8rem';
            message.style.marginTop = '4px';
            message.textContent = fieldError.message;
            input.insertAdjacentElement('afterend', message);
        });
    }

    function setBusy(button, busy) {
        if (!button) return;
        button.disabled = busy;
        button.setAttribute('aria-busy', busy ? 'true' : 'false');
    }

    // App shell behaviour
    function initShell() {
        const menuButton = document.getElementById('menu-toggle');
        const sidebar = document.getElementById('sidebar');
        const backdrop = document.getElementById('sidebar-backdrop');
        const closeNav = () => {
            if (sidebar) sidebar.classList.remove('is-open');
            if (backdrop) backdrop.classList.remove('is-open');
            if (menuButton) menuButton.setAttribute('aria-expanded', 'false');
        };
        if (menuButton) {
            menuButton.addEventListener('click', () => {
                const isOpen = sidebar && sidebar.classList.toggle('is-open');
                if (backdrop) backdrop.classList.toggle('is-open', isOpen);
                menuButton.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
            });
        }
        if (backdrop) backdrop.addEventListener('click', closeNav);
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') closeNav();
        });

        // Highlight active link
        const path = window.location.pathname;
        document.querySelectorAll('.sidebar__link').forEach((link) => {
            const href = link.getAttribute('href');
            if (href === path || (href !== '/' && path.startsWith(href + '/'))) {
                link.classList.add('is-active');
                link.setAttribute('aria-current', 'page');
            }
        });

        // Context strip toggle
        const contextToggle = document.getElementById('topbar-context-toggle');
        const contextStrip = document.getElementById('context-strip');
        const workbenchLayout = document.getElementById('workbench-layout');
        if (contextToggle && contextStrip) {
            const isCollapsed = localStorage.getItem('acados_context_collapsed') === 'true';
            if (isCollapsed) {
                contextStrip.classList.add('is-collapsed');
                if (workbenchLayout) workbenchLayout.classList.add('no-strip');
                contextToggle.classList.add('is-active');
            }
            contextToggle.addEventListener('click', () => {
                const collapsed = contextStrip.classList.toggle('is-collapsed');
                if (workbenchLayout) workbenchLayout.classList.toggle('no-strip', collapsed);
                contextToggle.classList.toggle('is-active', collapsed);
                localStorage.setItem('acados_context_collapsed', collapsed ? 'true' : 'false');
            });
        }

        // Client-side authentication sync fallback
        const clientRole = (typeof sessionStorage !== 'undefined') ? sessionStorage.getItem('acadosRole') : null;
        const clientBadge = document.getElementById('client-user-badge');
        const clientLogout = document.getElementById('client-logout-button');
        const guestLogin = document.getElementById('guest-login-btn');
        if (clientRole && clientBadge) {
            clientBadge.classList.remove('hidden');
            const roleEl = document.getElementById('client-user-role');
            if (roleEl) roleEl.textContent = clientRole;
            if (clientLogout) clientLogout.classList.remove('hidden');
            if (guestLogin) guestLogin.classList.add('hidden');
        }

        // Logout handling
        const handleLogout = async () => {
            sessionStorage.removeItem('acadosToken');
            sessionStorage.removeItem('acadosRole');
            document.cookie = 'acados_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 UTC; SameSite=Lax';
            try {
                await api.post('/api/v1/auth/logout', null, { redirectOn401: false });
            } catch(e) {}
            window.location.assign('/login');
        };

        const logoutButton = document.getElementById('logout-button');
        if (logoutButton) logoutButton.addEventListener('click', handleLogout);
        if (clientLogout) clientLogout.addEventListener('click', handleLogout);

        refreshUnreadCount();
    }

    function refreshUnreadCount() {
        const countEl = document.getElementById('unread-count');
        if (!countEl) return;
        api.get('/api/v1/notifications', { redirectOn401: false }).then((res) => {
            if (res.ok && Array.isArray(res.data)) {
                const unread = res.data.filter(n => !n.isRead).length;
                countEl.textContent = unread > 99 ? '99+' : String(unread);
                countEl.classList.toggle('hidden', unread === 0);
            }
        }).catch(() => {});
    }

    document.addEventListener('DOMContentLoaded', initShell);

    return {
        escapeHtml: escapeHtml,
        toast: toast,
        confirmDialog: confirmDialog,
        openDecisionSheet: openDecisionSheet,
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
