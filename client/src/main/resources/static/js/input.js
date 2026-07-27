window.KFC = window.KFC || {};

KFC.Input = (function () {
    'use strict';

    let callbacks = null;
    let drag = null;
    let selected = null;

    function attach(handlers) {
        callbacks = handlers;
    }

    function selectedCell() {
        return selected;
    }

    function draggingCell() {
        return drag && drag.moved ? { row: drag.row, col: drag.col } : null;
    }

    function onPointerDown(row, col, event) {
        const piece = callbacks.pieceAt(row, col);
        if (!piece) {
            return;
        }
        event.preventDefault();
        drag = {
            row: row,
            col: col,
            startX: event.clientX,
            startY: event.clientY,
            moved: false,
            sprite: KFC.Sprites.pathFor(piece)
        };
        window.addEventListener('mousemove', onPointerMove);
        window.addEventListener('mouseup', onPointerUp);
    }

    function onPointerMove(event) {
        if (!drag) {
            return;
        }
        const dx = event.clientX - drag.startX;
        const dy = event.clientY - drag.startY;
        if (!drag.moved && Math.hypot(dx, dy) > KFC.Config.DRAG_THRESHOLD_PX) {
            drag.moved = true;
            callbacks.onRedraw();
        }
        if (drag.moved) {
            KFC.BoardView.showDragGhost(drag.sprite, event.clientX, event.clientY);
        }
    }

    function onPointerUp(event) {
        window.removeEventListener('mousemove', onPointerMove);
        window.removeEventListener('mouseup', onPointerUp);
        if (!drag) {
            return;
        }

        KFC.BoardView.hideDragGhost();
        const wasDrag = drag.moved;
        const from = { row: drag.row, col: drag.col };
        drag = null;

        if (!wasDrag) {
            onClick(from.row, from.col);
            return;
        }
        const to = KFC.BoardView.cellFromPagePoint(event.clientX, event.clientY);
        if (to && (to.row !== from.row || to.col !== from.col)) {
            callbacks.onMove(from, to);
        } else {
            callbacks.onRedraw();
        }
    }

    function onClick(row, col) {
        if (selected === null) {
            if (callbacks.pieceAt(row, col)) {
                selected = { row: row, col: col };
            }
        } else if (selected.row === row && selected.col === col) {
            selected = null;
        } else {
            const from = selected;
            selected = null;
            callbacks.onMove(from, { row: row, col: col });
        }
        callbacks.onRedraw();
    }

    function onDoubleClick(row, col) {
        if (callbacks.pieceAt(row, col)) {
            callbacks.onJump({ row: row, col: col });
        }
        selected = null;
    }

    function clearSelection() {
        selected = null;
    }

    return {
        attach: attach,
        selectedCell: selectedCell,
        draggingCell: draggingCell,
        clearSelection: clearSelection,
        onPointerDown: onPointerDown,
        onDoubleClick: onDoubleClick
    };
})();
