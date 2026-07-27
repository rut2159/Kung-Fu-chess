const BOARD_SIZE = 8;
const FRAME_COUNT = 5;
const DRAG_THRESHOLD_PX = 6;
const CELL_PCT = 100 / BOARD_SIZE;

const myUsername = new URLSearchParams(window.location.search).get('username');
const myToken = new URLSearchParams(window.location.search).get('token');

// If this line is missing from the file the browser loaded, it is serving a
// stale copy - most likely the kung-fu-chess-client JAR in ~/.m2 rather than
// client/src/main/resources/. Rebuild with: mvn clean install
console.info('[kung-fu-chess] client build: move-history-restore');
if (!myUsername || !myToken) {
    window.location.href = 'index.html';
}

function cellToPercent(row, col) {
    return { left: col * CELL_PCT, top: row * CELL_PCT };
}

// -----------------------------------------------------------------------
// Sprite resolution - mirrors SpriteResolver.java exactly.
// -----------------------------------------------------------------------
const STATE_FOLDER = {
    IDLE: 'idle', MOVING: 'move', AIRBORNE: 'jump',
    COOLDOWN_LONG: 'long_rest', COOLDOWN_SHORT: 'short_rest'
};
const KIND_LETTER = { KING: 'K', QUEEN: 'Q', ROOK: 'R', BISHOP: 'B', KNIGHT: 'N', PAWN: 'P' };

function pieceFolderCode(color, kind) {
    return KIND_LETTER[kind] + (color === 'WHITE' ? 'W' : 'B');
}
function currentFrameIndex(state) {
    if (state !== 'MOVING' && state !== 'AIRBORNE') return 1;
    const fps = state === 'AIRBORNE' ? 8 : 12;
    return (Math.floor(Date.now() * fps / 1000) % FRAME_COUNT) + 1;
}
function spritePath(piece) {
    const stateFolder = STATE_FOLDER[piece.state];
    if (!stateFolder) return null;
    const code = pieceFolderCode(piece.color, piece.kind);
    return '/pieces/' + code + '/states/' + stateFolder + '/sprites/' + currentFrameIndex(piece.state) + '.png';
}

// -----------------------------------------------------------------------
// Sound - the BROWSER plays these, not the server. Server-side sound (like
// desktop's ClipPlayer) would play on the server machine's speakers, not
// the speakers of whoever is looking at this page - useless for a remote
// web client. So playback has to happen here, in JS, triggered by STOMP
// messages we receive.
// -----------------------------------------------------------------------
const sounds = {
    move: document.getElementById('soundMove'),
    capture: document.getElementById('soundCapture'),
    illegal: document.getElementById('soundIllegal'),
    gameOver: document.getElementById('soundGameOver')
};
function playSound(name) {
    const el = sounds[name];
    el.currentTime = 0;
    el.play().catch(() => { /* still locked / blocked - see unlockAudioOnFirstInteraction */ });
}

// Browsers refuse programmatic audio playback until the page has seen a real
// user gesture (click/tap/key). Without this, the very first sound (e.g. an
// opponent's move arriving before you've clicked anything on this page) can
// fail silently forever in some browsers, even though every following call
// looks identical in code. Playing (and instantly pausing) each clip once,
// on the first real interaction, satisfies that requirement up front.
function unlockAudioOnFirstInteraction() {
    Object.values(sounds).forEach(el => {
        el.play().then(() => { el.pause(); el.currentTime = 0; }).catch(() => {});
    });
    document.removeEventListener('pointerdown', unlockAudioOnFirstInteraction);
    document.removeEventListener('keydown', unlockAudioOnFirstInteraction);
}
document.addEventListener('pointerdown', unlockAudioOnFirstInteraction);
document.addEventListener('keydown', unlockAudioOnFirstInteraction);

// -----------------------------------------------------------------------
// DOM setup
// -----------------------------------------------------------------------
const boardWrapEl = document.getElementById('boardWrap');
const statusEl = document.getElementById('status');
const roleInfoEl = document.getElementById('roleInfo');
const gameOverBandEl = document.getElementById('gameOverBand');
const gameOverWinnerEl = document.getElementById('gameOverWinner');
const dragGhostEl = document.getElementById('dragGhost');
const boardRect = () => boardWrapEl.getBoundingClientRect();

