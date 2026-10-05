/* AutoDrive Motors — interfaz web que consume la API REST del mismo servidor. */
(() => {
  'use strict';

  // ---------- Utilidades ----------
  const $ = (sel, root = document) => root.querySelector(sel);
  const view = $('#view');

  const cop = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
  const usd = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'USD', currencyDisplay: 'code', maximumFractionDigits: 0 });
  // Las fechas sin hora (2026-10-03) se interpretan en hora local para no correrse un día por la zona horaria
  const toDate = (s) => new Date(/^\d{4}-\d{2}-\d{2}$/.test(s) ? `${s}T00:00:00` : s);
  const fecha = (s) => s ? toDate(s).toLocaleDateString('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }) : '';

  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

  const ESTADOS = {
    DISPONIBLE: 'Disponible',
    EN_MANTENIMIENTO: 'En taller',
    VENDIDO: 'Vendido',
    EN_PROCESO: 'En proceso',
    FINALIZADO: 'Finalizado'
  };
  const TIPOS = { PREVENTIVO: 'Preventivo', CORRECTIVO: 'Correctivo', REVISION: 'Revisión' };
  const UMBRAL_DESCUENTO = 100000000;

  const estado = (e) => `<span class="estado estado-${e}">${ESTADOS[e] || e}</span>`;
  const placaTxt = (p) => /^[A-Z]{3}\d/.test(p) ? `${p.slice(0, 3)} ${p.slice(3)}` : p;
  const placa = (v, sm = false) =>
    `<span class="placa${sm ? ' placa-sm' : ''}" aria-label="Placa ${esc(v.placa || v.vehiculoPlaca)}">${esc(placaTxt(v.placa || v.vehiculoPlaca))}${sm ? '' : `<small>${esc((v.marca || '').toUpperCase())}</small>`}</span>`;

  const icon = {
    plus: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
    search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5"/><path d="m16 16 4 4"/></svg>'
  };

  // ---------- Cliente HTTP ----------
  class ApiError extends Error {
    constructor(status, body) {
      super(body?.mensaje || `Error ${status}`);
      this.status = status;
      this.errores = body?.errores || null;
    }
  }

  async function api(method, path, body) {
    let res;
    try {
      res = await fetch(path, {
        method,
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined
      });
    } catch {
      throw new ApiError(0, { mensaje: 'No hay conexión con el servidor. Revisa que la aplicación esté corriendo.' });
    }
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) throw new ApiError(res.status, data);
    return data;
  }

  // ---------- Avisos ----------
  function toast(msg, type = 'ok') {
    const el = document.createElement('div');
    el.className = `toast${type === 'error' ? ' error' : ''}`;
    el.setAttribute('role', type === 'error' ? 'alert' : 'status');
    el.textContent = msg;
    $('#toasts').append(el);
    setTimeout(() => el.remove(), type === 'error' ? 6000 : 3800);
  }

  // ---------- Modal ----------
  const modal = $('#modal');
  const modalForm = $('#modal-form');
  let onSubmit = null;

  modal.addEventListener('click', (e) => {
    if (e.target === modal || e.target.closest('[data-close]')) modal.close();
  });
  modal.addEventListener('close', () => { onSubmit = null; });

  function openModal({ title, body, actions = '', submit = null }) {
    $('#modal-title').textContent = title;
    $('#modal-body').innerHTML = body;
    $('#modal-foot').innerHTML = actions;
    $('#modal-error').hidden = true;
    onSubmit = submit;
    modal.showModal();
    const first = modal.querySelector('.modal-body input, .modal-body select, .modal-body textarea');
    if (first) first.focus();
  }

  modalForm.addEventListener('submit', async (e) => {
    if (!onSubmit) return;
    e.preventDefault();
    const btn = modalForm.querySelector('button[type="submit"]');
    modalForm.querySelectorAll('.field').forEach((f) => { f.classList.remove('invalid'); f.querySelector('.err')?.remove(); });
    $('#modal-error').hidden = true;

    const data = Object.fromEntries(new FormData(modalForm));
    if (btn) btn.disabled = true;
    try {
      await onSubmit(data);
      modal.close();
    } catch (err) {
      if (err.errores) {
        Object.entries(err.errores).forEach(([campo, msg]) => {
          const field = modalForm.querySelector(`[name="${campo}"]`)?.closest('.field');
          if (field) {
            field.classList.add('invalid');
            field.insertAdjacentHTML('beforeend', `<span class="err">${esc(msg)}</span>`);
          }
        });
      }
      const box = $('#modal-error');
      box.textContent = err.message;
      box.hidden = false;
    } finally {
      if (btn) btn.disabled = false;
    }
  });

  const field = (name, label, { type = 'text', value = '', required = false, hint = '', attrs = '' } = {}) => `
    <div class="field">
      <label for="f-${name}">${label}</label>
      ${type === 'textarea'
        ? `<textarea id="f-${name}" name="${name}" ${required ? 'required' : ''} ${attrs}>${esc(value)}</textarea>`
        : `<input id="f-${name}" name="${name}" type="${type}" value="${esc(value)}" ${required ? 'required' : ''} ${attrs}>`}
      ${hint ? `<span class="hint">${hint}</span>` : ''}
    </div>`;

  const select = (name, label, options, { value = '', placeholder = '', hint = '' } = {}) => `
    <div class="field">
      <label for="f-${name}">${label}</label>
      <select id="f-${name}" name="${name}">
        ${placeholder ? `<option value="">${placeholder}</option>` : ''}
        ${options.map(([v, t]) => `<option value="${esc(v)}" ${String(v) === String(value) ? 'selected' : ''}>${esc(t)}</option>`).join('')}
      </select>
      ${hint ? `<span class="hint">${hint}</span>` : ''}
    </div>`;

  const formActions = (label) =>
    `<button type="button" class="btn" data-close>Cancelar</button><button type="submit" class="btn btn-primary">${label}</button>`;

  const toNumber = (v) => (v === '' || v == null ? null : Number(String(v).replace(/[^\d.-]/g, '')));

  // ---------- Tasa de cambio ----------
  let tasa = null;
  async function loadRate() {
    const el = $('#rate-value');
    try {
      tasa = await api('GET', '/tasa-cambio');
      el.innerHTML = `${cop.format(tasa.usdCop)}<small>por 1 dólar, tasa del día</small>`;
    } catch {
      el.innerHTML = 'Sin datos<small>No se pudo consultar la tasa</small>';
    }
  }

  // ---------- Encabezado de página ----------
  function setHead(title, sub = '', actions = '') {
    $('#page-title').textContent = title;
    $('#page-sub').textContent = sub;
    $('#page-actions').innerHTML = actions;
    document.title = `${title} | AutoDrive Motors`;
  }

  const loading = () => { view.innerHTML = '<p class="loading">Cargando…</p>'; };

  function fail(err) {
    view.innerHTML = `
      <div class="empty">
        <h2>No se pudo cargar la información</h2>
        <p>${esc(err.message)}</p>
        <button class="btn" type="button" data-action="reload">Intentar de nuevo</button>
      </div>`;
  }

  // =========================================================
  // PATIO
  // =========================================================
  async function renderPatio() {
    setHead('Patio', '', `<button class="btn" data-action="new-maint">Enviar a taller</button><button class="btn btn-primary" data-action="new-sale">${icon.plus}Registrar venta</button>`);
    loading();
    const [vehiculos, resumen, ventas] = await Promise.all([
      api('GET', '/vehiculos'), api('GET', '/reportes/resumen'), api('GET', '/ventas')
    ]);

    const por = (e) => vehiculos.filter((v) => v.estado === e);
    const zonas = [
      ['DISPONIBLE', 'Listos para la venta', 'No hay vehículos disponibles. Registra uno desde Inventario.'],
      ['EN_MANTENIMIENTO', 'En el taller', 'Ningún vehículo está en mantenimiento.'],
      ['VENDIDO', 'Vendidos', 'Todavía no se ha vendido ningún vehículo.']
    ];

    const d = resumen.vehiculosDisponibles;
    const intro = vehiculos.length === 0
      ? 'El patio está vacío. Empieza registrando los vehículos del inventario.'
      : `Hay <b>${d} ${d === 1 ? 'vehículo listo' : 'vehículos listos'}</b> para vender, ${resumen.vehiculosEnMantenimiento} en el taller y ${resumen.vehiculosVendidos} ${resumen.vehiculosVendidos === 1 ? 'vendido' : 'vendidos'}.`;

    view.innerHTML = `
      <p class="patio-intro">${intro}</p>
      <div class="patio-layout">
        <section class="lot" aria-label="Vehículos por estado">
          ${zonas.map(([key, titulo, vacio]) => {
            const lista = por(key);
            return `
              <div class="lot-zone" data-zone="${key}">
                <h2>${titulo} <span class="count">${lista.length}</span></h2>
                ${lista.length ? `<div class="spots">${lista.map((v) => `
                  <button class="spot" type="button" data-action="vehicle" data-id="${v.id}">
                    ${placa(v)}
                    <span>
                      <span class="spot-name">${esc(v.marca)} ${esc(v.modelo)}</span><br>
                      <span class="spot-meta">${v.anio}, ${cop.format(v.precio)}</span>
                    </span>
                  </button>`).join('')}</div>` : `<p class="lot-empty">${vacio}</p>`}
              </div>`;
          }).join('')}
        </section>

        <div class="side">
          <section class="panel" aria-labelledby="h-ingresos">
            <h2 id="h-ingresos">Ingresos</h2>
            <div class="figures">
              <div>
                <div class="figure-label">Total vendido</div>
                <div class="figure-value">${cop.format(resumen.ingresosTotales)}</div>
                ${tasa ? `<div class="figure-label">${usd.format(resumen.ingresosTotales * tasa.copUsd)} al cambio de hoy</div>` : ''}
              </div>
              <div class="pair">
                <div><div class="figure-label">Ventas</div><div class="figure-value sm">${resumen.totalVentas}</div></div>
                <div><div class="figure-label">Descuentos</div><div class="figure-value sm">${cop.format(resumen.descuentosOtorgados)}</div></div>
              </div>
              <div class="pair">
                <div><div class="figure-label">Clientes</div><div class="figure-value sm">${resumen.totalClientes}</div></div>
                <div><div class="figure-label">En el taller</div><div class="figure-value sm">${resumen.mantenimientosEnProceso}</div></div>
              </div>
            </div>
          </section>

          <section class="panel" aria-labelledby="h-recientes">
            <h2 id="h-recientes">Últimas ventas</h2>
            ${ventas.length ? `<ul class="recent">${ventas.slice(0, 5).map((v) => `
              <li>
                ${placa(v, true)}
                <span class="who">${esc(v.clienteNombre)}</span>
                <span class="how">${cop.format(v.total)}${v.descuentoAplicado ? ' con descuento' : ''}, ${fecha(v.fechaVenta)}</span>
              </li>`).join('')}</ul>` : '<p class="lot-empty">Aquí aparecerán las ventas a medida que se registren.</p>'}
          </section>
        </div>
      </div>`;
  }

  // =========================================================
  // INVENTARIO
  // =========================================================
  const inv = { filtro: 'TODOS', q: '', usd: {} };

  async function renderInventario() {
    setHead('Inventario', 'Todos los vehículos del concesionario. El estado cambia solo cuando se vende o entra al taller.',
      `<button class="btn btn-primary" data-action="new-vehicle">${icon.plus}Agregar vehículo</button>`);
    loading();
    const vehiculos = await api('GET', '/vehiculos');

    const filtros = [['TODOS', 'Todos'], ['DISPONIBLE', 'Disponibles'], ['EN_MANTENIMIENTO', 'En taller'], ['VENDIDO', 'Vendidos']];
    const q = inv.q.trim().toLowerCase();
    const lista = vehiculos.filter((v) =>
      (inv.filtro === 'TODOS' || v.estado === inv.filtro) &&
      (!q || `${v.placa} ${v.marca} ${v.modelo} ${v.color || ''}`.toLowerCase().includes(q)));

    if (!vehiculos.length) {
      view.innerHTML = `
        <div class="empty">
          <h2>Todavía no hay vehículos</h2>
          <p>Registra el primer vehículo con su placa, marca, modelo y precio para empezar a vender.</p>
          <button class="btn btn-primary" data-action="new-vehicle">${icon.plus}Agregar vehículo</button>
        </div>`;
      return;
    }

    view.innerHTML = `
      <div class="toolbar">
        <div class="segmented" role="group" aria-label="Filtrar por estado">
          ${filtros.map(([k, t]) => `<button type="button" data-action="filter" data-value="${k}" aria-pressed="${inv.filtro === k}">${t}</button>`).join('')}
        </div>
        <label class="search">${icon.search}
          <input type="search" id="inv-q" placeholder="Buscar por placa, marca o modelo" value="${esc(inv.q)}" aria-label="Buscar vehículos">
        </label>
      </div>
      ${lista.length ? `
      <div class="table-wrap">
        <table>
          <thead><tr><th>Placa</th><th>Vehículo</th><th class="num">Precio</th><th>Estado</th><th><span hidden>Acciones</span></th></tr></thead>
          <tbody>
            ${lista.map((v) => `
              <tr>
                <td>${placa(v, true)}</td>
                <td><div class="cell-main">${esc(v.marca)} ${esc(v.modelo)}</div><div class="cell-sub">${v.anio}${v.color ? `, ${esc(v.color)}` : ''}</div></td>
                <td class="num">
                  ${cop.format(v.precio)}
                  ${inv.usd[v.id] ? `<span class="usd">${usd.format(inv.usd[v.id])}</span>` : `<button class="btn-link" type="button" data-action="usd" data-id="${v.id}">Ver en dólares</button>`}
                </td>
                <td>${estado(v.estado)}</td>
                <td>
                  <div class="row-actions">
                    ${v.estado === 'DISPONIBLE' ? `<button class="btn btn-sm" data-action="new-sale" data-vehiculo="${v.id}">Vender</button>` : ''}
                    ${v.estado !== 'EN_MANTENIMIENTO' ? `<button class="btn-link" data-action="new-maint" data-vehiculo="${v.id}">Enviar a taller</button>` : ''}
                    ${v.estado !== 'VENDIDO' ? `<button class="btn-link" data-action="edit-vehicle" data-id="${v.id}">Editar</button>` : ''}
                    <button class="btn-link" data-action="vehicle" data-id="${v.id}">Detalle</button>
                  </div>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>` : `<div class="empty"><h2>Sin resultados</h2><p>Ningún vehículo coincide con la búsqueda o el filtro.</p></div>`}`;

    const input = $('#inv-q');
    input.addEventListener('input', debounce(() => { inv.q = input.value; renderInventario().then(() => { const i = $('#inv-q'); i.focus(); i.setSelectionRange(i.value.length, i.value.length); }); }, 250));
  }

  function vehicleForm(v = null) {
    openModal({
      title: v ? 'Editar vehículo' : 'Agregar vehículo',
      body: `
        <div class="row-2">
          ${field('placa', 'Placa', { value: v?.placa, required: true, hint: 'Formato ABC123', attrs: 'maxlength="7" autocomplete="off" style="text-transform:uppercase"' })}
          ${field('anio', 'Año', { type: 'number', value: v?.anio ?? new Date().getFullYear(), required: true, attrs: 'min="1950" max="2100"' })}
        </div>
        <div class="row-2">
          ${field('marca', 'Marca', { value: v?.marca, required: true })}
          ${field('modelo', 'Modelo', { value: v?.modelo, required: true })}
        </div>
        <div class="row-2">
          ${field('color', 'Color', { value: v?.color })}
          ${field('precio', 'Precio en pesos', { type: 'number', value: v?.precio, required: true, attrs: 'min="0" step="1000"', hint: 'Más de $100.000.000 recibe 5 % de descuento al venderse' })}
        </div>`,
      actions: formActions(v ? 'Guardar cambios' : 'Agregar vehículo'),
      submit: async (d) => {
        const body = { placa: d.placa.toUpperCase(), marca: d.marca, modelo: d.modelo, anio: toNumber(d.anio), color: d.color || null, precio: toNumber(d.precio) };
        const r = v ? await api('PUT', `/vehiculos/${v.id}`, body) : await api('POST', '/vehiculos', body);
        toast(v ? `Vehículo ${placaTxt(r.placa)} actualizado` : `Vehículo ${placaTxt(r.placa)} agregado al inventario`);
        route();
      }
    });
  }

  async function vehicleDetail(id) {
    const [v, mants] = await Promise.all([api('GET', `/vehiculos/${id}`), api('GET', `/vehiculos/${id}/mantenimientos`)]);
    let usdTxt = '';
    try {
      const p = await api('GET', `/vehiculos/${id}/precio-usd`);
      usdTxt = usd.format(p.precioUsd);
    } catch { usdTxt = 'No disponible'; }

    openModal({
      title: `${v.marca} ${v.modelo}`,
      body: `
        <div class="detail">
          <div class="detail-top">${placa(v)} ${estado(v.estado)}</div>
          <dl>
            <dt>Año</dt><dd>${v.anio}</dd>
            <dt>Color</dt><dd>${esc(v.color) || 'Sin registrar'}</dd>
            <dt>Precio</dt><dd>${cop.format(v.precio)}</dd>
            <dt>En dólares</dt><dd>${usdTxt}</dd>
            ${v.precio > UMBRAL_DESCUENTO && v.estado !== 'VENDIDO' ? `<dt>Al venderse</dt><dd class="discount">${cop.format(v.precio * 0.95)} con 5 % de descuento</dd>` : ''}
          </dl>
          <div>
            <h3 style="font-size:18px;margin-bottom:8px">Historial de taller</h3>
            ${mants.length ? `<ul class="timeline">${mants.map((m) => `
              <li><div class="cell-main">${TIPOS[m.tipo]}: ${esc(m.descripcion)}</div>
              <div class="cell-sub">${fecha(m.fechaIngreso)}${m.fechaSalida ? ` a ${fecha(m.fechaSalida)}` : ''}, ${cop.format(m.costo)}, ${ESTADOS[m.estado].toLowerCase()}</div></li>`).join('')}</ul>`
              : '<p class="cell-sub">Este vehículo no ha pasado por el taller.</p>'}
          </div>
        </div>`,
      actions: `
        ${v.estado !== 'VENDIDO' && mants.length === 0 ? `<button type="button" class="btn btn-danger" data-action="delete-vehicle" data-id="${v.id}">Eliminar</button>` : ''}
        <span style="flex:1"></span>
        ${v.estado !== 'EN_MANTENIMIENTO' ? `<button type="button" class="btn" data-action="new-maint" data-vehiculo="${v.id}">Enviar a taller</button>` : ''}
        ${v.estado === 'DISPONIBLE' ? `<button type="button" class="btn btn-primary" data-action="new-sale" data-vehiculo="${v.id}">Vender</button>` : ''}`
    });
  }

  // =========================================================
  // CLIENTES
  // =========================================================
  async function renderClientes() {
    setHead('Clientes', 'Personas registradas que pueden comprar un vehículo. El correo y el documento no se pueden repetir.',
      `<button class="btn btn-primary" data-action="new-client">${icon.plus}Registrar cliente</button>`);
    loading();
    const [clientes, ventas] = await Promise.all([api('GET', '/clientes'), api('GET', '/ventas')]);

    if (!clientes.length) {
      view.innerHTML = `
        <div class="empty">
          <h2>Todavía no hay clientes</h2>
          <p>Registra a la primera persona interesada para poder venderle un vehículo.</p>
          <button class="btn btn-primary" data-action="new-client">${icon.plus}Registrar cliente</button>
        </div>`;
      return;
    }

    const compras = (id) => ventas.filter((v) => v.clienteId === id);
    view.innerHTML = `
      <div class="table-wrap">
        <table>
          <thead><tr><th>Cliente</th><th>Documento</th><th>Contacto</th><th class="num">Compras</th><th><span hidden>Acciones</span></th></tr></thead>
          <tbody>
            ${clientes.map((c) => {
              const cs = compras(c.id);
              return `
              <tr>
                <td><div class="cell-main">${esc(c.nombre)} ${esc(c.apellido)}</div><div class="cell-sub">Cliente desde ${fecha(c.fechaRegistro)}</div></td>
                <td>${esc(c.documento)}</td>
                <td><div>${esc(c.correo)}</div><div class="cell-sub">${esc(c.telefono) || 'Sin teléfono'}</div></td>
                <td class="num">${cs.length ? `<div class="cell-main">${cs.length}</div><div class="cell-sub">${cop.format(cs.reduce((a, v) => a + v.total, 0))}</div>` : '<span class="cell-sub">Ninguna</span>'}</td>
                <td><div class="row-actions">
                  ${cs.length ? `<button class="btn-link" data-action="client-sales" data-id="${c.id}">Ver compras</button>` : ''}
                  <button class="btn-link" data-action="edit-client" data-id="${c.id}">Editar</button>
                  ${cs.length ? '' : `<button class="btn-link" data-action="delete-client" data-id="${c.id}">Eliminar</button>`}
                </div></td>
              </tr>`;
            }).join('')}
          </tbody>
        </table>
      </div>`;
  }

  function clientForm(c = null) {
    openModal({
      title: c ? 'Editar cliente' : 'Registrar cliente',
      body: `
        <div class="row-2">
          ${field('nombre', 'Nombre', { value: c?.nombre, required: true, attrs: 'autocomplete="given-name"' })}
          ${field('apellido', 'Apellido', { value: c?.apellido, required: true, attrs: 'autocomplete="family-name"' })}
        </div>
        <div class="row-2">
          ${field('documento', 'Documento', { value: c?.documento, required: true, hint: 'Solo números', attrs: 'inputmode="numeric"' })}
          ${field('telefono', 'Teléfono', { type: 'tel', value: c?.telefono, attrs: 'autocomplete="tel"' })}
        </div>
        ${field('correo', 'Correo', { type: 'email', value: c?.correo, required: true, attrs: 'autocomplete="email"' })}
        ${field('direccion', 'Dirección', { value: c?.direccion, attrs: 'autocomplete="street-address"' })}`,
      actions: formActions(c ? 'Guardar cambios' : 'Registrar cliente'),
      submit: async (d) => {
        const body = { nombre: d.nombre, apellido: d.apellido, documento: d.documento, correo: d.correo, telefono: d.telefono || null, direccion: d.direccion || null };
        const r = c ? await api('PUT', `/clientes/${c.id}`, body) : await api('POST', '/clientes', body);
        toast(c ? `Datos de ${r.nombre} actualizados` : `${r.nombre} ${r.apellido} quedó registrado`);
        route();
      }
    });
  }

  async function clientSales(id) {
    const [c, ventas] = await Promise.all([api('GET', `/clientes/${id}`), api('GET', `/clientes/${id}/ventas`)]);
    openModal({
      title: `Compras de ${c.nombre}`,
      body: `<ul class="timeline">${ventas.map((v) => `
        <li class="with-plate">
          ${placa(v, true)}
          <div><div class="cell-main">${esc(v.vehiculoDescripcion)}</div>
          <div class="cell-sub">${fecha(v.fechaVenta)}, ${cop.format(v.total)}${v.descuentoAplicado ? ` (ahorró ${cop.format(v.descuento)})` : ''}</div></div>
        </li>`).join('')}</ul>`,
      actions: '<button type="button" class="btn" data-close>Cerrar</button>'
    });
  }

  // =========================================================
  // VENTAS
  // =========================================================
  async function renderVentas() {
    setHead('Ventas', 'La fecha, el descuento y el total los calcula el sistema al registrar la venta.',
      `<button class="btn btn-primary" data-action="new-sale">${icon.plus}Registrar venta</button>`);
    loading();
    const ventas = await api('GET', '/ventas');

    if (!ventas.length) {
      view.innerHTML = `
        <div class="empty">
          <h2>Todavía no hay ventas</h2>
          <p>Cuando vendas un vehículo disponible, la venta aparecerá aquí y el vehículo pasará a vendido.</p>
          <button class="btn btn-primary" data-action="new-sale">${icon.plus}Registrar venta</button>
        </div>`;
      return;
    }

    const total = ventas.reduce((a, v) => a + v.total, 0);
    const desc = ventas.reduce((a, v) => a + v.descuento, 0);
    view.innerHTML = `
      <div class="summary">
        <div><div class="figure-label">Ventas registradas</div><div class="figure-value">${ventas.length}</div></div>
        <div><div class="figure-label">Ingresos</div><div class="figure-value">${cop.format(total)}</div></div>
        <div><div class="figure-label">Descuentos otorgados</div><div class="figure-value">${cop.format(desc)}</div></div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Fecha</th><th>Vehículo</th><th>Cliente</th><th>Pago</th><th class="num">Precio</th><th class="num">Descuento</th><th class="num">Total</th></tr></thead>
          <tbody>
            ${ventas.map((v) => `
              <tr>
                <td class="nowrap"><div class="cell-main">${fecha(v.fechaVenta)}</div><div class="cell-sub">${new Date(v.fechaVenta).toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' })}</div></td>
                <td><div class="with-plate">${placa(v, true)}<span class="cell-sub">${esc(v.vehiculoDescripcion)}</span></div></td>
                <td><div class="cell-main">${esc(v.clienteNombre)}</div><div class="cell-sub">${esc(v.clienteCorreo)}</div></td>
                <td>${esc(v.metodoPago) || '<span class="cell-sub">Sin dato</span>'}</td>
                <td class="num">${cop.format(v.precioBase)}</td>
                <td class="num">${v.descuentoAplicado ? `<span class="discount">−${cop.format(v.descuento)}</span>` : '<span class="cell-sub">No aplica</span>'}</td>
                <td class="num cell-main">${cop.format(v.total)}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

  async function saleForm(vehiculoId = '') {
    const [clientes, disponibles] = await Promise.all([api('GET', '/clientes'), api('GET', '/vehiculos/disponibles')]);
    if (!clientes.length || !disponibles.length) {
      openModal({
        title: 'Registrar venta',
        body: `<p>${!clientes.length ? 'Primero registra al menos un cliente.' : 'No hay vehículos disponibles para vender. Los que están en el taller o ya vendidos no se pueden vender.'}</p>`,
        actions: `<button type="button" class="btn" data-close>Cerrar</button>
          <a class="btn btn-primary" href="${!clientes.length ? '#/clientes' : '#/inventario'}" data-close>${!clientes.length ? 'Ir a clientes' : 'Ir al inventario'}</a>`
      });
      return;
    }

    openModal({
      title: 'Registrar venta',
      body: `
        ${select('vehiculoId', 'Vehículo', disponibles.map((v) => [v.id, `${placaTxt(v.placa)}, ${v.marca} ${v.modelo} ${v.anio}`]), { value: vehiculoId, placeholder: 'Elige un vehículo disponible' })}
        ${select('clienteId', 'Cliente', clientes.map((c) => [c.id, `${c.nombre} ${c.apellido}, ${c.documento}`]), { placeholder: 'Elige el comprador' })}
        ${select('metodoPago', 'Forma de pago', [['Contado', 'Contado'], ['Transferencia', 'Transferencia'], ['Crédito', 'Crédito'], ['Leasing', 'Leasing']], { value: 'Transferencia' })}
        <div class="breakdown" id="breakdown" aria-live="polite"></div>`,
      actions: formActions('Registrar venta'),
      submit: async (d) => {
        if (!d.vehiculoId || !d.clienteId) throw new ApiError(400, { mensaje: 'Elige el vehículo y el cliente.' });
        const r = await api('POST', '/ventas', { clienteId: toNumber(d.clienteId), vehiculoId: toNumber(d.vehiculoId), metodoPago: d.metodoPago });
        toast(`Venta registrada por ${cop.format(r.total)}. ${placaTxt(r.vehiculoPlaca)} quedó vendido.`);
        route();
      }
    });

    const sel = $('#f-vehiculoId');
    const paint = () => {
      const v = disponibles.find((x) => String(x.id) === sel.value);
      const box = $('#breakdown');
      if (!v) { box.innerHTML = '<span class="note">Elige un vehículo para ver el total.</span>'; return; }
      const conDesc = v.precio > UMBRAL_DESCUENTO;
      const d = conDesc ? Math.round(v.precio * 5) / 100 : 0;
      box.innerHTML = `
        <div><span>Precio</span><span>${cop.format(v.precio)}</span></div>
        <div class="${conDesc ? 'discount' : 'note'}"><span>Descuento 5 %</span><span>${conDesc ? `−${cop.format(d)}` : 'No aplica, el precio no supera $100.000.000'}</span></div>
        <div class="total"><span>Total</span><span>${cop.format(v.precio - d)}</span></div>
        ${tasa ? `<div class="note"><span>En dólares</span><span>${usd.format((v.precio - d) * tasa.copUsd)}</span></div>` : ''}`;
    };
    sel.addEventListener('change', paint);
    paint();
  }

  // =========================================================
  // TALLER
  // =========================================================
  async function renderTaller() {
    setHead('Taller', 'Al entrar al taller un vehículo deja de estar disponible. Al finalizar el trabajo vuelve al patio.',
      `<button class="btn btn-primary" data-action="new-maint">${icon.plus}Registrar ingreso</button>`);
    loading();
    const mants = await api('GET', '/mantenimientos');

    if (!mants.length) {
      view.innerHTML = `
        <div class="empty">
          <h2>El taller está vacío</h2>
          <p>Registra un ingreso cuando un vehículo necesite mantenimiento preventivo, correctivo o una revisión.</p>
          <button class="btn btn-primary" data-action="new-maint">${icon.plus}Registrar ingreso</button>
        </div>`;
      return;
    }

    const orden = [...mants].sort((a, b) => (a.estado === 'EN_PROCESO' ? -1 : 1) - (b.estado === 'EN_PROCESO' ? -1 : 1));
    view.innerHTML = `
      <div class="table-wrap">
        <table>
          <thead><tr><th>Vehículo</th><th>Trabajo</th><th>Ingreso</th><th class="num">Costo</th><th>Estado</th><th><span hidden>Acciones</span></th></tr></thead>
          <tbody>
            ${orden.map((m) => `
              <tr>
                <td>${placa(m, true)}</td>
                <td><div class="cell-main">${TIPOS[m.tipo]}</div><div class="cell-sub">${esc(m.descripcion)}</div></td>
                <td><div>${fecha(m.fechaIngreso)}</div>${m.fechaSalida ? `<div class="cell-sub">Salió el ${fecha(m.fechaSalida)}</div>` : ''}</td>
                <td class="num">${cop.format(m.costo)}</td>
                <td>${estado(m.estado)}</td>
                <td><div class="row-actions">${m.estado === 'EN_PROCESO' ? `<button class="btn btn-sm" data-action="finish-maint" data-id="${m.id}">Finalizar</button>` : ''}</div></td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

  async function maintForm(vehiculoId = '') {
    const vehiculos = (await api('GET', '/vehiculos')).filter((v) => v.estado !== 'EN_MANTENIMIENTO');
    if (!vehiculos.length) {
      openModal({ title: 'Registrar ingreso al taller', body: '<p>No hay vehículos que puedan entrar al taller en este momento.</p>', actions: '<button type="button" class="btn" data-close>Cerrar</button>' });
      return;
    }
    const hoy = new Date().toLocaleDateString('en-CA');
    openModal({
      title: 'Registrar ingreso al taller',
      body: `
        ${select('vehiculoId', 'Vehículo', vehiculos.map((v) => [v.id, `${placaTxt(v.placa)}, ${v.marca} ${v.modelo}${v.estado === 'VENDIDO' ? ' (vendido, servicio postventa)' : ''}`]), { value: vehiculoId, placeholder: 'Elige el vehículo' })}
        <div class="row-2">
          ${select('tipo', 'Tipo de trabajo', Object.entries(TIPOS), { value: 'PREVENTIVO' })}
          ${field('fechaIngreso', 'Fecha de ingreso', { type: 'date', value: hoy, attrs: `max="${hoy}"` })}
        </div>
        ${field('descripcion', 'Qué se le va a hacer', { type: 'textarea', required: true, attrs: 'maxlength="500" placeholder="Ej. cambio de aceite, filtros y revisión de frenos"' })}
        ${field('costo', 'Costo estimado en pesos', { type: 'number', required: true, attrs: 'min="0" step="1000"' })}`,
      actions: formActions('Registrar ingreso'),
      submit: async (d) => {
        if (!d.vehiculoId) throw new ApiError(400, { mensaje: 'Elige el vehículo que entra al taller.' });
        const r = await api('POST', '/mantenimientos', {
          vehiculoId: toNumber(d.vehiculoId), tipo: d.tipo, descripcion: d.descripcion, costo: toNumber(d.costo), fechaIngreso: d.fechaIngreso || null
        });
        toast(`${placaTxt(r.vehiculoPlaca)} ingresó al taller`);
        route();
      }
    });
  }

  // ---------- Confirmación ----------
  function confirmar(title, text, label, action) {
    openModal({
      title,
      body: `<p>${text}</p>`,
      actions: `<button type="button" class="btn" data-close>Cancelar</button><button type="submit" class="btn btn-primary">${label}</button>`,
      submit: action
    });
  }

  // ---------- Acciones globales ----------
  const handlers = {
    reload: () => route(),
    filter: (el) => { inv.filtro = el.dataset.value; renderInventario(); },
    'new-vehicle': () => vehicleForm(),
    'edit-vehicle': async (el) => vehicleForm(await api('GET', `/vehiculos/${el.dataset.id}`)),
    vehicle: (el) => vehicleDetail(el.dataset.id),
    'delete-vehicle': (el) => confirmar('Eliminar vehículo', 'El vehículo se borrará del inventario. Esta acción no se puede deshacer.', 'Eliminar', async () => {
      await api('DELETE', `/vehiculos/${el.dataset.id}`);
      toast('Vehículo eliminado del inventario');
      route();
    }),
    usd: async (el) => {
      el.disabled = true; el.textContent = 'Consultando…';
      try {
        const p = await api('GET', `/vehiculos/${el.dataset.id}/precio-usd`);
        inv.usd[el.dataset.id] = p.precioUsd;
        el.outerHTML = `<span class="usd">${usd.format(p.precioUsd)}</span>`;
      } catch (e) { toast(e.message, 'error'); el.disabled = false; el.textContent = 'Ver en dólares'; }
    },
    'new-client': () => clientForm(),
    'edit-client': async (el) => clientForm(await api('GET', `/clientes/${el.dataset.id}`)),
    'client-sales': (el) => clientSales(el.dataset.id),
    'delete-client': (el) => confirmar('Eliminar cliente', 'Se borrarán los datos de este cliente. Esta acción no se puede deshacer.', 'Eliminar', async () => {
      await api('DELETE', `/clientes/${el.dataset.id}`);
      toast('Cliente eliminado');
      route();
    }),
    'new-sale': (el) => saleForm(el.dataset.vehiculo || ''),
    'new-maint': (el) => maintForm(el.dataset.vehiculo || ''),
    'finish-maint': (el) => confirmar('Finalizar mantenimiento', 'El trabajo quedará cerrado con la fecha de hoy y el vehículo volverá a estar disponible para la venta.', 'Finalizar', async () => {
      const r = await api('PUT', `/mantenimientos/${el.dataset.id}/finalizar`);
      toast(`${placaTxt(r.vehiculoPlaca)} salió del taller`);
      route();
    })
  };

  document.addEventListener('click', async (e) => {
    const el = e.target.closest('[data-action]');
    if (!el) return;
    const h = handlers[el.dataset.action];
    if (!h) return;
    e.preventDefault();
    // Si la acción se dispara desde un modal abierto, ciérralo antes de abrir el siguiente
    if (modal.open && !el.closest('.modal-body') && el.closest('.modal')) modal.close();
    try { await h(el); } catch (err) { toast(err.message, 'error'); }
  });

  function debounce(fn, ms) { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; }

  // ---------- Rutas ----------
  const routes = { patio: renderPatio, inventario: renderInventario, clientes: renderClientes, ventas: renderVentas, taller: renderTaller };

  async function route() {
    const name = (location.hash.replace('#/', '') || 'patio').split('?')[0];
    const render = routes[name] || renderPatio;
    document.querySelectorAll('.nav a').forEach((a) => {
      if (a.dataset.route === name) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    });
    try { await render(); } catch (err) { fail(err); }
  }

  window.addEventListener('hashchange', () => { route(); $('#main').focus({ preventScroll: true }); window.scrollTo(0, 0); });

  loadRate().finally(route);
})();
