async function doRegister() {
    const username = document.getElementById('usernameInput').value.trim();
    const password = document.getElementById('passwordInput').value;
    const msgEl = document.getElementById('loginMessage');
    if (!username || !password) { msgEl.textContent = 'נא למלא שם משתמש וסיסמה'; return; }

    const response = await fetch('/api/register', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    const data = await response.json();
    msgEl.textContent = data.success
            ? 'נרשמת בהצלחה! עכשיו לחצי כניסה (Log in)'
            : 'שם המשתמש כבר תפוס';
}

async function doLogin() {
    const username = document.getElementById('usernameInput').value.trim();
    const password = document.getElementById('passwordInput').value;
    const msgEl = document.getElementById('loginMessage');
    if (!username || !password) { msgEl.textContent = 'נא למלא שם משתמש וסיסמה'; return; }

    const response = await fetch('/api/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });
    const data = await response.json();
    if (!data.success) {
        msgEl.textContent = 'שם משתמש או סיסמה שגויים (נרשמת קודם? לחצי הרשמה)';
        return;
    }

    // Username travels along only for immediate display (page title, "You are: X").
    // The token - not the username - is what the server actually trusts when
    // this page later tries to /app/join as a player.
    window.location.href = 'game.html?token=' + encodeURIComponent(data.token)
            + '&username=' + encodeURIComponent(data.username);
}
