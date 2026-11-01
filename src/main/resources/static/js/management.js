import { request, write, refreshCsrf, showNotice } from './api.js';
import { $, element, button, actions, titleCell, empty, table, matches, date, createDialogs } from './ui.js';

const collections = { students: 'Students', teachers: 'Teachers', admins: 'Admins' };
let me, data = { courses: [], enrollments: [], notices: [], students: [], teachers: [], admins: [] };
let view = 'overview', collection = 'students', refreshTask;
const admin = () => me?.role === 'ADMIN';
const teacher = () => me?.role === 'TEACHER';
const ownCollection = () => ({ ADMIN: 'admins', TEACHER: 'teachers', STUDENT: 'students' })[me.role];
const roleName = () => me.role.charAt(0) + me.role.slice(1).toLowerCase();
const dialogs = createDialogs(handleError);

function handleError(error, target = $('#page-status')) {
    if (error.status === 401) { location.replace('/login.html?expired=1'); return; }
    showNotice(target, error.message, 'error');
}
function success(message) { showNotice($('#page-status'), message, 'success'); }
function count(value, noun) { return `${value} ${noun}${value === 1 ? '' : 's'}`; }
function navigate(key) { location.hash = key; }
function setIdentity() {
    $('#identity').textContent = me.fullName;
    $('#role').textContent = `${roleName()} account`;
    $('#avatar').textContent = me.fullName.split(/\s+/).filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase();
    $('#workspace-role').textContent = `${me.role} WORKSPACE`;
    $('#accounts-nav').hidden = !admin();
    $('#add-course').hidden = !admin(); $('#add-notice').hidden = !admin(); $('#add-enrollment').hidden = !admin();
    $('#courses-nav-label').textContent = teacher() ? 'My courses' : 'Courses';
    $('#enrollments-nav-label').textContent = admin() ? 'Enrollments' : teacher() ? 'My students' : 'My courses';
    $('label[for="enrollment-search"]').textContent = teacher() ? 'Search students' : me.role === 'STUDENT' ? 'Search my courses' : 'Search enrollments';
    $('#enrollment-search').placeholder = me.role === 'STUDENT' ? 'Course code or title' : 'Course or student';
}