let latestPieces = [];
let selected = null;
let drag = null;
let wasGameOver = false;

const pieceElements = new Map();
const cooldownElements = new Map();
const premoveElements = new Map();

const squareElements = {};
for (let row = 0; row < BOARD_SIZE; row++) {
    for (let col = 0; col < BOARD_SIZE; col++) {
        const square = document.createElement('div');
        square.className = 'square';
        const p = cellToPercent(row, col);
        square.style.left = p.left + '%';
        square.style.top = p.top + '%';
        square.addEventListener('mousedown', e => onMouseDown(e, row, col));
        square.addEventListener('dblclick', () => onDoubleClick(row, col));
        boardWrapEl.appendChild(square);
        squareElements[row + ',' + col] = square;
    }
}
for (let col = 0; col < BOARD_SIZE; col++) {
    const label = document.createElement('div');
    label.className = 'coordinate-label';
    label.textContent = String.fromCharCode(97 + col);
    label.style.left = (col * CELL_PCT + 1) + '%';
    label.style.bottom = '1%';
    boardWrapEl.appendChild(label);
}
for (let row = 0; row < BOARD_SIZE; row++) {
    const label = document.createElement('div');
    label.className = 'coordinate-label';
    label.textContent = String(BOARD_SIZE - row);
    label.style.left = '1%';
    label.style.top = (row * CELL_PCT + 1) + '%';
    boardWrapEl.appendChild(label);
}

function pieceAt(row, col) {
    return latestPieces.find(p => p.row === row && p.col === col) || null;
}
function cellFromPageXY(pageX, pageY) {
    const rect = boardRect();
    const col = Math.floor(((pageX - rect.left) / rect.width) * BOARD_SIZE);
    const row = Math.floor(((pageY - rect.top) / rect.height) * BOARD_SIZE);
    if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) return null;
    return { row, col };
}
function currentCellSizePx() {
    return boardRect().width / BOARD_SIZE;
}

// -----------------------------------------------------------------------
// Rendering - mirrors RenderUI.java's draw* methods, called in the same order.
// -----------------------------------------------------------------------

function renderFrame(newState) {
    if (newState.pieces) latestPieces = newState.pieces;
    if (newState.whiteUsername !== undefined) {
        updateRoleInfo(newState);
        updateScoreboard(newState);
    }

    drawSelectedCellHighlight();
    syncPieceElements();

    if (newState.resignInSeconds !== undefined || newState.disconnectedUsername !== undefined) {
        drawDisconnectBanner(newState);
    }

    if (newState.gameOver !== undefined) {
        if (newState.gameOver && !wasGameOver) playSound('gameOver');
        wasGameOver = newState.gameOver;
        drawGameOverBand(newState);
    }
}

function drawDisconnectBanner(state) {
    const banner = document.getElementById('disconnectBanner');
    if (!state.disconnectedUsername || state.resignInSeconds === null || state.resignInSeconds === undefined) {
        banner.style.display = 'none';
        return;
    }
    banner.textContent = state.disconnectedUsername
        + ' disconnected - the game ends in ' + state.resignInSeconds + 's';
    banner.style.display = 'inline-block';
}

function drawSelectedCellHighlight() {
    for (let row = 0; row < BOARD_SIZE; row++) {
        for (let col = 0; col < BOARD_SIZE; col++) {
            squareElements[row + ',' + col].classList.toggle(
                'selected', selected !== null && selected.row === row && selected.col === col);
        }
    }
}

function syncPieceElements() {
    const currentIds = new Set(latestPieces.map(p => p.id));
    for (const [id, el] of pieceElements) if (!currentIds.has(id)) { el.remove(); pieceElements.delete(id); }
    for (const [id, el] of cooldownElements) if (!currentIds.has(id)) { el.remove(); cooldownElements.delete(id); }
    for (const [id, el] of premoveElements) if (!currentIds.has(id)) { el.remove(); premoveElements.delete(id); }

    for (const piece of latestPieces) {
        updatePremoveHighlight(piece);
        updateCooldownHighlight(piece);
        updatePieceSprite(piece);
    }
}

