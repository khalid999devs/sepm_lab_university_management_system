import { request, write, refreshCsrf, showNotice } from './api.js';

const $ = selector => document.querySelector(selector);
const collections = { admins: 'Admins', teachers: 'Teachers', students: 'Students' };
const dialog = $('#account-dialog');
const deleteDialog = $('#delete-dialog');
const form = $('#account-form');
let collection;
let editing;
let deleting;
let busy = false;
let loadVersion = 0;
let currentAccount;

function handleError(error, element = $('#page-status')) {
    if (error.status === 401) {
        location.replace('/login.html?expired=1');
        return;
    }
    showNotice(element, error.message, 'error');
}

async function identity() {
    const me = await request('/api/auth/me');
    currentAccount = me;
    $('#identity').textContent = me.fullName;
    $('#role').textContent = me.role.charAt(0) + me.role.slice(1).toLowerCase();
    if (me.role !== 'ADMIN') {
        $('#dashboard-role').textContent = `${me.role} DASHBOARD`;
        $('#profile-name').textContent = me.fullName;
        $('#profile-email').textContent = me.email;
        $('#profile-role').textContent = $('#role').textContent;
    }
    return me;
}

async function loadAccounts(clearNotice = true) {
    const version = ++loadVersion;
    const current = collection;
    if (clearNotice) showNotice($('#page-status'), '');
    $('#record-count').textContent = 'Loading…';
    $('#refresh').disabled = true;
    $('#account-rows').replaceChildren();
    $('#empty-state').hidden = true;
    try {
        const records = await request(`/api/${current}`);
        if (version !== loadVersion) return;
        for (const record of records) {
            const row = document.createElement('tr');
            for (const value of [record.fullName, record.email]) {
                const cell = document.createElement('td');
                cell.textContent = value;
                row.append(cell);
            }
            const cell = document.createElement('td');
            const actions = document.createElement('div');
            actions.className = 'row-actions';
            for (const [label, callback] of [['Edit', () => openForm(record)], ['Delete', () => openDelete(record)]]) {
                const button = document.createElement('button');
                button.type = 'button';
                button.textContent = label;
                button.className = `text-button ${label === 'Delete' ? 'danger-text' : ''}`;
                button.setAttribute('aria-label', `${label} ${record.fullName}`);
                button.addEventListener('click', callback);
                actions.append(button);
            }
            cell.append(actions);
            row.append(cell);
            $('#account-rows').append(row);
        }
        $('#record-count').textContent = `${records.length} ${records.length === 1 ? 'account' : 'accounts'}`;
        $('#empty-state').hidden = records.length > 0;
    } catch (error) {
        if (version !== loadVersion) return;
        $('#record-count').textContent = 'Unable to load accounts';
        handleError(error);
    } finally {
        if (version === loadVersion) $('#refresh').disabled = false;
    }
}

function selectCollection(key) {
    collection = key;
    for (const button of $('#resource-tabs').children) {
        if (button.dataset.collection === key) button.setAttribute('aria-current', 'page');
        else button.removeAttribute('aria-current');
    }
    $('#collection-title').textContent = collections[key];
    loadAccounts();
}

function clearFieldErrors() {
    for (const field of ['fullName', 'email', 'password']) {
        form.elements[field].removeAttribute('aria-invalid');
        $(`#${field}-error`).hidden = true;
    }
    showNotice($('#form-status'), '');
}

function openForm(account) {
    editing = account;
    form.reset();
    clearFieldErrors();
    $('#form-title').textContent = currentAccount.role !== 'ADMIN' ? 'Edit my profile' : account ? 'Edit account' : 'Add account';
    $('#form-description').textContent = currentAccount.role !== 'ADMIN' ? 'Update your personal details.' : `Collection: ${collections[collection]}`;
    form.elements.fullName.value = account?.fullName || '';
    form.elements.email.value = account?.email || '';
    form.elements.password.required = !account;
    $('#password-label').textContent = account ? 'New password (optional)' : 'Password';
    $('#password-hint').textContent = account ? 'Leave blank to keep the current password.' : 'At least 8 characters, up to 72 UTF-8 bytes.';
    dialog.showModal();
}

function openDelete(account) {
    deleting = account;
    $('#delete-description').textContent = `Delete ${account.fullName} (${account.email})? This account will lose access.`;
    showNotice($('#delete-status'), '');
    deleteDialog.showModal();
}

function setBusy(value) {
    busy = value;
    for (const selector of ['#save-account', '#cancel-account', '#confirm-delete', '#cancel-delete']) $(selector).disabled = value;
    $('#save-account').textContent = value ? 'Saving…' : 'Save account';
    $('#confirm-delete').textContent = value ? 'Deleting…' : 'Delete account';
}

for (const modal of [dialog, deleteDialog]) {
    modal.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
}
$('#add').addEventListener('click', () => openForm());
$('#edit-profile').addEventListener('click', () => openForm(currentAccount));
$('#refresh').addEventListener('click', () => loadAccounts());
$('#cancel-account').addEventListener('click', () => dialog.close());
$('#cancel-delete').addEventListener('click', () => deleteDialog.close());

form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy) return;
    clearFieldErrors();
    const body = { fullName: form.elements.fullName.value, email: form.elements.email.value };
    const password = form.elements.password.value;
    if (password) body.password = password;
    setBusy(true);
    try {
        const path = `/api/${collection}${editing ? `/${editing.id}` : ''}`;
        await write(path, editing ? 'PUT' : 'POST', body);
        dialog.close();
        await identity();
        showNotice($('#page-status'), editing ? 'Account updated.' : 'Account created.', 'success');
        if (currentAccount.role === 'ADMIN') await loadAccounts(false);
    } catch (error) {
        handleError(error, dialog.open ? $('#form-status') : $('#page-status'));
        for (const [field, message] of Object.entries(error.fields || {})) {
            if (!form.elements[field]) continue;
            form.elements[field].setAttribute('aria-invalid', 'true');
            $(`#${field}-error`).textContent = message;
            $(`#${field}-error`).hidden = false;
        }
    } finally { setBusy(false); }
});

$('#confirm-delete').addEventListener('click', async () => {
    if (busy) return;
    setBusy(true);
    try {
        await request(`/api/${collection}/${deleting.id}`, { method: 'DELETE' });
        deleteDialog.close();
        await identity();
        showNotice($('#page-status'), 'Account deleted.', 'success');
        await loadAccounts(false);
    } catch (error) { handleError(error, deleteDialog.open ? $('#delete-status') : $('#page-status')); }
    finally { setBusy(false); }
});

$('#logout').addEventListener('click', async () => {
    $('#logout').disabled = true;
    try {
        await request('/api/auth/logout', { method: 'POST' });
        location.replace('/login.html');
    } catch (error) { handleError(error); $('#logout').disabled = false; }
});

try {
    const me = await identity();
    await refreshCsrf();
    $('#session').hidden = false;
    if (me.role !== 'ADMIN') {
        collection = me.role === 'TEACHER' ? 'teachers' : 'students';
        $('#profile-section').hidden = false;
        showNotice($('#page-status'), '');
    } else {
        for (const key of Object.keys(collections)) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'tab';
            button.dataset.collection = key;
            button.textContent = collections[key];
            button.addEventListener('click', () => selectCollection(key));
            $('#resource-tabs').append(button);
        }
        $('#accounts-section').hidden = false;
        selectCollection('admins');
    }
} catch (error) { handleError(error); }
