window.KFC = window.KFC || {};

KFC.Scoreboard = (function () {
    'use strict';

    function update(state) {
        setSide('white', state.whiteUsername || 'White', state.whiteScore);
        setSide('black', state.blackUsername || 'Black', state.blackScore);
    }

    function setSide(side, name, score) {
        const nameEl = document.getElementById(side + 'HistoryName');
        nameEl.setAttribute('dir', 'auto');
        nameEl.textContent = name;
        document.getElementById(side + 'HistoryScore').textContent = 'Score: ' + score;
    }

    return { update: update };
})();
