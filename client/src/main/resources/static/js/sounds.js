window.KFC = window.KFC || {};

KFC.Sounds = (function () {
    'use strict';

    let clips = {};
    let unlocked = false;

    function init() {
        clips = {
            move: document.getElementById('soundMove'),
            capture: document.getElementById('soundCapture'),
            illegal: document.getElementById('soundIllegal'),
            gameOver: document.getElementById('soundGameOver')
        };
        document.addEventListener('pointerdown', unlock);
        document.addEventListener('keydown', unlock);
    }

    function unlock() {
        if (unlocked) {
            return;
        }
        unlocked = true;
        Object.values(clips).forEach(function (clip) {
            if (!clip) {
                return;
            }
            clip.play().then(function () {
                clip.pause();
                clip.currentTime = 0;
            }).catch(function () {});
        });
        document.removeEventListener('pointerdown', unlock);
        document.removeEventListener('keydown', unlock);
    }

    function play(name) {
        const clip = clips[name];
        if (!clip) {
            return;
        }
        clip.currentTime = 0;
        clip.play().catch(function () {});
    }

    return { init: init, play: play };
})();
