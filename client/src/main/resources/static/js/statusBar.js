window.KFC = window.KFC || {};

KFC.StatusBar = (function () {
    'use strict';

    let statusEl, roleEl, roomEl, waitingEl, absenceEl, toastEl;
    let toastTimer = null;

    function init() {
        statusEl = document.getElementById('connectionStatus');
        roleEl = document.getElementById('roleInfo');
        roomEl = document.getElementById('roomLabel');
        waitingEl = document.getElementById('waitingBanner');
        absenceEl = document.getElementById('absenceBanner');
        toastEl = document.getElementById('rejectionToast');
    }

    function showRoom(roomId) {
        roomEl.textContent = roomId;
    }

    function setConnection(text, connected) {
        statusEl.textContent = text;
        statusEl.classList.toggle('connected', connected);
    }

    function showRole(state, myUsername) {
        const waiting = !state.whiteUsername || !state.blackUsername;
        waitingEl.style.display = waiting ? 'block' : 'none';

        if (state.whiteUsername === myUsername) {
            roleEl.textContent = 'You are White';
        } else if (state.blackUsername === myUsername) {
            roleEl.textContent = 'You are Black';
        } else {
            roleEl.textContent = 'You are watching';
        }
    }

    function showAbsence(state) {
        const counting = state.disconnectedUsername
            && state.resignInSeconds !== null
            && state.resignInSeconds !== undefined;
        if (!counting) {
            absenceEl.style.display = 'none';
            return;
        }
        absenceEl.textContent = state.disconnectedUsername
            + ' left. The game ends in ' + state.resignInSeconds + 's';
        absenceEl.style.display = 'inline-block';
    }

    function showToast(text) {
        toastEl.textContent = text;
        toastEl.style.display = 'block';
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () {
            toastEl.style.display = 'none';
        }, KFC.Config.TOAST_DURATION_MS);
    }

    return {
        init: init,
        showRoom: showRoom,
        setConnection: setConnection,
        showRole: showRole,
        showAbsence: showAbsence,
        showToast: showToast
    };
})();