function refreshData(clear = false) {
    if (refreshTask) return refreshTask.then(() => refreshData(clear));
    refreshTask = loadData(clear).finally(() => { refreshTask = undefined; });
    return refreshTask;
}
async function loadData(clear) {
    $('#refresh').disabled = true; $('#refresh').textContent = 'Refreshing…';
    if (clear) showNotice($('#page-status'), '');
    try {
        const keys = ['courses', 'enrollments', 'notices', ...(admin() ? ['students', 'teachers', 'admins'] : [])];
        const values = await Promise.all(keys.map(key => request(`/api/${key}`)));
        data = { ...data, ...Object.fromEntries(keys.map((key, index) => [key, values[index]])) };
        updateCourseFilter(); render(); $('#workspace-content').hidden = false;
    } finally { $('#refresh').disabled = false; $('#refresh').textContent = '↻ Refresh'; }
}
async function changed(message) {
    // A successful write should not be reported as failed if a later refresh loses its connection.
    try { me = await request('/api/auth/me'); setIdentity(); await refreshData(); success(message); }
    catch (error) { handleError(error); }
}
function setView() {
    let key = location.hash.slice(1) || 'overview';
    if (!['overview', 'courses', 'accounts', 'enrollments', 'notices', 'profile'].includes(key) || (key === 'accounts' && !admin())) key = 'overview';
    view = key;
    const names = {
        overview: ['Overview', `Welcome back, ${me.fullName.split(' ')[0]}.`, admin() ? 'Manage people, courses, and university updates in one place.' : teacher() ? 'Your teaching workspace, with courses and students at a glance.' : 'Your courses, enrollments, and university updates in one place.'],
        courses: [teacher() ? 'My courses' : 'Courses', teacher() ? 'My courses' : 'Course catalog', admin() ? 'Create courses, assign teachers, and manage available seats.' : teacher() ? 'Review the courses assigned to you and open their student rosters.' : 'Find a course and enroll when a seat is available.'],
        accounts: ['People', 'University directory', 'Maintain student, teacher, and admin accounts.'],
        enrollments: [admin() ? 'Enrollments' : teacher() ? 'My students' : 'My courses', admin() ? 'Student enrollments' : teacher() ? 'My students' : 'My courses', admin() ? 'Enroll students and manage course registrations.' : teacher() ? 'Student rosters for your assigned courses.' : 'Review your enrolled courses or withdraw from a course.'],
        notices: ['Notice board', 'Notice board', 'University announcements and information for everyone.'],
        profile: ['My profile', 'My profile', 'Manage your personal information and account password.']
    };
    const [breadcrumb, title, description] = names[key];
    $('#breadcrumb').textContent = breadcrumb; $('#page-title').textContent = title; $('#page-description').textContent = description;
    document.title = `${breadcrumb} · University Management`;
    for (const section of document.querySelectorAll('.view')) section.hidden = section.id !== `view-${key}`;
    for (const link of document.querySelectorAll('#main-nav a')) {
        if (link.dataset.view === key) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
    }
    showNotice($('#page-status'), ''); render();
}
function render() {
    if (!me) return;
    ({ overview: renderOverview, courses: renderCourses, accounts: renderAccounts, enrollments: renderEnrollments, notices: renderNotices, profile: renderProfile })[view]();
}
function renderOverview() {
    const teachingCredits = data.courses.reduce((total, course) => total + course.credits, 0);
    const enrolledCredits = data.enrollments.reduce((total, enrollment) => total + enrollment.credits, 0);
    const uniqueStudents = new Set(data.enrollments.map(enrollment => enrollment.studentId)).size;
    const available = data.courses.filter(course => course.enrolledCount < course.capacity && !data.enrollments.some(enrollment => enrollment.courseId === course.id)).length;
    const stats = admin() ? [
        ['Students', data.students.length, 'Registered student accounts', 'accounts'], ['Teachers', data.teachers.length, 'Registered teacher accounts', 'accounts'],
        ['Courses', data.courses.length, 'Courses in the catalog', 'courses'], ['Enrollments', data.enrollments.length, 'Current course registrations', 'enrollments']
    ] : teacher() ? [
        ['Assigned courses', data.courses.length, 'Courses you are teaching', 'courses'], ['Students', uniqueStudents, 'Across your course rosters', 'enrollments'],
        ['Teaching credits', teachingCredits, 'Total assigned course credits', 'courses'], ['Notices', data.notices.length, 'University announcements', 'notices']
    ] : [
        ['My courses', data.enrollments.length, 'Current course registrations', 'enrollments'], ['Enrolled credits', enrolledCredits, 'Total for your current courses', 'enrollments'],
        ['Available courses', available, 'Courses with an open seat', 'courses'], ['Notices', data.notices.length, 'University announcements', 'notices']
    ];
    $('#summary-cards').replaceChildren(...stats.map(([label, value, description, destination], index) => {
        const card = element('a', null, `summary-card${index === 0 ? ' featured' : ''}`); card.href = `#${destination}`;
        card.append(element('span', label, 'stat-label'), element('strong', value, 'stat-value'), element('span', description, 'stat-description'));
        if (admin() && destination === 'accounts') card.addEventListener('click', () => { collection = label.toLowerCase(); });
        return card;
    }));
    $('#overview-courses-title').textContent = admin() ? 'Course overview' : teacher() ? 'Assigned courses' : 'My courses';
    const courses = admin() || teacher() ? data.courses : data.courses.filter(course => data.enrollments.some(enrollment => enrollment.courseId === course.id));
    $('#overview-courses').replaceChildren();
    if (!courses.length) $('#overview-courses').append(empty(teacher() ? 'No courses assigned yet. An admin can assign a course to you.' : admin() ? 'No courses yet. Add a course to start registration.' : 'You are not enrolled yet. Browse the catalog to find a course.'));
    for (const course of courses.slice(0, 4)) {
        const row = element('a', null, 'course-preview'); row.href = teacher() ? '#enrollments' : '#courses';
        if (teacher()) row.addEventListener('click', () => { $('#enrollment-filter').value = course.id; });
        row.append(element('span', course.code, 'course-code'), titleCell(course.title, admin() || teacher() ? `${course.enrolledCount} / ${course.capacity} students` : course.teacherName), element('span', count(course.credits, 'credit'), 'credit-pill'));
        $('#overview-courses').append(row);
    }
    const shortcuts = admin() ? [
        ['Add a student', 'Create a student account', () => openAccount(null, 'students')],
        ['Create a course', 'Assign a teacher and set capacity', () => openCourse()],
        ['Post a notice', 'Share an update with everyone', () => openNotice()]
    ] : teacher() ? [
        ['My courses', 'Review your teaching assignments', () => navigate('courses')],
        ['My students', 'Open your course rosters', () => navigate('enrollments')],
        ['Edit my profile', 'Keep your personal details current', () => openAccount(me)]
    ] : [
        ['Browse courses', 'Find your next course', () => navigate('courses')],
        ['My enrollments', 'Review your registered courses', () => navigate('enrollments')],
        ['Edit my profile', 'Keep your personal details current', () => openAccount(me)]
    ];
    $('#quick-actions').replaceChildren(...shortcuts.map(([label, hint, callback]) => {
        const node = button('', callback, 'quick-action'); node.append(titleCell(label, hint), element('span', '→', 'muted')); return node;
    }));
    $('#overview-notices').replaceChildren(...data.notices.slice(0, 3).map(notice => noticeCard(notice, true)));
    if (!data.notices.length) $('#overview-notices').append(empty('No university notices have been posted yet.'));
}
function renderCourses() {
    $('#courses-title').textContent = teacher() ? 'Assigned courses' : 'All courses';
    const records = data.courses.filter(course => matches($('#course-search').value, course.code, course.title, course.teacherName));
    $('#course-count').textContent = count(records.length, 'course');
    table($('#course-table'), 'Courses', ['Course', 'Teacher', 'Credits', 'Seats filled', 'Actions'], records.map(course => {
        let controls;
        if (admin()) controls = actions(button('Edit', () => openCourse(course)), button('Delete', () => removeCourse(course), 'text-button danger-text'));
        else if (teacher()) controls = actions(button('View roster →', () => { $('#enrollment-filter').value = course.id; navigate('enrollments'); }));
        else {
            const enrolled = data.enrollments.some(enrollment => enrollment.courseId === course.id);
            const full = course.enrolledCount >= course.capacity;
            const enroll = button(enrolled ? 'Enrolled' : full ? 'Full' : '+ Enroll', () => enrollSelf(course), 'button compact' + (enrolled || full ? '' : ' primary'));
            enroll.disabled = enrolled || full; controls = actions(enroll);
        }
        return [titleCell(course.code, course.title), course.teacherName, course.credits, `${course.enrolledCount} / ${course.capacity}`, controls];
    }), $('#course-search').value ? 'No courses match your search.' : teacher() ? 'No courses assigned yet. Contact an admin for an assignment.' : 'No courses yet. An admin can add the first course.');
}
function renderAccounts() {
    if (!admin()) return;
    const records = data[collection].filter(account => matches($('#account-search').value, account.fullName, account.email));
    for (const tab of $('#resource-tabs').children) tab.setAttribute('aria-pressed', String(tab.dataset.collection === collection));
    $('#collection-title').textContent = collections[collection]; $('#record-count').textContent = count(records.length, 'account');
    table($('#account-table'), collections[collection], ['Name', 'Email address', 'Actions'], records.map(account => [
        titleCell(account.fullName, `Account #${account.id}${account.id === me.id ? ' · You' : ''}`), account.email,
        actions(button('Edit', () => openAccount(account, collection)), button('Delete', () => removeAccount(account), 'text-button danger-text'))
    ]), $('#account-search').value ? 'No people match your search.' : `No ${collection} yet. Add the first account.`);
}
function updateCourseFilter() {
    const selected = $('#enrollment-filter').value;
    $('#enrollment-filter').replaceChildren();
    const all = element('option', 'All courses'); all.value = ''; $('#enrollment-filter').append(all);
    for (const course of data.courses) { const option = element('option', course.code); option.value = course.id; $('#enrollment-filter').append(option); }
    $('#enrollment-filter').value = data.courses.some(course => String(course.id) === selected) ? selected : '';
}
function renderEnrollments() {
    $('#enrollments-title').textContent = admin() ? 'All enrollments' : teacher() ? 'Course rosters' : 'Enrolled courses';
    const filter = $('#enrollment-filter').value;
    const records = data.enrollments.filter(enrollment => (!filter || String(enrollment.courseId) === filter)
        && matches($('#enrollment-search').value, enrollment.courseCode, enrollment.courseTitle, enrollment.studentName, enrollment.studentEmail));
    $('#enrollment-count').textContent = count(records.length, 'enrollment');
    const headers = ['Course', ...(me.role === 'STUDENT' ? ['Teacher', 'Credits'] : ['Student', 'Email address']), 'Enrolled on', ...(!teacher() ? ['Actions'] : [])];
    table($('#enrollment-table'), 'Course enrollments', headers, records.map(enrollment => [
        titleCell(enrollment.courseCode, enrollment.courseTitle), ...(me.role === 'STUDENT' ? [enrollment.teacherName, enrollment.credits] : [enrollment.studentName, enrollment.studentEmail]),
        date(enrollment.enrolledAt), ...(!teacher() ? [actions(button('Withdraw', () => withdraw(enrollment), 'text-button danger-text'))] : [])
    ]), filter || $('#enrollment-search').value ? 'No enrollments match this filter.' : teacher() ? 'No students enrolled in your courses yet.' : admin() ? 'No enrollments yet. Enroll a student to get started.' : 'No enrolled courses yet. Choose a course from the catalog.');
}
function noticeCard(notice, preview = false) {
    const card = element('article', null, preview ? 'notice-preview' : 'panel notice-card');
    card.append(element('p', `${date(notice.publishedAt)} · ${notice.authorName}`, 'notice-meta'), element('h3', notice.title), element('p', notice.body, `notice-body${preview ? ' preview-body' : ''}`));
    if (admin() && !preview) card.append(actions(button('Edit notice', () => openNotice(notice)), button('Delete', () => removeNotice(notice), 'text-button danger-text')));
    return card;
}
function renderNotices() {
    const records = data.notices.filter(notice => matches($('#notice-search').value, notice.title, notice.body));
    $('#notice-list').replaceChildren(...records.map(notice => noticeCard(notice)));
    if (!records.length) $('#notice-list').append(empty($('#notice-search').value ? 'No notices match your search.' : 'No university notices have been posted yet.'));
}
function renderProfile() {
    $('#profile-details').replaceChildren(...[['Full name', me.fullName], ['Email address', me.email], ['Account role', roleName()], ['Account ID', `#${me.id}`]].map(([label, value]) => {
        const row = element('div'); row.append(element('dt', label), element('dd', value)); return row;
    }));
}

