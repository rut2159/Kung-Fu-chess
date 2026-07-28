window.KFC = window.KFC || {};

KFC.BoardView = (function () {
    'use strict';

    let boardEl = null;
    let gameOverBandEl = null;
    let gameOverWinnerEl = null;
    let dragGhostEl = null;

    const squares = {};
    const pieceElements = new Map();
    const cooldownElements = new Map();
    const premoveElements = new Map();

    function cellToPercent(row, col) {
        return { left: col * KFC.Config.CELL_PCT, top: row * KFC.Config.CELL_PCT };
    }

    function init(handlers) {
        boardEl = document.getElementById('boardWrap');
        gameOverBandEl = document.getElementById('gameOverBand');
        gameOverWinnerEl = document.getElementById('gameOverWinner');
        dragGhostEl = document.getElementById('dragGhost');

        const size = KFC.Config.BOARD_SIZE;
        for (let row = 0; row < size; row++) {
            for (let col = 0; col < size; col++) {
                const square = document.createElement('div');
                square.className = 'square';
                const at = cellToPercent(row, col);
                square.style.left = at.left + '%';
                square.style.top = at.top + '%';
                square.addEventListener('mousedown', handlers.onPointerDown.bind(null, row, col));
                square.addEventListener('dblclick', handlers.onDoubleClick.bind(null, row, col));
                boardEl.appendChild(square);
                squares[row + ',' + col] = square;
            }
        }

        for (let col = 0; col < size; col++) {
            const label = document.createElement('div');
            label.className = 'coordinate-label';
            label.textContent = String.fromCharCode(97 + col);
            label.style.left = (col * KFC.Config.CELL_PCT + 1) + '%';
            label.style.bottom = '1%';
            boardEl.appendChild(label);
        }
        for (let row = 0; row < size; row++) {
            const label = document.createElement('div');
            label.className = 'coordinate-label';
            label.textContent = String(size - row);
            label.style.left = '1%';
            label.style.top = (row * KFC.Config.CELL_PCT + 1) + '%';
            boardEl.appendChild(label);
        }
    }

    function element() {
        return boardEl;
    }

    function rect() {
        return boardEl.getBoundingClientRect();
    }

    function cellSizePx() {
        return rect().width / KFC.Config.BOARD_SIZE;
    }

    function cellFromPagePoint(pageX, pageY) {
        const bounds = rect();
        const size = KFC.Config.BOARD_SIZE;
        const col = Math.floor(((pageX - bounds.left) / bounds.width) * size);
        const row = Math.floor(((pageY - bounds.top) / bounds.height) * size);
        if (row < 0 || row >= size || col < 0 || col >= size) {
            return null;
        }
        return { row: row, col: col };
    }

    function render(view) {
        drawSelection(view.selected);
        syncPieces(view.pieces, view.draggingCell);
    }

    function drawSelection(selected) {
        const size = KFC.Config.BOARD_SIZE;
        for (let row = 0; row < size; row++) {
            for (let col = 0; col < size; col++) {
                const isSelected = selected !== null && selected.row === row && selected.col === col;
                squares[row + ',' + col].classList.toggle('selected', isSelected);
            }
        }
    }

    function syncPieces(pieces, draggingCell) {
        const liveIds = new Set(pieces.map(function (piece) { return piece.id; }));
        dropStale(pieceElements, liveIds);
        dropStale(cooldownElements, liveIds);
        dropStale(premoveElements, liveIds);

        pieces.forEach(function (piece) {
            updatePremove(piece);
            updateCooldown(piece);
            updateSprite(piece, draggingCell);
        });
    }

    function dropStale(map, liveIds) {
        map.forEach(function (el, id) {
            if (!liveIds.has(id)) {
                el.remove();
                map.delete(id);
            }
        });
    }

    function overlayFor(map, piece, className) {
        let el = map.get(piece.id);
        if (!el) {
            el = document.createElement('div');
            el.className = className;
            boardEl.appendChild(el);
            map.set(piece.id, el);
        }
        return el;
    }

    function updatePremove(piece) {
        const existing = premoveElements.get(piece.id);
        if (!piece.hasPremove) {
            if (existing) {
                existing.style.display = 'none';
            }
            return;
        }
        const el = overlayFor(premoveElements, piece, 'premove-highlight');
        const at = cellToPercent(piece.displayRow, piece.displayCol);
        el.style.display = 'block';
        el.style.left = at.left + '%';
        el.style.top = at.top + '%';
    }

    function updateCooldown(piece) {
        const existing = cooldownElements.get(piece.id);
        if (piece.cooldownRemaining <= 0) {
            if (existing) {
                existing.style.display = 'none';
            }
            return;
        }
        const el = overlayFor(cooldownElements, piece, 'cooldown-highlight');
        const at = cellToPercent(piece.displayRow, piece.displayCol);
        const heightPct = KFC.Config.CELL_PCT * piece.cooldownRemaining;
        el.style.display = 'block';
        el.style.left = at.left + '%';
        el.style.height = heightPct + '%';
        el.style.top = (at.top + (KFC.Config.CELL_PCT - heightPct)) + '%';
    }

    function updateSprite(piece, draggingCell) {
        const hidden = draggingCell
            && draggingCell.row === piece.row
            && draggingCell.col === piece.col;
        const path = KFC.Sprites.pathFor(piece);
        let el = pieceElements.get(piece.id);

        if (!path || hidden) {
            if (el) {
                el.style.display = 'none';
            }
            return;
        }
        if (!el) {
            el = document.createElement('img');
            el.className = 'piece-sprite';
            el.draggable = false;
            el.alt = '';
            boardEl.appendChild(el);
            pieceElements.set(piece.id, el);
        }
        const at = cellToPercent(piece.displayRow, piece.displayCol);
        el.style.display = 'block';
        el.style.left = at.left + '%';
        el.style.top = at.top + '%';
        if (!el.src.endsWith(path)) {
            el.src = path;
        }
    }

    function showDragGhost(spritePath, pageX, pageY) {
        const size = cellSizePx();
        dragGhostEl.src = spritePath;
        dragGhostEl.style.display = 'block';
        dragGhostEl.style.width = size + 'px';
        dragGhostEl.style.height = size + 'px';
        dragGhostEl.style.left = (pageX - size / 2) + 'px';
        dragGhostEl.style.top = (pageY - size / 2) + 'px';
    }

    function hideDragGhost() {
        dragGhostEl.style.display = 'none';
    }

    function showGameOver(state) {
        gameOverBandEl.style.display = state.gameOver ? 'flex' : 'none';
        if (!state.gameOver) {
            return;
        }
        const name = state.winner === 'WHITE' ? state.whiteUsername
            : state.winner === 'BLACK' ? state.blackUsername
            : null;

        if (state.disconnectedUsername) {
            // Nobody's king was captured here - the win (if any) is a forfeit
            // for abandonment, not a real chess victory. Saying just "X wins"
            // would look identical to a checkmate, which is misleading.
            gameOverWinnerEl.textContent = name
                ? name + ' wins - ' + state.disconnectedUsername + ' disconnected'
                : state.disconnectedUsername + ' left the game';
            return;
        }
        gameOverWinnerEl.textContent = name ? name + ' wins' : '';
    }

    return {
        init: init,
        element: element,
        cellSizePx: cellSizePx,
        cellFromPagePoint: cellFromPagePoint,
        render: render,
        showDragGhost: showDragGhost,
        hideDragGhost: hideDragGhost,
        showGameOver: showGameOver
    };
})();