function updatePremoveHighlight(piece) {
    let el = premoveElements.get(piece.id);
    if (!piece.hasPremove) { if (el) el.style.display = 'none'; return; }
    if (!el) {
        el = document.createElement('div');
        el.className = 'premove-highlight';
        boardWrapEl.appendChild(el);
        premoveElements.set(piece.id, el);
    }
    const p = cellToPercent(piece.displayRow, piece.displayCol);
    el.style.display = 'block';
    el.style.left = p.left + '%';
    el.style.top = p.top + '%';
}

function updateCooldownHighlight(piece) {
    let el = cooldownElements.get(piece.id);
    if (piece.cooldownRemaining <= 0) { if (el) el.style.display = 'none'; return; }
    if (!el) {
        el = document.createElement('div');
        el.className = 'cooldown-highlight';
        boardWrapEl.appendChild(el);
        cooldownElements.set(piece.id, el);
    }
    const p = cellToPercent(piece.displayRow, piece.displayCol);
    const highlightHeightPct = CELL_PCT * piece.cooldownRemaining;
    el.style.display = 'block';
    el.style.left = p.left + '%';
    el.style.height = highlightHeightPct + '%';
    el.style.top = (p.top + (CELL_PCT - highlightHeightPct)) + '%';
}

function updatePieceSprite(piece) {
    const isBeingDragged = drag && drag.moved && drag.row === piece.row && drag.col === piece.col;
    let el = pieceElements.get(piece.id);
    const path = spritePath(piece);
    if (!path || isBeingDragged) { if (el) el.style.display = 'none'; return; }

    if (!el) {
        el = document.createElement('img');
        el.className = 'piece-sprite';
        el.draggable = false; // stops the browser's own native image-drag ghost
        boardWrapEl.appendChild(el);
        pieceElements.set(piece.id, el);
    }
    const p = cellToPercent(piece.displayRow, piece.displayCol);
    el.style.display = 'block';
    el.style.left = p.left + '%';
    el.style.top = p.top + '%';
    if (!el.src.endsWith(path)) el.src = path;
}

function drawGameOverBand(state) {
    gameOverBandEl.style.display = state.gameOver ? 'flex' : 'none';

    if (!state.winner) {
        gameOverWinnerEl.textContent = state.disconnectedUsername
            ? state.disconnectedUsername + ' left the game'
            : '';
        return;
    }

    const winnerName = state.winner === 'WHITE' ? state.whiteUsername : state.blackUsername;
    gameOverWinnerEl.textContent = (winnerName || state.winner) + ' wins!';
}

function updateRoleInfo(state) {
    document.getElementById('waitingBanner').style.display =
        (!state.whiteUsername || !state.blackUsername) ? 'block' : 'none';

    if (state.whiteUsername === myUsername) roleInfoEl.textContent = 'You are: WHITE';
    else if (state.blackUsername === myUsername) roleInfoEl.textContent = 'You are: BLACK';
    else roleInfoEl.textContent = 'You are: VIEWER (spectating)';
}

function updateScoreboard(state) {
    const whiteName = state.whiteUsername || 'White';
    const blackName = state.blackUsername || 'Black';

    const whiteNameEl = document.getElementById('whiteHistoryName');
    whiteNameEl.setAttribute('dir', 'auto');
    whiteNameEl.textContent = whiteName;
    document.getElementById('whiteHistoryScore').textContent = 'Score: ' + state.whiteScore;

    const blackNameEl = document.getElementById('blackHistoryName');
    blackNameEl.setAttribute('dir', 'auto');
    blackNameEl.textContent = blackName;
    document.getElementById('blackHistoryScore').textContent = 'Score: ' + state.blackScore;
}

// -----------------------------------------------------------------------
// Move history (mirrors desktop's MoveHistoryPanel: a TIME/MOVE table per color)
// -----------------------------------------------------------------------
// The move table is rebuilt from this array rather than being written to
// directly, so that a refresh or a reconnect can restore the whole thing
// from the server instead of starting from an empty table.
let moveHistory = [];
const seenMoveKeys = new Set();