function openAccount(account, targetCollection = ownCollection()) {
    const personal = account?.id === me.id;
    dialogs.edit(personal ? 'Edit my profile' : account ? 'Edit account' : `Add ${targetCollection.slice(0, -1)}`, personal ? 'Update your personal information.' : `Account collection: ${collections[targetCollection]}`, [
        { name: 'fullName', label: 'Full name', value: account?.fullName, required: true, minLength: 2, maxLength: 100, autocomplete: 'name' },
        { name: 'email', label: 'Email address', type: 'email', value: account?.email, required: true, maxLength: 254, autocomplete: 'off' },
        { name: 'password', label: account ? 'New password (optional)' : 'Password', type: 'password', required: !account, autocomplete: 'new-password', hint: account ? 'Leave blank to keep the current password.' : 'At least 8 characters, up to 72 UTF-8 bytes.' }
    ], async values => {
        if (!values.password) delete values.password;
        await write(`/api/${targetCollection}${account ? `/${account.id}` : ''}`, account ? 'PUT' : 'POST', values);
        await changed(account ? 'Account updated.' : 'Account created.');
    });
}
function openCourse(course) {
    if (!data.teachers.length) { showNotice($('#page-status'), 'Add a teacher in People before creating a course.', 'error'); return; }
    dialogs.edit(course ? 'Edit course' : 'Create a course', 'Set the course details and assign its teacher.', [
        { name: 'code', label: 'Course code', value: course?.code, required: true, minLength: 2, maxLength: 20, hint: 'For example, CSE 3230. Each code must be unique.' },
        { name: 'title', label: 'Course title', value: course?.title, required: true, minLength: 2, maxLength: 150 },
        { name: 'teacherId', label: 'Assigned teacher', value: course?.teacherId, required: true, options: data.teachers.map(account => ({ value: account.id, label: `${account.fullName} · ${account.email}` })) },
        { name: 'credits', label: 'Credits', type: 'number', value: course?.credits ?? 3, required: true, min: 1, max: 6, step: 1 },
        { name: 'capacity', label: 'Student capacity', type: 'number', value: course?.capacity ?? 40, required: true, min: 1, max: 200, step: 1 }
    ], async values => {
        for (const key of ['teacherId', 'credits', 'capacity']) values[key] = Number(values[key]);
        await write(`/api/courses${course ? `/${course.id}` : ''}`, course ? 'PUT' : 'POST', values);
        await changed(course ? 'Course updated.' : 'Course created.');
    });
}
function openNotice(notice) {
    dialogs.edit(notice ? 'Edit notice' : 'Post a notice', 'This notice will be visible to all university accounts.', [
        { name: 'title', label: 'Title', value: notice?.title, required: true, minLength: 2, maxLength: 150 },
        { name: 'body', label: 'Message', type: 'textarea', value: notice?.body, required: true, minLength: 2, maxLength: 3000 }
    ], async values => { await write(`/api/notices${notice ? `/${notice.id}` : ''}`, notice ? 'PUT' : 'POST', values); await changed(notice ? 'Notice updated.' : 'Notice published.'); });
}
function openEnrollment() {
    const available = data.courses.filter(course => course.enrolledCount < course.capacity);
    if (!data.students.length || !available.length) { showNotice($('#page-status'), 'You need a student account and a course with an open seat first.', 'error'); return; }
    dialogs.edit('Enroll a student', 'Choose a student and a course with an available seat.', [
        { name: 'studentId', label: 'Student', required: true, options: data.students.map(account => ({ value: account.id, label: `${account.fullName} · ${account.email}` })) },
        { name: 'courseId', label: 'Course', required: true, options: available.map(course => ({ value: course.id, label: `${course.code} · ${course.title}` })) }
    ], async values => { await write('/api/enrollments', 'POST', { studentId: Number(values.studentId), courseId: Number(values.courseId) }); await changed('Student enrolled.'); });
}
function enrollSelf(course) {
    dialogs.confirm(`Enroll in ${course.code}?`, `${course.title} · ${count(course.credits, 'credit')}. This course will be added to My courses.`, 'Enroll', async () => {
        await write('/api/enrollments', 'POST', { courseId: course.id }); await changed(`Enrolled in ${course.code}.`);
    });
    $('#submit-confirm').className = 'button primary';
}
function withdraw(enrollment) {
    dialogs.confirm(`Withdraw from ${enrollment.courseCode}?`, admin() ? `Remove ${enrollment.studentName}'s enrollment in ${enrollment.courseTitle}?` : `This will remove ${enrollment.courseTitle} from your enrolled courses. You can enroll again if a seat remains.`, 'Withdraw', async () => {
        await request(`/api/enrollments/${enrollment.id}`, { method: 'DELETE' }); await changed('Enrollment withdrawn.');
    }); $('#submit-confirm').className = 'button danger';
}
function removeAccount(account) {
    dialogs.confirm('Delete account?', `Delete ${account.fullName} (${account.email})? Withdraw their enrollments or reassign their courses first.`, 'Delete account', async () => {
        await request(`/api/${collection}/${account.id}`, { method: 'DELETE' }); await changed('Account deleted.');
    }); $('#submit-confirm').className = 'button danger';
}
function removeCourse(course) {
    dialogs.confirm('Delete course?', `Delete ${course.code} · ${course.title}? Enrolled students must be withdrawn first.`, 'Delete course', async () => {
        await request(`/api/courses/${course.id}`, { method: 'DELETE' }); await changed('Course deleted.');
    }); $('#submit-confirm').className = 'button danger';
}
function removeNotice(notice) {
    dialogs.confirm('Delete notice?', `Remove “${notice.title}” from the notice board?`, 'Delete notice', async () => {
        await request(`/api/notices/${notice.id}`, { method: 'DELETE' }); await changed('Notice deleted.');
    }); $('#submit-confirm').className = 'button danger';
}

