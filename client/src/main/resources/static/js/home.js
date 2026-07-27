window.KFC = window.KFC || {};

KFC.Home = (function () {
    'use strict';

    let account = null;
    let searchTimer = null;

    function el(id) {
        return document.getElementById(id);
    }

    function showCard(name) {
        el('loginCard').classList.toggle('is-hidden', name !== 'login');
        el('menuCard').classList.toggle('is-hidden', name !== 'menu');
        el('roomCard').classList.toggle('is-hidden', name !== 'room');
    }

    function setLoginMessage(text) {
        el('loginMessage').textContent = text;
    }

    function setRoomMessage(text) {
        el('roomMessage').textContent = text;
    }

    function setSearching(active) {
        el('menuButtonsRow').classList.toggle('is-hidden', active);
        el('searchingHint').classList.toggle('is-hidden', !active);
        el('searchingRow').classList.toggle('is-hidden', !active);
    }

    function credentials() {
        return {
            username: el('usernameInput').value.trim(),
            password: el('passwordInput').value
        };
    }

    async function postJson(url, payload) {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        KFC.ActivityLog.http('POST', url, response.status);
        return { ok: response.ok, data: await response.json().catch(function () { return {}; }) };
    }

    async function register() {
        const input = credentials();
        if (!input.username || !input.password) {
            setLoginMessage('Enter a username and a password');
            return;
        }
        const result = await postJson('/api/register', input);
        setLoginMessage(result.data.success
            ? 'Account created. Now log in.'
            : 'That username is taken');
    }

    async function login() {
        const input = credentials();
        if (!input.username || !input.password) {
            setLoginMessage('Enter a username and a password');
            return;
        }
        const result = await postJson('/api/login', input);
        if (!result.data.success) {
            setLoginMessage('Wrong username or password');
            return;
        }
        account = { token: result.data.token, username: result.data.username };
        setLoginMessage('');
        el('menuGreeting').textContent = 'Signed in as ' + account.username;
        el('roomGreeting').textContent = 'Signed in as ' + account.username;
        showCard('menu');
    }

    function enterRoom(roomId) {
        window.location.href = 'game.html'
            + '?token=' + encodeURIComponent(account.token)
            + '&username=' + encodeURIComponent(account.username)
            + '&room=' + encodeURIComponent(roomId);
    }

    async function createRoom() {
        setRoomMessage('');
        const response = await fetch('/api/rooms', { method: 'POST' });
        KFC.ActivityLog.http('POST', '/api/rooms', response.status);
        if (!response.ok) {
            setRoomMessage('Could not create a room. Try again.');
            return;
        }
        const data = await response.json();
        enterRoom(data.roomId);
    }

    async function joinRoom() {
        const roomId = el('roomIdInput').value.trim().toUpperCase();
        if (!roomId) {
            setRoomMessage('Enter a room id, or create a new room');
            return;
        }
        const url = '/api/rooms/' + encodeURIComponent(roomId);
        const response = await fetch(url);
        KFC.ActivityLog.http('GET', url, response.status);
        if (!response.ok) {
            setRoomMessage('Room ' + roomId + ' not found');
            return;
        }
        enterRoom(roomId);
    }

    function openRoom() {
        setRoomMessage('');
        el('roomIdInput').value = '';
        showCard('room');
        el('roomIdInput').focus();
    }

    function backToMenu() {
        setRoomMessage('');
        el('roomIdInput').value = '';
        showCard('menu');
    }

    async function pollMatchmaking() {
        const result = await postJson('/api/matchmaking', { token: account.token });
        if (!result.ok) {
            setSearching(false);
            alert('Could not reach the matchmaking service. Try again.');
            return;
        }
        if (result.data.status === 'MATCHED') {
            setSearching(false);
            enterRoom(result.data.roomId);
            return;
        }
        if (result.data.status === 'TIMED_OUT') {
            setSearching(false);
            alert('Could not find an opponent in your rating range. Try again later.');
            return;
        }
        searchTimer = setTimeout(pollMatchmaking, KFC.Config.MATCHMAKING_POLL_MS);
    }

    function play() {
        setSearching(true);
        pollMatchmaking();
    }

    function cancelSearch() {
        clearTimeout(searchTimer);
        searchTimer = null;
        setSearching(false);
    }

    function start() {
        el('registerButton').addEventListener('click', register);
        el('loginButton').addEventListener('click', login);
        el('playButton').addEventListener('click', play);
        el('roomButton').addEventListener('click', openRoom);
        el('cancelSearchButton').addEventListener('click', cancelSearch);
        el('createRoomButton').addEventListener('click', createRoom);
        el('joinRoomButton').addEventListener('click', joinRoom);
        el('cancelRoomButton').addEventListener('click', backToMenu);

        el('passwordInput').addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                login();
            }
        });
        el('roomIdInput').addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                joinRoom();
            }
        });

        showCard('login');
    }

    return { start: start };
})();

document.addEventListener('DOMContentLoaded', KFC.Home.start);
