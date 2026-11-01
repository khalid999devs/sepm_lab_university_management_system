import { showNotice } from './api.js';

export const $ = selector => document.querySelector(selector);
export function element(tag, text, className) {
    const node = document.createElement(tag);
    if (text != null) node.textContent = text;
    if (className) node.className = className;
    return node;
}
export function button(label, action, className = 'text-button') {
    const node = element('button', label, className);
    node.type = 'button';
    node.addEventListener('click', action);
    return node;
}
export function actions(...children) {
    const node = element('div', null, 'row-actions');
    node.append(...children);
    return node;
}
export function titleCell(title, subtitle) {
    const node = element('div', null, 'cell-title');
    node.append(element('strong', title), element('span', subtitle, 'muted small'));
    return node;
}
export function empty(text) {
    const node = element('div', null, 'empty-state');
    node.append(element('p', text, 'muted'));
    return node;
}
export function table(target, caption, headers, rows, emptyText) {
    target.replaceChildren();
    if (!rows.length) { target.append(empty(emptyText)); return; }
    const scroll = element('div', null, 'table-scroll');
    const node = element('table');
    node.append(element('caption', caption, 'sr-only'));
    const head = element('thead'), headRow = element('tr'), body = element('tbody');
    for (const label of headers) {
        const cell = element('th', label);
        cell.scope = 'col';
        if (label === 'Actions') cell.className = 'actions-heading';
        headRow.append(cell);
    }
    head.append(headRow);
    for (const values of rows) {
        const row = element('tr');
        for (const value of values) {
            const cell = element('td');
            if (value instanceof Node) cell.append(value);
            else cell.textContent = value;
            row.append(cell);
        }
        body.append(row);
    }
    node.append(head, body); scroll.append(node); target.append(scroll);
}
export function matches(query, ...values) {
    return values.join(' ').toLocaleLowerCase().includes(query.trim().toLocaleLowerCase());
}
export function date(value) {
    return new Intl.DateTimeFormat(undefined, { year: 'numeric', month: 'short', day: 'numeric' }).format(new Date(value));
}

export function createDialogs(onError) {
    const dialog = $('#editor-dialog'), form = $('#editor-form'), confirm = $('#confirm-dialog');
    let saving = false, saveAction, confirmAction;
    function busy(value) {
        saving = value;
        for (const control of document.querySelectorAll('#editor-dialog button, #confirm-dialog button')) control.disabled = value;
        $('#save-editor').textContent = value ? 'Saving…' : 'Save changes';
        $('#submit-confirm').textContent = value ? 'Working…' : $('#submit-confirm').dataset.label;
    }
    for (const modal of [dialog, confirm]) modal.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    $('#cancel-editor').addEventListener('click', () => dialog.close());
    $('#cancel-confirm').addEventListener('click', () => confirm.close());
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving) return;
        for (const control of form.querySelectorAll('[aria-invalid]')) control.removeAttribute('aria-invalid');
        for (const error of form.querySelectorAll('.field-error')) error.hidden = true;
        showNotice($('#editor-status'), '');
        busy(true);
        try { await saveAction(Object.fromEntries(new FormData(form))); dialog.close(); }
        catch (error) {
            onError(error, $('#editor-status'));
            for (const [name, message] of Object.entries(error.fields || {})) {
                const control = form.elements.namedItem(name);
                if (!control) continue;
                control.setAttribute('aria-invalid', 'true');
                const feedback = document.getElementById(`error-${name}`);
                feedback.textContent = message; feedback.hidden = false;
            }
        } finally { busy(false); }
    });
    $('#submit-confirm').addEventListener('click', async () => {
        if (saving) return;
        busy(true);
        try { await confirmAction(); confirm.close(); }
        catch (error) { onError(error, $('#confirm-status')); }
        finally { busy(false); }
    });
    return {
        edit(title, description, fields, save) {
            saveAction = save; form.reset(); $('#editor-fields').replaceChildren();
            $('#editor-title').textContent = title; $('#editor-description').textContent = description;
            showNotice($('#editor-status'), '');
            for (const field of fields) {
                const id = `edit-${field.name}`;
                const label = element('label', field.label); label.htmlFor = id;
                const control = element(field.options ? 'select' : field.type === 'textarea' ? 'textarea' : 'input');
                control.id = id; control.name = field.name;
                if (field.options) {
                    const placeholder = element('option', 'Select an option'); placeholder.value = ''; control.append(placeholder);
                    for (const option of field.options) { const node = element('option', option.label); node.value = option.value; control.append(node); }
                } else if (field.type !== 'textarea') { control.type = field.type || 'text'; }
                if (field.type === 'textarea') control.rows = 6;
                for (const key of ['required', 'min', 'max', 'minLength', 'maxLength', 'autocomplete', 'step']) {
                    if (field[key] != null) control[key] = field[key];
                }
                control.value = field.value ?? '';
                control.setAttribute('aria-describedby', `${id}-hint error-${field.name}`);
                const hint = element('p', field.hint || '', 'field-hint'); hint.id = `${id}-hint`; hint.hidden = !field.hint;
                const feedback = element('p', '', 'field-error'); feedback.id = `error-${field.name}`; feedback.hidden = true;
                $('#editor-fields').append(label, control, hint, feedback);
            }
            dialog.showModal();
        },
        confirm(title, description, label, action) {
            confirmAction = action;
            $('#confirm-title').textContent = title; $('#confirm-description').textContent = description;
            $('#submit-confirm').textContent = label; $('#submit-confirm').dataset.label = label;
            showNotice($('#confirm-status'), ''); confirm.showModal();
        }
    };
}
