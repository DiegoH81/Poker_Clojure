document.getElementById('btn-entrar').addEventListener('click', async () => {
    const nombre = document.getElementById('nombre-input').value.trim();
    const errorMsg = document.getElementById('login-error');

    if (!nombre) {
        errorMsg.textContent = 'Escribe un nombre';
        return;
    }

    try {
        const resp = await fetch(`/api/conectar?nombre=${encodeURIComponent(nombre)}`);
        if (!resp.ok) throw new Error();

        window.location.href = '/mesa';
    } catch (e) {
        errorMsg.textContent = 'No se pudo conectar al servidor';
    }
});


document.getElementById('nombre-input').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') document.getElementById('btn-entrar').click();
});