$('#refresh').addEventListener('click', async () => {
    try {
        if (!me) { me = await request('/api/auth/me'); setIdentity(); await refreshCsrf(); setView(); }
        await refreshData(true);
    } catch (error) { handleError(error); }
});
$('#add-account').addEventListener('click', () => openAccount(null, collection));
$('#add-course').addEventListener('click', () => openCourse());
$('#add-notice').addEventListener('click', () => openNotice());
$('#add-enrollment').addEventListener('click', openEnrollment);
$('#edit-profile').addEventListener('click', () => openAccount(me));
for (const tab of $('#resource-tabs').children) tab.addEventListener('click', () => { collection = tab.dataset.collection; $('#account-search').value = ''; renderAccounts(); });
for (const id of ['course-search', 'account-search', 'enrollment-search', 'notice-search']) $(`#${id}`).addEventListener('input', render);
$('#enrollment-filter').addEventListener('change', renderEnrollments);
window.addEventListener('hashchange', () => { if (me) setView(); });
async function logout() {
    $('#logout').disabled = true; $('#mobile-logout').disabled = true;
    try { await request('/api/auth/logout', { method: 'POST' }); location.replace('/login.html'); }
    catch (error) { handleError(error); $('#logout').disabled = false; $('#mobile-logout').disabled = false; }
}
$('#logout').addEventListener('click', logout);
$('#mobile-logout').addEventListener('click', logout);
$('#today').dateTime = new Date().toISOString().slice(0, 10);
$('#today').textContent = new Intl.DateTimeFormat(undefined, { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }).format(new Date());
try {
    me = await request('/api/auth/me'); setIdentity(); await refreshCsrf(); setView();
    await refreshData(); showNotice($('#page-status'), '');
} catch (error) { handleError(error); }
