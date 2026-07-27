(function (window) {
    'use strict';

    const STORAGE_KEY = 'kfc.activityLog';
    const MAX_ENTRIES = 1500;
    const MAX_BODY_CHARS = 300;
    const PERSIST_INTERVAL_MS = 1000;

    // Mirrors the server-side redaction. The browser holds the password in
    // clear text at login time, and localStorage survives the tab closing, so
    // an unredacted log would leave credentials sitting on disk.
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

    // Game state arrives about twenty times a second. Serialising the whole
    // buffer on every entry would stall rendering, so writes are batched and
    // also flushed when the page goes away.
    function persist() {
        if (!dirty) return;
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
            dirty = false;
        } catch (e) {
            // Quota exceeded or storage disabled. Drop the oldest half and
            // carry on rather than breaking the page.
            entries = entries.slice(Math.floor(entries.length / 2));
        }
    }

    function redact(text) {
        return String(text).replace(SECRET_FIELD, '$1"***"');
    }

    function summarise(body) {
        if (body === undefined || body === null || body === '') return '-';
        const clean = redact(typeof body === 'string' ? body : JSON.stringify(body));
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
        return line.at + ' ' + line.kind.padEnd(5) +
            ' session=' + line.session + ' user=' + line.user +
            ' dest=' + line.dest + ' body=' + line.body;
    }

    window.setInterval(persist, PERSIST_INTERVAL_MS);
    window.addEventListener('beforeunload', persist);

    window.ActivityLog = {
        setContext: function (session, user) {
            context = { session: session || '-', user: user || '-' };
        },
        sent: function (destination, body) { append('SEND', destination, body); },
        received: function (destination, body) { append('RECV', destination, body); },
        http: function (method, url, status) { append('HTTP', method + ' ' + url, 'status=' + status); },
        event: function (text) { append('EVENT', text, null); },
        clear: function () {
            entries = [];
            dirty = true;
            persist();
        },
        download: function () {
            persist();
            const text = entries.map(format).join('\n');
            const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
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
})(window);
