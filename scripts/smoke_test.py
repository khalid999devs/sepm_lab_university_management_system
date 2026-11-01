#!/usr/bin/env python3
"""Exercise the running demo over HTTP; no third-party Python packages needed."""

import argparse
import json
import os
import time
import uuid
from http.cookiejar import CookieJar
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPCookieProcessor, Request, build_opener


class Client:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip('/')
        self.cookies = CookieJar()
        self.opener = build_opener(HTTPCookieProcessor(self.cookies))
        self.csrf = None

    def call(self, path, method='GET', body=None, form=None):
        headers = {'Accept': 'application/json'}
        payload = None
        if body is not None:
            payload = json.dumps(body).encode()
            headers['Content-Type'] = 'application/json'
        if form is not None:
            payload = urlencode(form).encode()
            headers['Content-Type'] = 'application/x-www-form-urlencoded'
        if method not in ('GET', 'HEAD') and self.csrf:
            headers[self.csrf['headerName']] = self.csrf['token']
        request = Request(self.base_url + path, data=payload, headers=headers, method=method)
        try:
            response = self.opener.open(request, timeout=15)
        except HTTPError as error:
            response = error
        with response:
            text = response.read().decode()
            result = json.loads(text) if text and 'application/json' in response.headers.get('Content-Type', '') else text
            return response.status, response.headers, result

    def refresh_csrf(self):
        status, headers, self.csrf = self.call('/api/auth/csrf')
        assert status == 200, 'CSRF endpoint failed'
        return headers

    def login(self, email, password):
        self.refresh_csrf()
        status, _, identity = self.call('/api/auth/login', 'POST', form={'email': email, 'password': password})
        assert status == 200, 'Demo login failed; check .env and the existing database credentials'
        assert set(identity) == {'id', 'fullName', 'email', 'role'}, 'Identity leaked extra fields'
        self.refresh_csrf()
        return identity


def configuration():
    values = {}
    env_file = Path(__file__).resolve().parents[1] / '.env'
    if env_file.exists():
        for line in env_file.read_text().splitlines():
            if '=' in line and not line.lstrip().startswith('#'):
                key, value = line.split('=', 1)
                values[key.strip()] = value.strip()
    values.update(os.environ)
    return values


