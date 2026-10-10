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

    let profileModalInitialized = false;

    function openProfileModal() {
        const modal = document.getElementById('user-profile-modal');
        const backdrop = document.getElementById('user-profile-backdrop');
        if (!modal || !backdrop) {
            window.location.assign('/login');
            return;
        }

        modal.classList.add('is-open');
        backdrop.classList.add('is-open');

        // Load profile
        loadUserProfileData();

        if (!profileModalInitialized) {
            initProfileModalListeners();
            profileModalInitialized = true;
        }
    }

    function closeProfileModal() {
        const modal = document.getElementById('user-profile-modal');
        const backdrop = document.getElementById('user-profile-backdrop');
        if (modal) modal.classList.remove('is-open');
        if (backdrop) backdrop.classList.remove('is-open');
    }

    async function loadUserProfileData() {
        const loading = document.getElementById('profile-info-loading');
        const body = document.getElementById('profile-info-body');
        if (loading) loading.classList.remove('hidden');
        if (body) body.classList.add('hidden');

        try {
            const res = await api.get('/api/v1/auth/me');
            if (res.ok && res.data) {
                const u = res.data;
                const idEl = document.getElementById('prof-university-id');
                if (idEl) idEl.textContent = u.universityId || '-';
                const roleEl = document.getElementById('prof-role-badge');
                if (roleEl) roleEl.innerHTML = badge(u.role || 'USER');
                const nameEl = document.getElementById('prof-full-name');
                if (nameEl) nameEl.textContent = u.fullName || '-';
                const emailEl = document.getElementById('prof-email');
                if (emailEl) emailEl.textContent = u.email || '-';
                const deptEl = document.getElementById('prof-department');
                if (deptEl) deptEl.textContent = u.department || 'ไม่ระบุ';
                const phoneEl = document.getElementById('prof-phone');
                if (phoneEl) phoneEl.textContent = u.phoneNumber || 'ไม่ระบุ';

                // If teacher, show teacher preference tab
                const teacherTabBtn = document.getElementById('tab-btn-teacher-pref');
                if (teacherTabBtn) {
                    if (u.role === 'TEACHER') {
                        teacherTabBtn.classList.remove('hidden');
                        loadTeacherPreferences();
                    } else {
                        teacherTabBtn.classList.add('hidden');
                    }
                }

                if (loading) loading.classList.add('hidden');
                if (body) body.classList.remove('hidden');
            } else {
                if (loading) loading.textContent = 'ไม่สามารถดึงข้อมูลผู้ใช้ได้ (กรุณาเข้าสู่ระบบ)';
            }
        } catch(e) {
            if (loading) loading.textContent = 'เกิดข้อผิดพลาดในการโหลดข้อมูล';
        }
    }

    async function loadTeacherPreferences() {
        const courseSelect = document.getElementById('pref-course-select');

        // 1. Load qualified courses (BR-06)
        try {
            let qualRes = await api.get('/api/v1/teacher/qualifications');
            if (!qualRes.ok) qualRes = await api.get('/api/v1/qualifications');
            if (qualRes.ok && Array.isArray(qualRes.data) && courseSelect) {
                courseSelect.innerHTML = '<option value="">-- เลือกรายวิชาที่สอนได้ --</option>';
                qualRes.data.forEach(q => {
                    const cId = q.id !== undefined ? q.id : (q.courseId !== undefined ? q.courseId : '');
                    const cCode = q.courseCode || '';
                    const cTitle = q.title || q.courseTitle || '';
                    const opt = document.createElement('option');
                    opt.value = cId;
                    opt.textContent = `${cCode} ${cTitle}`.trim();
                    courseSelect.appendChild(opt);
                });
            }
        } catch(e) {}

        // 2. Load current course preferences (D21)
        try {
            let prefRes = await api.get('/api/v1/teacher/preferences');
            if (!prefRes.ok) prefRes = await api.get('/api/v1/preferences');
            if (prefRes.ok && Array.isArray(prefRes.data)) {
                renderTeacherPrefList(prefRes.data);
            }
        } catch(e) {}

        // 3. Load interactive unavailable slots (D22, BR-07)
        loadTeacherAvailabilities();
    }

    function renderTeacherPrefList(prefs) {
        const container = document.getElementById('teacher-pref-list');
        if (!container) return;
        if (!prefs || prefs.length === 0) {
            container.innerHTML = '<div class="muted" style="font-size:0.85rem; padding:12px; background:var(--surface-alt); border-radius:6px; text-align:center;">ยังไม่ได้ระบุความประสงค์รายวิชา (สามารถเลือกวิชาและ Priority ด้านบน)</div>';
            return;
        }

        container.innerHTML = '';
        prefs.forEach(p => {
            const row = document.createElement('div');
            row.style.display = 'flex';
            row.style.justifyContent = 'space-between';
            row.style.alignItems = 'center';
            row.style.padding = '8px 12px';
            row.style.background = 'var(--surface)';
            row.style.border = '1px solid var(--border-subtle)';
            row.style.borderRadius = 'var(--radius-sm)';
            row.style.gap = '8px';

            const pNum = p.priority || 1;
            const stars = '⭐'.repeat(Math.max(1, 6 - pNum));

            row.innerHTML = `
                <div>
                    <div style="font-weight:600; font-size:0.88rem; color:var(--ink-navy);">
                        ${escapeHtml(p.courseCode || 'วิชา')} ${escapeHtml(p.courseTitle || '')}
                    </div>
                    <div class="muted" style="font-size:0.78rem;">
                        ลำดับความประสงค์: <span style="font-weight:600; color:var(--primary);">Priority ${pNum} (${stars})</span>
                    </div>
                </div>
                <div>
                    <button type="button" class="btn btn--danger btn--sm" style="padding:3px 8px; font-size:0.75rem;" onclick="ui.deleteTeacherPreference(${p.id})">
                        ลบ
                    </button>
                </div>
            `;
            container.appendChild(row);
        });
    }

    async function loadTeacherAvailabilities() {
        const gridBody = document.getElementById('teacher-availability-grid');
        if (!gridBody) return;

        try {
            let res = await api.get('/api/v1/teacher/availabilities');
            if (!res.ok) res = await api.get('/api/v1/availabilities');
            if (!res.ok || !Array.isArray(res.data)) {
                gridBody.innerHTML = '<tr><td colspan="6" class="muted">ไม่สามารถโหลดข้อมูลความพร้อมสอนได้</td></tr>';
                return;
            }

            const availabilities = res.data;
            const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

            const periods = [
                { label: 'ช่วงเช้า<br><span style="font-size:0.72rem; color:var(--ink-muted);">09:00 - 12:00 น.</span>', isMorning: true },
                { label: 'ช่วงบ่าย<br><span style="font-size:0.72rem; color:var(--ink-muted);">13:00 - 16:00 น.</span>', isMorning: false }
            ];

            gridBody.innerHTML = '';
            periods.forEach(period => {
                const tr = document.createElement('tr');
                const th = document.createElement('th');
                th.style.textAlign = 'left';
                th.style.background = 'var(--surface-alt)';
                th.style.padding = '8px 10px';
                th.innerHTML = period.label;
                tr.appendChild(th);

                days.forEach(day => {
                    const td = document.createElement('td');
                    td.style.padding = '6px';

                    // Find matching slot (3h standard slot or earliest slot for that period)
                    const slot = availabilities.find(a => {
                        if (a.dayOfWeek !== day) return false;
                        const startH = typeof a.startTime === 'string' ? parseInt(a.startTime.split(':')[0], 10) :
                                       (Array.isArray(a.startTime) ? a.startTime[0] : 9);
                        return period.isMorning ? (startH >= 8 && startH < 12) : (startH >= 12 && startH <= 16);
                    });

                    if (!slot) {
                        td.innerHTML = '<span class="muted" style="font-size:0.75rem;">-</span>';
                    } else {
                        const isAvail = slot.isAvailable !== false;
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.style.width = '100%';
                        btn.style.padding = '7px 4px';
                        btn.style.fontSize = '0.78rem';
                        btn.style.borderRadius = '6px';
                        btn.style.cursor = 'pointer';
                        btn.style.transition = 'all 0.15s ease';

                        if (isAvail) {
                            btn.style.background = '#ECFDF5';
                            btn.style.color = '#065F46';
                            btn.style.border = '1px solid #A7F3D0';
                            btn.style.fontWeight = '600';
                            btn.innerHTML = '✓ สะดวกสอน';
                            btn.title = 'คลิกเพื่อสลับเป็นไม่สะดวกสอน (Hard Constraint BR-07)';
                        } else {
                            btn.style.background = '#FEF2F2';
                            btn.style.color = '#991B1B';
                            btn.style.border = '1px solid #FECACA';
                            btn.style.fontWeight = '700';
                            btn.innerHTML = '✕ ไม่สะดวกสอน';
                            btn.title = 'คลิกเพื่อสลับเป็นสะดวกสอน';
                        }

                        btn.onclick = async () => {
                            btn.disabled = true;
                            const newAvail = !isAvail;
                            try {
                                let toggleRes = await api.put('/api/v1/teacher/availabilities', {
                                    timeSlotId: slot.timeSlotId,
                                    isAvailable: newAvail
                                });
                                if (!toggleRes.ok) {
                                    toggleRes = await api.put('/api/v1/availabilities', {
                                        timeSlotId: slot.timeSlotId,
                                        isAvailable: newAvail
                                    });
                                }
                                if (toggleRes.ok) {
                                    toast('success', newAvail ? 'บันทึกเป็นสะดวกสอนแล้ว' : 'บันทึกเป็นไม่สะดวกสอน (Hard Constraint BR-07) แล้ว');
                                    loadTeacherAvailabilities();
                                } else {
                                    toast('error', toggleRes.error.message || 'บันทึกไม่สำเร็จ');
                                    btn.disabled = false;
                                }
                            } catch (e) {
                                toast('error', 'เชื่อมต่อล้มเหลว');
                                btn.disabled = false;
                            }
                        };
                        td.appendChild(btn);
                    }
                    tr.appendChild(td);
                });
                gridBody.appendChild(tr);
            });
        } catch (e) {
            gridBody.innerHTML = '<tr><td colspan="6" class="muted">เกิดข้อผิดพลาดในการโหลด</td></tr>';
        }
    }

    async function deleteTeacherPreference(id) {
        if (!confirm('ยืนยันลบความประสงค์รายวิชานี้?')) return;
        let res = await api.del('/api/v1/teacher/preferences/' + id);
        if (!res.ok) res = await api.del('/api/v1/preferences/' + id);
        if (res.ok) {
            toast('success', 'ลบความประสงค์เรียบร้อยแล้ว');
            loadTeacherPreferences();
        } else {
            toast('error', res.error.message || 'ลบไม่สำเร็จ');
        }
    }

    function initProfileModalListeners() {
        const closeBtn = document.getElementById('btn-close-profile-modal');
        const footerCloseBtn = document.getElementById('btn-footer-close-profile');
        const backdrop = document.getElementById('user-profile-backdrop');

        if (closeBtn) closeBtn.onclick = closeProfileModal;
        if (footerCloseBtn) footerCloseBtn.onclick = closeProfileModal;
        if (backdrop) backdrop.onclick = closeProfileModal;

        // Tab Switching
        const tabs = document.querySelectorAll('#profile-modal-tabs .modal-tab-btn');
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                tabs.forEach(t => t.classList.remove('is-active'));
                tab.classList.add('is-active');

                document.querySelectorAll('#user-profile-modal .modal-tab-content').forEach(c => c.classList.add('hidden'));
                const targetId = tab.dataset.tab;
                const targetContent = document.getElementById(targetId);
                if (targetContent) targetContent.classList.remove('hidden');
            });
        });

        // Change Password Form
        const cpForm = document.getElementById('form-change-password');
        const cpError = document.getElementById('change-pwd-error');
        const cpSubmit = document.getElementById('btn-submit-change-pwd');
        if (cpForm) {
            cpForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                cpError.classList.add('hidden');
                const oldPwd = document.getElementById('cp-old-password').value;
                const newPwd = document.getElementById('cp-new-password').value;
                const confirmPwd = document.getElementById('cp-confirm-password').value;

                if (newPwd !== confirmPwd) {
                    cpError.textContent = 'รหัสผ่านใหม่และการยืนยันรหัสผ่านไม่ตรงกัน';
                    cpError.classList.remove('hidden');
                    return;
                }

                if (newPwd.length < 6) {
                    cpError.textContent = 'รหัสผ่านใหม่ต้องมีความยาวอย่างน้อย 6 ตัวอักษร';
                    cpError.classList.remove('hidden');
                    return;
                }

                setBusy(cpSubmit, true);
                try {
                    const res = await api.put('/api/v1/auth/change-password', {
                        oldPassword: oldPwd,
                        newPassword: newPwd
                    });
                    if (res.ok) {
                        toast('success', 'เปลี่ยนรหัสผ่านเรียบร้อยแล้ว');
                        cpForm.reset();
                    } else {
                        cpError.textContent = res.error.message || 'รหัสผ่านเดิมไม่ถูกต้อง';
                        cpError.classList.remove('hidden');
                    }
                } catch(err) {
                    cpError.textContent = 'ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้';
                    cpError.classList.remove('hidden');
                } finally {
                    setBusy(cpSubmit, false);
                }
            });
        }

        // Add Preference Form
        const prefForm = document.getElementById('form-add-preference');
        const prefSubmit = document.getElementById('btn-save-pref');
        if (prefForm) {
            prefForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const courseId = document.getElementById('pref-course-select').value;
                const priority = parseInt(document.getElementById('pref-score-select').value, 10);

                if (!courseId) {
                    toast('error', 'กรุณาเลือกรายวิชาที่สอนได้');
                    return;
                }

                setBusy(prefSubmit, true);
                try {
                    let res = await api.post('/api/v1/teacher/preferences', {
                        courseId: parseInt(courseId, 10),
                        priority: priority
                    });
                    if (!res.ok) {
                        res = await api.post('/api/v1/preferences', {
                            courseId: parseInt(courseId, 10),
                            priority: priority
                        });
                    }
                    if (res.ok) {
                        toast('success', 'บันทึกความประสงค์รายวิชาเรียบร้อยแล้ว');
                        loadTeacherPreferences();
                    } else {
                        toast('error', res.error.message || 'บันทึกไม่สำเร็จ');
                    }
                } catch(err) {
                    toast('error', 'เกิดข้อผิดพลาดในการเชื่อมต่อ');
                } finally {
                    setBusy(prefSubmit, false);
                }
            });
        }
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
        refreshUnreadCount: refreshUnreadCount,
        openProfileModal: openProfileModal,
        closeProfileModal: closeProfileModal,
        deleteTeacherPreference: deleteTeacherPreference,
        loadTeacherAvailabilities: loadTeacherAvailabilities
    };
})();
