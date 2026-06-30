const SUIT_ROW = { 'h':0, 'd':1, 's':2, 'c':3 }; // hearts, diamonds, spades y clubs
const RANK_COL = { 'A':0,'2':1,'3':2,'4':3,'5':4,'6':5,'7':6, '8':7,'9':8,'10':9,'J':10,'Q':11,'K':12 };
let votoEnviado = false;

function create_card(cardStr)
{
    const div = document.createElement('div');
    div.className = 'card';

    if (cardStr === 'back')
    {
        div.classList.add('back');
        return div;
    }

    const suit = cardStr.slice(-1).toLowerCase();
    const rank = cardStr.slice(0, -1).toUpperCase();

    const col  = RANK_COL[rank];
    const row  = SUIT_ROW[suit];

    div.style.backgroundPosition = `-${col * 48}px -${row * 64}px`;
    return div;
}

const FICHA_BASE = [ [0,0], [0,1], [0,2], [0,3], 
                     [192,0], [192,1], [192,2], [192,3] ];

function create_money_token(tipo, variante)
{
    const div = document.createElement('div');
    div.className = 'ficha';

    const [xBase, fila] = FICHA_BASE[tipo];
    const x = xBase + variante * 48;
    const y = fila * 48;
    div.style.backgroundPosition = `-${x}px -${y}px`;

    return div;
}

const DENOMS = [ { valor: 25000, tipo:7 },
                 { valor: 5000, tipo:6 },
                 { valor: 1000, tipo:5 },
                 { valor: 500, tipo:4 },
                 { valor: 100, tipo:3 },
                 { valor: 25, tipo:2 },
                 { valor: 10, tipo:1 },
                 { valor: 1, tipo:0 } ];

function render_tokens(contenedor, monto)
{
    contenedor.innerHTML = '';

    let resto = monto, current_tokens = 0;

    for (const { valor, tipo } of DENOMS)
    {
        if (current_tokens >= 8 || resto <= 0)
            break;

        const cant = Math.floor(resto / valor);

        if (cant > 0)
        {
            contenedor.appendChild(create_money_token(tipo, Math.min(cant - 1, 3)));
            resto -= cant * valor;
            current_tokens++;
        }
    }
}

const BOTON_IMG = {
    'Apostar':'assets/boton_apostar.png',
    'Subir':  'assets/boton_apostar.png',
    'Igualar':'assets/boton_igualar.png',
    'Pasar':  'assets/boton_pasar.png',
    'Retirarse':'assets/boton_retirar.png',
};