function moveKey(entry) {
    return entry.color + '|' + entry.timestampMs + '|' + entry.notation;
}

function renderMoveHistory() {
    const bodies = {
        WHITE: document.getElementById('whiteHistoryBody'),
        BLACK: document.getElementById('blackHistoryBody')
    };
    bodies.WHITE.replaceChildren();
    bodies.BLACK.replaceChildren();

    for (const entry of moveHistory) {
        const row = document.createElement('div');
        row.className = 'moveHistoryRow';

        const timeSpan = document.createElement('span');
        timeSpan.textContent = entry.time;
        const moveSpan = document.createElement('span');
        moveSpan.textContent = entry.notation;

        row.appendChild(timeSpan);
        row.appendChild(moveSpan);

        const body = bodies[entry.color];
        if (body) body.appendChild(row);
    }
}

/**
 * Adds one entry if it isn't already known. Returns true when it was new,
 * so the caller can decide whether a sound belongs with it.
 */
function addMoveHistoryEntry(entry) {
    const key = moveKey(entry);
    if (seenMoveKeys.has(key)) return false;
    seenMoveKeys.add(key);
    moveHistory.push(entry);
    moveHistory.sort((a, b) => a.timestampMs - b.timestampMs);
    return true;
}

// A live move arriving over /topic/moves: show it, and play its sound.
function appendMoveHistoryRow(entry) {
    if (!addMoveHistoryEntry(entry)) return;
    renderMoveHistory();

    // Reuse the notation itself to decide the sound: capture notations
    // always contain "x" (e.g. "Nxe5", "exd5"), matching desktop's own
    // isCapture()-based move/capture sound choice.
    playSound(entry.notation.includes('x') ? 'capture' : 'move');
}

/**
 * Pulls the full history from the server. Runs on first load and again on
 * every reconnect - /topic/moves only ever carries moves made from now on,
 * so without this the table is empty after any refresh or dropped socket.
 *
 * Deliberately silent: replaying the sound of every move made so far would
 * be a burst of noise, not feedback.
 */
async function loadMoveHistorySnapshot() {
    try {
        const response = await fetch('/api/moves');
        ActivityLog.http('GET', '/api/moves', response.status);
        if (!response.ok) {
            // Loud on purpose. A silent failure here looks exactly like
            // "the table just doesn't work", with nothing to go on.
            console.error('[move history] GET /api/moves ->', response.status,
                '- the table cannot be restored. If this is 404, the server is '
                + 'running without GameController.moveHistory().');
            return;
        }
        const entries = await response.json();
        let added = false;
        for (const entry of entries) {
            if (addMoveHistoryEntry(entry)) added = true;
        }
        if (added) renderMoveHistory();
        console.info('[move history] restored', entries.length, 'move(s) from the server');
    } catch (e) {
        console.error('[move history] could not fetch /api/moves:', e);
    }
}

// -----------------------------------------------------------------------
// Input: drag (mirrors BoardDragHandler.java), click-to-select fallback,
// double-click = jump (mirrors Controller.jump).
// -----------------------------------------------------------------------

function sendMove(fromRow, fromCol, toRow, toCol) {
    const body = JSON.stringify({ fromRow, fromCol, toRow, toCol });
    ActivityLog.sent('/app/move', body);
    stompClient.send('/app/move', {}, body);
}
function sendJump(row, col) {
    const body = JSON.stringify({ row, col });
    ActivityLog.sent('/app/jump', body);
    stompClient.send('/app/jump', {}, body);
}

function onMouseDown(e, row, col) {
    const piece = pieceAt(row, col);
    if (!piece) return;
    e.preventDefault(); // stops the browser's own drag/selection handling from fighting our own drag logic
    drag = { row, col, startX: e.clientX, startY: e.clientY, moved: false, sprite: spritePath(piece) };
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
}

