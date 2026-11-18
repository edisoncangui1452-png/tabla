const STORAGE_KEY = 'mi-tabla-v1';
const rowsInput = document.querySelector('#rows');
const columnsInput = document.querySelector('#columns');
const table = document.querySelector('#editor-table');
const count = document.querySelector('#table-count');
const toast = document.querySelector('#toast');
let toastTimer;

function validSize(value, max) {
  const number = Number(value);
  return Number.isInteger(number) && number >= 1 && number <= max;
}

function restore() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved && validSize(saved.rows, 100) && validSize(saved.columns, 30) &&
        Array.isArray(saved.cells)) {
      return {
        rows: Number(saved.rows),
        columns: Number(saved.columns),
        cells: Array.from({ length: Number(saved.rows) }, (_, row) =>
          Array.from({ length: Number(saved.columns) }, (_, col) =>
            typeof saved.cells[row]?.[col] === 'string' ? saved.cells[row][col] : ''
          )
        )
      };
    }
  } catch {
    // Ignore unavailable storage or invalid saved data.
  }
  return { rows: 4, columns: 3, cells: Array.from({ length: 4 }, () => Array(3).fill('')) };
}

let state = restore();

function save() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Editing continues even when private browsing blocks storage.
  }
}

function columnName(index) {
  let name = '';
  for (let n = index + 1; n > 0; n = Math.floor((n - 1) / 26)) {
    name = String.fromCharCode(65 + (n - 1) % 26) + name;
  }
  return name;
}

function render() {
  rowsInput.value = state.rows;
  columnsInput.value = state.columns;
  count.textContent = `${state.rows} ${state.rows === 1 ? 'fila' : 'filas'} · ${state.columns} ${state.columns === 1 ? 'columna' : 'columnas'}`;

  const head = document.createElement('thead');
  const headRow = document.createElement('tr');
  const corner = document.createElement('th');
  corner.className = 'row-label';
  corner.setAttribute('aria-label', 'Número de fila');
  headRow.append(corner);
  for (let col = 0; col < state.columns; col++) {
    const header = document.createElement('th');
    header.scope = 'col';
    header.textContent = columnName(col);
    headRow.append(header);
  }
  head.append(headRow);

  const body = document.createElement('tbody');
  for (let row = 0; row < state.rows; row++) {
    const tr = document.createElement('tr');
    const label = document.createElement('th');
    label.scope = 'row';
    label.className = 'row-label';
    label.textContent = row + 1;
    tr.append(label);
    for (let col = 0; col < state.columns; col++) {
      const td = document.createElement('td');
      const input = document.createElement('textarea');
      input.rows = 1;
      input.value = state.cells[row][col];
      input.dataset.row = row;
      input.dataset.col = col;
      input.setAttribute('aria-label', `Fila ${row + 1}, columna ${columnName(col)}`);
      if (row === 0) input.placeholder = 'Escribe aquí...';
      td.append(input);
      tr.append(td);
    }
    body.append(tr);
  }
  table.replaceChildren(head, body);
}

function notify(message) {
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('visible'), 2800);
}

document.querySelectorAll('.stepper').forEach(button => {
  button.addEventListener('click', () => {
    const input = document.getElementById(button.dataset.target);
    const current = Number(input.value);
    const next = (Number.isFinite(current) ? current : Number(input.min)) + Number(button.dataset.step);
    input.value = Math.max(Number(input.min), Math.min(Number(input.max), next));
  });
});

document.querySelector('#size-form').addEventListener('submit', event => {
  event.preventDefault();
  if (!validSize(rowsInput.value, 100) || !validSize(columnsInput.value, 30)) {
    notify('Elige entre 1 y 100 filas y entre 1 y 30 columnas.');
    return;
  }
  const rows = Number(rowsInput.value);
  const columns = Number(columnsInput.value);
  state.cells = Array.from({ length: rows }, (_, row) =>
    Array.from({ length: columns }, (_, col) => state.cells[row]?.[col] ?? '')
  );
  state.rows = rows;
  state.columns = columns;
  render();
  save();
  notify('Tamaño de la tabla actualizado');
});

table.addEventListener('input', event => {
  const input = event.target;
  if (!input.matches('textarea[data-row][data-col]')) return;
  state.cells[Number(input.dataset.row)][Number(input.dataset.col)] = input.value;
  save();
});

function clipboardContent() {
  const text = state.cells.map(row =>
    row.map(value => value.replace(/\t/g, ' ').replace(/\r?\n/g, ' ')).join('\t')
  ).join('\n');
  const htmlTable = document.createElement('table');
  const body = htmlTable.createTBody();
  for (const row of state.cells) {
    const tr = body.insertRow();
    for (const value of row) {
      const td = tr.insertCell();
      td.textContent = value;
      td.style.border = '1px solid #cccccc';
      td.style.padding = '6px';
      td.style.whiteSpace = 'pre-wrap';
    }
  }
  return { text, html: htmlTable.outerHTML };
}

async function copyTable() {
  const { text, html } = clipboardContent();
  try {
    if (navigator.clipboard?.write && window.ClipboardItem) {
      await navigator.clipboard.write([new ClipboardItem({
        'text/plain': new Blob([text], { type: 'text/plain' }),
        'text/html': new Blob([html], { type: 'text/html' })
      })]);
      notify('¡Tabla copiada! Ya puedes pegarla.');
      return;
    }
  } catch {
    // Some browsers deny rich clipboard content, but allow plain text.
  }
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text);
      notify('¡Tabla copiada! Ya puedes pegarla.');
      return;
    }
  } catch {
    // Fall back to the older copy command below.
  }
  const fallback = document.createElement('textarea');
  fallback.value = text;
  fallback.style.position = 'fixed';
  fallback.style.opacity = '0';
  document.body.append(fallback);
  fallback.select();
  const copied = document.execCommand('copy');
  fallback.remove();
  notify(copied ? '¡Tabla copiada! Ya puedes pegarla.' : 'No se pudo copiar. Abre la app con HTTPS o localhost.');
}

document.querySelector('#copy-button').addEventListener('click', copyTable);
document.querySelector('#clear-button').addEventListener('click', () => {
  if (!state.cells.some(row => row.some(value => value !== ''))) {
    notify('La tabla ya está vacía');
    return;
  }
  if (!window.confirm('¿Borrar todos los datos de la tabla?')) return;
  state.cells = Array.from({ length: state.rows }, () => Array(state.columns).fill(''));
  render();
  save();
  notify('Datos borrados');
});

render();
if ('serviceWorker' in navigator && location.protocol !== 'file:') {
  window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));
}
