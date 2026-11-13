#!/usr/bin/env python3
"""End-to-end API smoke check against a RUNNING backend; deletes only its own fixture."""
import json
import os
import time
import sys
import urllib.request
import urllib.error

BASE = os.environ.get('PAGEWATCH_API', 'http://127.0.0.1:8088/api')
DEMO = 'https://demo.pagewatch.test/changelog'


def request(path, method='GET', body=None, expected=200, headers=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method,
                                 headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        response = urllib.request.urlopen(req, timeout=35)
    except urllib.error.HTTPError as e:
        response = e
    with response:
        raw = response.read()
        assert response.status == expected, (path, response.status, raw.decode())
        return json.loads(raw) if raw and response.headers.get('Content-Type', '').startswith('application/json') else None


def wait_check(mid, previous):
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        detail = request('/monitors/' + mid)
        if not detail['monitor']['checking'] and detail['monitor']['lastCheckedAt'] != previous:
            return detail
        time.sleep(.15)
    raise AssertionError('Check did not complete within 25 seconds')


def main():
    mid = None
    try:
        assert request('/health')['status'] == 'UP'
        request('/preview', 'POST', {'url': 'http://127.0.0.1'}, 400)
        request('/preview', 'POST', {'url': DEMO, 'selector': '.missing'}, 400)
        request('/monitors', 'POST', {'name': '', 'url': DEMO, 'intervalMinutes': 0}, 400)
        request('/checks', 'POST', expected=403, headers={'Origin': 'https://untrusted.example'})
        preview = request('/preview', 'POST', {'url': DEMO, 'selector': 'main'})
        assert preview['lines'] >= 6 and '导航' not in preview['content']
        monitor = request('/monitors', 'POST', {'name': 'API 冒烟验证（自动清理）', 'url': DEMO, 'selector': 'main', 'intervalMinutes': 1 if '--scheduler' in sys.argv else 60}, 201)
        mid = monitor['id']
        detail = request('/monitors/' + mid)
        assert len(detail['snapshots']) == 1 and not detail['monitor']['unread']
        if '--scheduler' in sys.argv:
            print('Waiting for a real automatic check (about 60–90 seconds)…', flush=True)
            deadline = time.monotonic() + 100
            baseline = detail['monitor']['lastCheckedAt']
            while time.monotonic() < deadline:
                detail = request('/monitors/' + mid)
                if detail['monitor']['lastCheckedAt'] != baseline:
                    break
                time.sleep(2)
            else:
                raise AssertionError('Scheduled check did not run within 100 seconds')
            assert detail['checks'][0]['outcome'] == 'UNCHANGED'
            print('PASS: automatic scheduler ran without a manual check request', flush=True)
        before = detail['monitor']['lastCheckedAt']
        request('/monitors/' + mid + '/check', 'POST', expected=202)
        detail = wait_check(mid, before)
        assert len(detail['snapshots']) == 1 and detail['checks'][0]['outcome'] == 'UNCHANGED'
        request('/demo/advance', 'POST')
        before = detail['monitor']['lastCheckedAt']
        request('/monitors/' + mid + '/check', 'POST', expected=202)
        detail = wait_check(mid, before)
        assert len(detail['snapshots']) == 2 and detail['monitor']['unread']
        latest = detail['snapshots'][0]
        diff = request('/monitors/' + mid + '/snapshots/' + latest['id'])
        assert any(line['kind'] == 'add' for line in diff['diff'])
        assert any(line['kind'] == 'remove' for line in diff['diff'])
        request('/monitors/' + mid + '/read', 'POST', expected=204)
        assert not request('/monitors/' + mid)['monitor']['unread']
        paused = request('/monitors/' + mid, 'PATCH', {'name': '已编辑', 'intervalMinutes': 15, 'active': False})
        assert not paused['active'] and paused['name'] == '已编辑'
        resumed = request('/monitors/' + mid, 'PATCH', {'name': '已编辑', 'intervalMinutes': 15, 'active': True})
        assert resumed['active'] and resumed['intervalMinutes'] == 15
        request('/monitors/' + mid, 'DELETE', expected=204)
        request('/monitors/' + mid, expected=404)
        mid = None
        print('PASS: health, input validation, local-address protection, origin protection, preview, baseline, unchanged, changed, diff, read, pause/resume, delete')
    finally:
        if mid:
            request('/monitors/' + mid, 'DELETE', expected=204)


if __name__ == '__main__':
    main()