function onMouseMove(e) {
    if (!drag) return;
    const dx = e.clientX - drag.startX, dy = e.clientY - drag.startY;
    if (!drag.moved && Math.hypot(dx, dy) > DRAG_THRESHOLD_PX) {
        drag.moved = true;
        dragGhostEl.src = drag.sprite;
        dragGhostEl.style.display = 'block';
        renderFrame({});
    }
    if (drag.moved) {
        const size = currentCellSizePx();
        dragGhostEl.style.width = size + 'px';
        dragGhostEl.style.height = size + 'px';
        dragGhostEl.style.left = (e.clientX - size / 2) + 'px';
        dragGhostEl.style.top = (e.clientY - size / 2) + 'px';
    }
}

function onMouseUp(e) {
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
    if (!drag) return;

    dragGhostEl.style.display = 'none';
    const wasActualDrag = drag.moved;
    const source = { row: drag.row, col: drag.col };
    drag = null;

    if (wasActualDrag) {
        const dest = cellFromPageXY(e.clientX, e.clientY);
        if (dest && (dest.row !== source.row || dest.col !== source.col)) {
            sendMove(source.row, source.col, dest.row, dest.col);
        } else {
            renderFrame({});
        }
    } else {
        onSquareClick(source.row, source.col);
    }
}

function onSquareClick(row, col) {
    if (selected === null) {
        if (pieceAt(row, col)) selected = { row, col };
    } else if (selected.row === row && selected.col === col) {
        selected = null;
    } else {
        sendMove(selected.row, selected.col, row, col);
        selected = null;
    }
    renderFrame({});
}

function onDoubleClick(row, col) {
    if (pieceAt(row, col)) sendJump(row, col);
    selected = null;
}

let toastTimer = null;
function showRejectionToast(reason) {
    const el = document.getElementById('rejectionToast');
    el.textContent = 'Move rejected: ' + reason;
    el.style.display = 'block';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.style.display = 'none'; }, 2000);
    playSound('illegal');
}

// Keeps MOVING/AIRBORNE animation frames advancing smoothly between the
// server's own 50ms broadcasts.
setInterval(() => renderFrame({}), 40);

// A dropped WebSocket used to leave the board frozen on screen with no
// indication at all - the game looked hung when in fact only the socket had
// died. Cloud proxies drop idle connections as a matter of routine, so the
// client has to notice and come back on its own.
let stompClient = null;
let reconnectAttempts = 0;
let reconnectTimer = null;

function setConnectionStatus(text, connected) {
    statusEl.textContent = text;
    statusEl.classList.toggle('connected', connected);
}

function scheduleReconnect() {
    if (reconnectTimer) return;
    // 1s, 2s, 4s ... capped at 15s, so a server restart doesn't get hammered.
    const delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 15000);
    reconnectAttempts++;
    ActivityLog.event('reconnect attempt ' + reconnectAttempts);
    setConnectionStatus('Connection lost - reconnecting...', false);
    reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        connect();
    }, delay);
}

function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({},
        function onConnected() {
            reconnectAttempts = 0;
            setConnectionStatus('', true);
            ActivityLog.setContext('-', myUsername);
            ActivityLog.event('stomp connected');

            const joinBody = JSON.stringify({ token: myToken });
            ActivityLog.sent('/app/join', joinBody);
            stompClient.send('/app/join', {}, joinBody);

            stompClient.subscribe('/topic/game', frame => {
                ActivityLog.received('/topic/game', frame.body);
                renderFrame(JSON.parse(frame.body));
            });
            stompClient.subscribe('/topic/moves', frame => {
                ActivityLog.received('/topic/moves', frame.body);
                appendMoveHistoryRow(JSON.parse(frame.body));
            });

            // Subscribe first, then fetch: a move landing during the fetch
            // arrives on the topic and is de-duplicated against the snapshot
            // by key, so nothing is lost and nothing is shown twice.
            loadMoveHistorySnapshot();
            stompClient.subscribe('/topic/errors', frame => {
                ActivityLog.received('/topic/errors', frame.body);
                const error = JSON.parse(frame.body);
                if (error.username === myUsername) showRejectionToast(error.reason);
            });
        },
        function onError() {
            ActivityLog.event('stomp connection lost');
            // Fires both on a failed handshake and on an established socket
            // dropping later - the same recovery works for both.
            scheduleReconnect();
        });
}

connect();