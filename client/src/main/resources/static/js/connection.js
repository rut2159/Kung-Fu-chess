window.KFC = window.KFC || {};

KFC.Connection = (function () {
    'use strict';

    let client = null;
    let handlers = null;
    let roomId = null;
    let token = null;
    let attempts = 0;
    let retryTimer = null;
    let closedByUs = false;

    function start(options) {
        handlers = options.handlers;
        roomId = options.roomId;
        token = options.token;
        connect();
    }

    function topic(suffix) {
        return '/topic/room/' + roomId + '/' + suffix;
    }

    function connect() {
        const socket = new SockJS('/ws');
        client = Stomp.over(socket);
        client.debug = null;
        client.connect({}, onConnected, onLost);
    }

    function onConnected() {
        attempts = 0;
        KFC.ActivityLog.setContext(roomId, handlers.username);
        KFC.ActivityLog.event('connected to room ' + roomId);
        handlers.onConnected();

        subscribe('game', handlers.onState);
        subscribe('moves', handlers.onMove);
        subscribe('errors', handlers.onError);
        subscribe('closed', handlers.onRoomClosed);

        send('/app/join', { token: token, roomId: roomId });
    }

    function subscribe(suffix, callback) {
        client.subscribe(topic(suffix), function (frame) {
            KFC.ActivityLog.received(topic(suffix), frame.body);
            callback(JSON.parse(frame.body));
        });
    }

    function send(destination, payload) {
        const body = JSON.stringify(payload);
        KFC.ActivityLog.sent(destination, body);
        client.send(destination, {}, body);
    }

    function onLost() {
        if (closedByUs) {
            return;
        }
        KFC.ActivityLog.event('connection lost');
        handlers.onDisconnected();
        scheduleRetry();
    }

    function scheduleRetry() {
        if (retryTimer) {
            return;
        }
        const delay = Math.min(
            KFC.Config.RECONNECT_BASE_MS * Math.pow(2, attempts),
            KFC.Config.RECONNECT_MAX_MS);
        attempts++;
        KFC.ActivityLog.event('reconnect attempt ' + attempts);
        retryTimer = setTimeout(function () {
            retryTimer = null;
            connect();
        }, delay);
    }

    function sendMove(from, to) {
        send('/app/move', {
            fromRow: from.row, fromCol: from.col,
            toRow: to.row, toCol: to.col
        });
    }

    function sendJump(cell) {
        send('/app/jump', { row: cell.row, col: cell.col });
    }

    /**
     * Tells the server we are going before dropping the socket.
     *
     * SessionDisconnectEvent is asynchronous, so leaving a room and joining
     * another straight away could race it and be refused as
     * ALREADY_IN_ANOTHER_ROOM. /app/leave settles it while we are still
     * connected. The disconnect listener remains the safety net for tabs that
     * close without warning.
     */
    function close() {
        closedByUs = true;
        clearTimeout(retryTimer);
        retryTimer = null;
        if (!client) {
            return;
        }
        try {
            send('/app/leave', { roomId: roomId });
        } catch (e) {
            KFC.ActivityLog.event('leave notice failed: ' + e);
        }
        try {
            client.disconnect();
        } catch (e) {
            KFC.ActivityLog.event('disconnect failed: ' + e);
        }
    }

    return {
        start: start,
        sendMove: sendMove,
        sendJump: sendJump,
        close: close
    };
})();
