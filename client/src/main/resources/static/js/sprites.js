window.KFC = window.KFC || {};

KFC.Sprites = (function () {
    'use strict';

    const STATE_FOLDER = {
        IDLE: 'idle',
        MOVING: 'move',
        AIRBORNE: 'jump',
        COOLDOWN_LONG: 'long_rest',
        COOLDOWN_SHORT: 'short_rest'
    };

    const KIND_LETTER = {
        KING: 'K', QUEEN: 'Q', ROOK: 'R',
        BISHOP: 'B', KNIGHT: 'N', PAWN: 'P'
    };

    function folderCode(color, kind) {
        return KIND_LETTER[kind] + (color === 'WHITE' ? 'W' : 'B');
    }

    function frameIndex(state) {
        if (state !== 'MOVING' && state !== 'AIRBORNE') {
            return 1;
        }
        const fps = state === 'AIRBORNE' ? KFC.Config.AIRBORNE_FPS : KFC.Config.MOVING_FPS;
        return (Math.floor(Date.now() * fps / 1000) % KFC.Config.SPRITE_FRAME_COUNT) + 1;
    }

    function pathFor(piece) {
        const folder = STATE_FOLDER[piece.state];
        if (!folder) {
            return null;
        }
        return '/pieces/' + folderCode(piece.color, piece.kind)
            + '/states/' + folder + '/sprites/' + frameIndex(piece.state) + '.png';
    }

    return { pathFor: pathFor };
})();
