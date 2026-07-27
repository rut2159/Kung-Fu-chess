window.KFC = window.KFC || {};

KFC.MoveHistory = (function () {
    'use strict';

    let entries = [];
    const seenKeys = new Set();

    function keyOf(entry) {
        return entry.color + '|' + entry.timestampMs + '|' + entry.notation;
    }

    function remember(entry) {
        const key = keyOf(entry);
        if (seenKeys.has(key)) {
            return false;
        }
        seenKeys.add(key);
        entries.push(entry);
        entries.sort(function (a, b) { return a.timestampMs - b.timestampMs; });
        return true;
    }

    function render() {
        const bodies = {
            WHITE: document.getElementById('whiteHistoryBody'),
            BLACK: document.getElementById('blackHistoryBody')
        };
        bodies.WHITE.replaceChildren();
        bodies.BLACK.replaceChildren();

        entries.forEach(function (entry) {
            const body = bodies[entry.color];
            if (!body) {
                return;
            }
            const row = document.createElement('div');
            row.className = 'moveHistoryRow';

            const time = document.createElement('span');
            time.textContent = entry.time;
            const move = document.createElement('span');
            move.textContent = entry.notation;

            row.appendChild(time);
            row.appendChild(move);
            body.appendChild(row);
        });
    }

    function addLive(entry) {
        if (!remember(entry)) {
            return null;
        }
        render();
        return entry.notation.indexOf('x') >= 0 ? 'capture' : 'move';
    }

    async function loadSnapshot(roomId) {
        const url = '/api/rooms/' + encodeURIComponent(roomId) + '/moves';
        try {
            const response = await fetch(url);
            KFC.ActivityLog.http('GET', url, response.status);
            if (!response.ok) {
                return;
            }
            const snapshot = await response.json();
            let added = false;
            snapshot.forEach(function (entry) {
                if (remember(entry)) {
                    added = true;
                }
            });
            if (added) {
                render();
            }
        } catch (e) {
            KFC.ActivityLog.event('move history snapshot failed: ' + e);
        }
    }

    function reset() {
        entries = [];
        seenKeys.clear();
        render();
    }

    return { addLive: addLive, loadSnapshot: loadSnapshot, reset: reset };
})();