function renderEstado(data)
{
    const esperandoDiv = document.getElementById('esperando-overlay');
    if (data.esperando)
    {
        esperandoDiv.style.display = 'flex';
        esperandoDiv.querySelector('.esperando-texto').textContent = data.mensaje;
        return;
    }
    else
    {
        esperandoDiv.style.display = 'none';
    }

    const ganadorDiv = document.getElementById('ganador-overlay');
    if (data.ganador)
    {
        document.getElementById('pot-display').style.display = 'none';
        
        ganadorDiv.style.display = 'flex';
        ganadorDiv.querySelector('.ganador-texto').textContent =
            data.ganador.tipo === 'abandono'
              ? `${data.ganador.nombre} gana $${data.ganador.pozo} (todos se retiraron)`
              : `${data.ganador.nombre} gana $${data.ganador.pozo} con ${data.ganador.jugada}`;

        const listaJugadores = data.jugadores || data.players || [];
        const jugadoresVivos = listaJugadores.filter(j => (j.dinero || j.money || 0) > 0).length;

        if (votoEnviado) {
            document.getElementById('btn-reinicio').style.display = 'none';
            document.getElementById('btn-reinicio-total').style.display = 'none';
            document.getElementById('texto-espera-reinicio').style.display = 'block';
        } else {
            if (jugadoresVivos <= 1) {
                document.getElementById('btn-reinicio').style.display = 'none';
                document.getElementById('btn-reinicio-total').style.display = 'block';
                ganadorDiv.querySelector('.ganador-texto').textContent += " ¡Y ES EL CAMPEÓN ABSOLUTO!";
            } else {
                document.getElementById('btn-reinicio').style.display = 'block';
                document.getElementById('btn-reinicio-total').style.display = 'none';
            }
            document.getElementById('texto-espera-reinicio').style.display = 'none';
        }

        const esperaDiv = document.getElementById('texto-espera-reinicio');
        esperaDiv.textContent = `Esperando jugadores (${data.votos_reinicio || 0}/4)...`;
    }
    else
    {
        votoEnviado = false; 
        
        ganadorDiv.style.display = 'none';
        document.getElementById('pot-display').style.display = 'block';
        
        document.getElementById('btn-reinicio').style.display = 'block';
        document.getElementById('texto-espera-reinicio').style.display = 'none';
    }

    const yo = data.jugadores.find(j => j.id === data.jugador_id_actual);

    const rivales = data.jugadores.filter(j => j.id !== data.jugador_id_actual);

    const rivalesDiv = document.getElementById('rivales');
    rivalesDiv.innerHTML = '';
    rivales.forEach(r => {
        const box = document.createElement('div');
        box.className = 'rival-box';

        const nombre = document.createElement('div');
        nombre.className = 'rival-nombre';
        nombre.textContent = r.nombre;

        const dinero = document.createElement('div');
        dinero.className = 'rival-dinero';
        dinero.textContent = `$${r.dinero}`;

        const cartas = document.createElement('div');
        cartas.className = 'rival-cartas';

        let cartasParaMostrar = ['back', 'back']; 
        
        if (data.ganador && r.cartas && r.cartas.length >= 2) {
            cartasParaMostrar = r.cartas.slice(0, 2); // reveal cards
        }

        cartasParaMostrar.forEach(c => cartas.appendChild(create_card(c)));

        box.append(nombre, dinero, cartas);
        rivalesDiv.appendChild(box);
    });

    document.getElementById('pot').textContent = data.mesa.pot;
    const cartasMesa = document.getElementById('cartas-mesa');
    cartasMesa.innerHTML = '';
    (data.mesa.cartas || []).forEach(c => cartasMesa.appendChild(create_card(c)));

    document.getElementById('nombre-jugador').textContent = yo.nombre;
    document.getElementById('dinero-jugador').textContent = yo.dinero;

    const cartasJugador = document.getElementById('cartas-jugador');
    cartasJugador.innerHTML = '';

    (yo.cartas || []).slice(0, 2).forEach(c => cartasJugador.appendChild(create_card(c)));

    render_tokens(document.getElementById('fichas-display'), yo.dinero);

    const accionesDiv = document.getElementById('acciones');
    accionesDiv.innerHTML = '';
    if (data.turno_id === data.jugador_id_actual)
    {
        (data.opciones || []).forEach(op => {
        const btn = document.createElement('button');
        const imgSrc = BOTON_IMG[op];
        
        btn.style.backgroundImage  = `url('${imgSrc}')`;
        btn.style.backgroundSize   = '112px 64px';
        btn.style.backgroundRepeat = 'no-repeat';
        btn.title = op;

        btn.onclick = () => enviarAccion(op);
        accionesDiv.appendChild(btn);
        });
    }
    else
    {
        const msg = document.createElement('div');
        msg.id = 'turno-msg';
        msg.textContent = 'ESPERANDO...';
        accionesDiv.appendChild(msg);
    }
}

async function actualizar()
{
    try
    {
        const resp = await fetch('/api/messages');
        const data = await resp.json();
        renderEstado(data);
    }
    catch(e)
    {
        console.error('Error:', e);
    }
}

async function enviarAccion(accion)
{
    const necesita = ['Apostar','Bet','Subir','Raise'].includes(accion);
    let v = 0;

    if (necesita) {
        let mensaje = '¿Cuánto?';
        if (accion === 'Subir' || accion === 'Raise') {
            mensaje = '¿Cuánto EXTRA deseas aumentar sobre la apuesta actual?';
        }

        const dineroDisplay = document.getElementById('dinero-jugador').textContent;
        const maxDinero = parseInt(dineroDisplay) || 0;

        const input = prompt(`${mensaje}\n(Dinero disponible: $${maxDinero})`, '100');
        if (input === null)
            return;

        v = parseInt(input);
        if (isNaN(v) || v <= 0) {
            alert('Por favor, ingresa una cantidad válida.');
            return;
        }

        if (v > maxDinero) {
            alert(`Solo tienes $${maxDinero}. Se enviará un All-In con todo tu dinero.`);
            v = maxDinero;
        }
    }

    await fetch(`/api/decision?accion=${encodeURIComponent(accion)}&valor=${v}`);
}

async function enviarReinicio() {
    votoEnviado = true;
    document.getElementById('btn-reinicio').style.display = 'none';
    document.getElementById('texto-espera-reinicio').style.display = 'block';
    await fetch(`/api/decision?accion=Reinicio&valor=0`);
}

async function enviarReinicioTotal() {
    votoEnviado = true;
    document.getElementById('btn-reinicio').style.display = 'none';
    document.getElementById('btn-reinicio-total').style.display = 'none';
    document.getElementById('texto-espera-reinicio').style.display = 'block';
    await fetch(`/api/decision?accion=ReinicioTotal&valor=0`);
}

async function iniciar()
{
    try
    {
        const resp = await fetch('/api/messages');
        if (!resp.ok)
            throw new Error();
        renderEstado(await resp.json());
        setInterval(actualizar, 1000);
    }
    catch(_)
    {
        console.info('Sin servidor -> datos de prueba');
        renderEstado(PRUEBA);
    }
}

iniciar();