window.KFC = window.KFC || {};

KFC.ActivityLog = (function () {
    'use strict';

    const STORAGE_KEY = 'kfc.activityLog';
    const MAX_ENTRIES = 1500;
    const MAX_BODY_CHARS = 300;
    const PERSIST_INTERVAL_MS = 1000;
    const SECRET_FIELD = /("(?:password|token)"\s*:\s*)"(?:[^"\\]|\\.)*"/gi;

    let entries = load();
    let dirty = false;
    let context = { session: '-', user: '-' };

    function load() {
        try {
            const raw = window.localStorage.getItem(STORAGE_KEY);
            return raw ? JSON.parse(raw) : [];
        } catch (e) {
            return [];
        }
    }

    function persist() {
        if (!dirty) {
            return;
        }
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
            dirty = false;
        } catch (e) {
            entries = entries.slice(Math.floor(entries.length / 2));
        }
    }

    function summarise(body) {
        if (body === undefined || body === null || body === '') {
            return '-';
        }
        const text = typeof body === 'string' ? body : JSON.stringify(body);
        const clean = text.replace(SECRET_FIELD, '$1"***"');
        return clean.length <= MAX_BODY_CHARS
            ? clean
            : clean.slice(0, MAX_BODY_CHARS) + '...(' + clean.length + ' chars)';
    }

    function append(kind, destination, body) {
        const line = {
            at: new Date().toISOString(),
            kind: kind,
            session: context.session,
            user: context.user,
            dest: destination || '-',
            body: summarise(body)
        };
        entries.push(line);
        if (entries.length > MAX_ENTRIES) {
            entries.splice(0, entries.length - MAX_ENTRIES);
        }
        dirty = true;
        console.log('[' + line.kind + '] ' + line.dest + ' ' + line.body);
    }

    function format(line) {
        return line.at + ' ' + line.kind
            + ' session=' + line.session + ' user=' + line.user
            + ' dest=' + line.dest + ' body=' + line.body;
    }

    window.setInterval(persist, PERSIST_INTERVAL_MS);
    window.addEventListener('beforeunload', persist);

    return {
        setContext: function (session, user) {
            context = { session: session || '-', user: user || '-' };
        },
        sent: function (destination, body) { append('SEND', destination, body); },
        received: function (destination, body) { append('RECV', destination, body); },
        http: function (method, url, status) { append('HTTP', method + ' ' + url, 'status=' + status); },
        event: function (text) { append('EVENT', text, null); },
        clear: function () { entries = []; dirty = true; persist(); },
        download: function () {
            persist();
            const blob = new Blob([entries.map(format).join('\n')], { type: 'text/plain;charset=utf-8' });
            const url = window.URL.createObjectURL(blob);
            const anchor = document.createElement('a');
            anchor.href = url;
            anchor.download = 'kungfuchess-client-' + Date.now() + '.log';
            document.body.appendChild(anchor);
            anchor.click();
            document.body.removeChild(anchor);
            window.URL.revokeObjectURL(url);
        }
    };
})();