def academic_checks(admin, members, suffix):
    created = []
    enrollment_location = None
    try:
        identities = {}
        clients = {}
        for resource in ('teachers', 'students'):
            email = f'academic-{resource}-{suffix}@ums.test'
            status, headers, account = admin.call('/api/' + resource, 'POST', body={
                'fullName': 'Academic Smoke Account', 'email': email, 'password': 'SmokePass123'
            })
            assert status == 201, 'Academic test account creation failed'
            created.append(headers['Location'])
            identities[resource] = account
            clients[resource] = Client(admin.base_url)
            clients[resource].login(email, 'SmokePass123')

        status, headers, course = admin.call('/api/courses', 'POST', body={
            'code': 'QA ' + suffix.upper(), 'title': 'Temporary Smoke Course',
            'credits': 3, 'capacity': 1, 'teacherId': identities['teachers']['id']
        })
        assert status == 201, 'Course creation failed'
        course_location = headers['Location']
        created.append(course_location)
        status, headers, notice = admin.call('/api/notices', 'POST', body={
            'title': 'Temporary smoke notice', 'body': 'This test announcement will be removed.'
        })
        assert status == 201, 'Notice creation failed'
        created.append(headers['Location'])
        assert clients['students'].call(headers['Location'])[0] == 200, 'Student could not read a notice'
        assert clients['teachers'].call('/api/courses')[2][0]['id'] == course['id'], 'Teacher course scope failed'
        assert members['TEACHER'].call(course_location)[0] == 404, 'Other teacher accessed an unassigned course'
        assert clients['students'].call('/api/notices', 'POST', body={})[0] == 403, 'Student published a notice'

        status, headers, enrollment = clients['students'].call('/api/enrollments', 'POST', body={'courseId': course['id']})
        assert status == 201, 'Student enrollment failed'
        enrollment_location = headers['Location']
        assert admin.call(course_location)[2]['enrolledCount'] == 1, 'Enrollment count did not update'
        assert clients['teachers'].call('/api/enrollments')[2][0]['studentId'] == identities['students']['id'], 'Teacher roster failed'
        assert members['STUDENT'].call(enrollment_location)[0] == 404, 'Student could read a peer enrollment'
        assert members['TEACHER'].call(enrollment_location)[0] == 404, 'Other teacher could read a private roster'
        assert clients['students'].call('/api/enrollments', 'POST', body={'courseId': course['id']})[0] == 409, 'Duplicate enrollment was accepted'
        assert members['STUDENT'].call('/api/enrollments', 'POST', body={'courseId': course['id']})[0] == 409, 'Full course accepted an enrollment'
        assert admin.call(course_location, 'DELETE')[0] == 409, 'Enrolled course was deleted'
        assert admin.call('/api/students/' + str(identities['students']['id']), 'DELETE')[0] == 409, 'Enrolled student account was deleted'
        assert clients['students'].call(enrollment_location, 'DELETE')[0] == 204, 'Student withdrawal failed'
        enrollment_location = None
        assert admin.call(course_location)[2]['enrolledCount'] == 0, 'Withdrawal did not free the seat'
    finally:
        if enrollment_location:
            assert admin.call(enrollment_location, 'DELETE')[0] in (204, 404), 'Enrollment cleanup failed'
        for path in reversed(created):
            assert admin.call(path, 'DELETE')[0] == 204, f'Test resource cleanup failed: {path}'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://localhost:8080')
    args = parser.parse_args()
    config = configuration()
    anonymous = Client(args.base_url)
    for attempt in range(30):
        try:
            status, _, body = anonymous.call('/actuator/health')
            if status == 200 and body.get('status') == 'UP':
                break
        except (URLError, TimeoutError, ConnectionError):
            pass
        time.sleep(1)
    else:
        raise AssertionError('Application did not become healthy within 30 attempts')

    for path, expected in [('/', 'login-form'), ('/management.html', 'summary-cards'), ('/css/styles.css', '--accent'), ('/js/ui.js', 'createDialogs')]:
        status, _, body = anonymous.call(path)
        assert status == 200 and expected in body, f'Page or asset unavailable: {path}'
    assert anonymous.call('/api/students')[0] == 401, 'Anonymous client could access accounts'
    headers = anonymous.refresh_csrf()
    cookie = headers.get('Set-Cookie', '')
    assert 'HttpOnly' in cookie and 'SameSite=Lax' in cookie, 'Session cookie settings are incorrect'

    admin = Client(args.base_url)
    admin.login('admin@ums.test', config['DEMO_ADMIN_PASSWORD'])
    members = {}
    for role in ('TEACHER', 'STUDENT'):
        client = Client(args.base_url)
        identity = client.login(role.lower() + '@ums.test', config['DEMO_' + role + '_PASSWORD'])
        assert identity['role'] == role, 'Incorrect login role'
        members[role] = client
        for resource in ('admins', 'teachers', 'students'):
            expected = 200 if resource == role.lower() + 's' else 403
            assert client.call('/api/' + resource)[0] == expected, 'Role permissions failed'
        route = '/api/' + role.lower() + 's'
        records = client.call(route)[2]
        assert len(records) == 1 and records[0]['id'] == identity['id'], 'Personal collection exposed another account'
        assert client.call(route + '/' + str(identity['id']), 'PUT', body={
            'fullName': identity['fullName'], 'email': identity['email']
        })[0] == 200, 'Own-profile update failed'
        assert client.call(route, 'POST', body={
            'fullName': 'Blocked Account', 'email': 'blocked@ums.test', 'password': 'BlockedPass123'
        })[0] == 403, 'Personal account could create another account'
        assert client.call(route + '/' + str(identity['id']), 'DELETE')[0] == 403, 'Personal account could delete its account'

    suffix = uuid.uuid4().hex[:12]
    for resource in ('admins', 'teachers', 'students'):
        route = '/api/' + resource
        email = f'smoke-{resource}-{suffix}@ums.test'
        status, headers, account = admin.call(route, 'POST', body={
            'fullName': 'Smoke Account', 'email': email, 'password': 'SmokePass123'
        })
        assert status == 201, 'Account creation failed'
        assert set(account) == {'id', 'fullName', 'email', 'role'}, 'Account response leaked extra fields'
        location = headers.get('Location')
        assert location == route + '/' + str(account['id']), 'Missing resource Location header'
        try:
            role = account['role']
            if role in members:
                member = members[role]
                assert member.call(location)[0] == 404, 'Personal account exposed another account'
                assert member.call(location, 'PUT', body={'fullName': 'Blocked Edit', 'email': email})[0] == 404, 'Personal account could edit another account'
                assert member.call(location, 'DELETE')[0] == 403, 'Personal account could delete another account'
                assert len(member.call(route)[2]) == 1, 'Personal account list included another account'
            assert admin.call(location)[0] == 200, 'Created resource could not be read'
            assert admin.call(location, 'PUT', body={'fullName': 'Edited Account', 'email': email})[0] == 200, 'Account update failed'
            assert admin.call(location)[2]['fullName'] == 'Edited Account', 'Update was not persisted'
            fresh = Client(args.base_url)
            assert fresh.login(email, 'SmokePass123')['fullName'] == 'Edited Account', 'Update changed the omitted password'
        finally:
            status, _, body = admin.call(location, 'DELETE')
            assert status == 204 and body == '', 'Deletion did not return an empty 204'
        assert admin.call(location)[0] == 404, 'Deleted resource still exists'

    academic_checks(admin, members, suffix)
    assert admin.call('/api/auth/logout', 'POST')[0] == 204, 'Logout failed'
    admin.csrf = None
    assert admin.call('/api/auth/me')[0] == 401, 'Logout left an authenticated session'
    print('HTTP smoke checks passed: assets, all role logins, ownership, account CRUD, courses, notices, rosters, enrollment, capacity, withdrawal, and logout.')


if __name__ == '__main__':
    main()
