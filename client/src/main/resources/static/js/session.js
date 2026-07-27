window.KFC = window.KFC || {};

KFC.Session = (function () {
    'use strict';

    const params = new URLSearchParams(window.location.search);

    const username = params.get('username');
    const token = params.get('token');
    const roomId = params.get('room');

    function isComplete() {
        return Boolean(username && token && roomId);
    }

    function returnHome() {
        window.location.href = 'index.html';
    }

    return {
        username: username,
        token: token,
        roomId: roomId,
        isComplete: isComplete,
        returnHome: returnHome
    };
})();
