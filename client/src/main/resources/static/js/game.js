window.KFC = window.KFC || {};

KFC.Game = (function () {
    'use strict';

    let pieces = [];
    let wasGameOver = false;

    function pieceAt(row, col) {
        return pieces.find(function (piece) {
            return piece.row === row && piece.col === col;
        }) || null;
    }

    function redraw() {
        KFC.BoardView.render({
            pieces: pieces,
            selected: KFC.Input.selectedCell(),
            draggingCell: KFC.Input.draggingCell()
        });
    }

    function onState(state) {
        if (state.pieces) {
            pieces = state.pieces;
        }
        KFC.StatusBar.showRole(state, KFC.Session.username);
        KFC.StatusBar.showAbsence(state);
        KFC.Scoreboard.update(state);

        if (state.gameOver && !wasGameOver) {
            KFC.Sounds.play('gameOver');
        }
        wasGameOver = Boolean(state.gameOver);
        KFC.BoardView.showGameOver(state);

        redraw();
    }

    function onMoveEntry(entry) {
        const sound = KFC.MoveHistory.addLive(entry);
        if (sound) {
            KFC.Sounds.play(sound);
        }
    }

    function joinRejectionText(message) {
        return message.reason === 'ALREADY_IN_ANOTHER_ROOM'
            ? 'You are already in room ' + message.occupiedRoomId
            : 'Could not join that room';
    }

    /**
     * The errors topic is shared by everyone in the room, so both kinds of
     * message reach every client and each one has to decide whether it is the
     * addressee. Anything not addressed to us is dropped silently.
     */
    function onError(message) {
        if (message.username !== KFC.Session.username) {
            return;
        }
        if (message.type === 'JOIN_REJECTED') {
            onJoinRejected(message);
            return;
        }
        KFC.StatusBar.showToast('Move rejected: ' + message.reason);
        KFC.Sounds.play('illegal');
    }

    /**
     * A refused join used to show a toast and stop there, leaving the player
     * looking at a board they never joined: no state would ever arrive and no
     * move would ever be accepted. Send them back to the home screen once the
     * message has had time to be read.
     */
    function onJoinRejected(message) {
        KFC.ActivityLog.event('join rejected: ' + message.reason);
        KFC.StatusBar.showToast(joinRejectionText(message));
        KFC.Connection.close();
        setTimeout(KFC.Session.returnHome, KFC.Config.TOAST_DURATION_MS);
    }

    function onRoomClosed() {
        KFC.ActivityLog.event('room closed by the server');
        KFC.Connection.close();
        KFC.Session.returnHome();
    }

    function leave() {
        KFC.ActivityLog.event('leaving room');
        KFC.Connection.close();
        KFC.Session.returnHome();
    }

    function start() {
        if (!KFC.Session.isComplete()) {
            KFC.Session.returnHome();
            return;
        }

        KFC.Sounds.init();
        KFC.StatusBar.init();
        KFC.StatusBar.showRoom(KFC.Session.roomId);

        KFC.Input.attach({
            pieceAt: pieceAt,
            onRedraw: redraw,
            onMove: function (from, to) { KFC.Connection.sendMove(from, to); },
            onJump: function (cell) { KFC.Connection.sendJump(cell); }
        });

        KFC.BoardView.init({
            onPointerDown: KFC.Input.onPointerDown,
            onDoubleClick: KFC.Input.onDoubleClick
        });

        document.getElementById('leaveButton').addEventListener('click', leave);

        KFC.Connection.start({
            roomId: KFC.Session.roomId,
            token: KFC.Session.token,
            handlers: {
                username: KFC.Session.username,
                onConnected: function () {
                    KFC.StatusBar.setConnection('', true);
                    KFC.MoveHistory.loadSnapshot(KFC.Session.roomId);
                },
                onDisconnected: function () {
                    KFC.StatusBar.setConnection('Connection lost, reconnecting', false);
                },
                onState: onState,
                onMove: onMoveEntry,
                onError: onError,
                onRoomClosed: onRoomClosed
            }
        });

        setInterval(redraw, KFC.Config.ANIMATION_INTERVAL_MS);
    }

    return { start: start };
})();

document.addEventListener('DOMContentLoaded', KFC.Game.start);
