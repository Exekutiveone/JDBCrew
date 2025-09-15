/********************
 * CONFIG (Frontend-only)
 * JDBC ist NUR im Backend. Hier nur HTTP zu deiner API.
 ********************/
const STORAGE_KEY = 'dbwebgui.baseUrl';
const CONFIG = {
  get BASE(){ return localStorage.getItem(STORAGE_KEY) || location.origin; },
  EP: {
    upload:  (db) => `/api/db/${db}/upload`,                   // POST (multipart/form-data)
    download:(db) => `/api/db/${db}/download`,                 // GET (stream; Content-Length empfohlen)
    relocate:(from,to) => `/api/admin/relocate?from=${from}&to=${to}`, // POST
    sync:    (db) => `/api/admin/sync?db=${db}`,               // POST
    data:    (db, f) => `/api/db/${db}/data${f ? `?${encodeURIComponent(f)}`: ''}` // GET JSON
  },
  headers: () => ({ /* z.B. Authorization: 'Bearer …' */ })
};

// UI init Base URL
const baseInput = document.getElementById('baseUrlInput');
const activeBase = document.getElementById('activeBase');
const saveBase = document.getElementById('saveBase');
function refreshBase(){ activeBase.textContent = CONFIG.BASE; baseInput.value = CONFIG.BASE; }
refreshBase();
saveBase.onclick = () => {
  localStorage.setItem(STORAGE_KEY, baseInput.value.trim() || location.origin);
  refreshBase();
  toast('Base URL gespeichert');
};

// Helpers
const $ = (id) => document.getElementById(id);
const fmtBytes = (n) => {
  if (!n && n !== 0) return '—';
  const u = ['B','KB','MB','GB','TB'];
  const i = Math.max(0, Math.floor(Math.log(n)/Math.log(1024)));
  return `${(n/Math.pow(1024,i)).toFixed(1)} ${u[i]}`;
};
function setProgress(elBar, elInfo, loaded, total) {
  let pct = 0; if (total && total > 0) pct = Math.round((loaded/total)*100);
  elBar.style.width = (pct || 5) + '%';
  elInfo.textContent = total ? `${pct}%  (${fmtBytes(loaded)} / ${fmtBytes(total)})` : `${fmtBytes(loaded)} geladen…`;
}
function toast(msg, ok=true){
  const n = document.createElement('div');
  n.textContent = msg;
  n.style.position='fixed'; n.style.right='16px'; n.style.bottom='16px'; n.style.padding='10px 12px';
  n.style.background = ok ? '#064e3b' : '#3b0a0a';
  n.style.border = '1px solid #1f2937'; n.style.borderRadius='10px'; n.style.color = '#e5e7eb';
  n.style.boxShadow='0 10px 30px rgba(0,0,0,.35)';
  document.body.appendChild(n);
  setTimeout(()=> n.remove(), 2500);
}

// Upload (XHR for real progress)
$('btnUpload').onclick = () => {
  const file = $('fileInput').files[0];
  if (!file) return toast('Bitte Datei auswählen', false);
  const db = $('dbSelect').value;
  const url = CONFIG.EP.upload(db);

  const fd = new FormData();
  fd.append('file', file);

  const xhr = new XMLHttpRequest();
  xhr.open('POST', CONFIG.BASE + url);
  Object.entries(CONFIG.headers()).forEach(([k,v])=> xhr.setRequestHeader(k,v));
  xhr.upload.onprogress = (e) => setProgress($('uploadBar'), $('uploadInfo'), e.loaded, e.total);
  xhr.onload = () => {
    if (xhr.status >= 200 && xhr.status < 300) toast('Upload erfolgreich');
    else toast('Upload fehlgeschlagen: ' + xhr.status, false);
    setProgress($('uploadBar'), $('uploadInfo'), 0, 0);
    $('uploadBar').style.width = '0%'; $('uploadInfo').textContent='0%';
  };
  xhr.onerror = () => { toast('Netzwerkfehler beim Upload', false); };
  xhr.send(fd);
};

// Download (XHR + blob + progress)
$('btnDownload').onclick = () => {
  const db = $('dbSelect').value;
  const url = CONFIG.EP.download(db);
  const xhr = new XMLHttpRequest();
  xhr.open('GET', CONFIG.BASE + url);
  xhr.responseType = 'blob';
  Object.entries(CONFIG.headers()).forEach(([k,v])=> xhr.setRequestHeader(k,v));
  xhr.onprogress = (e) => setProgress($('downloadBar'), $('downloadInfo'), e.loaded, e.total || 0);
  xhr.onload = () => {
    if (xhr.status >= 200 && xhr.status < 300) {
      const blob = xhr.response;
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `export-${db}.bin`;
      document.body.appendChild(a); a.click(); a.remove();
      URL.revokeObjectURL(a.href);
      toast('Download bereit');
    } else {
      toast('Download fehlgeschlagen: ' + xhr.status, false);
    }
    setProgress($('downloadBar'), $('downloadInfo'), 0, 0);
    $('downloadBar').style.width='0%'; $('downloadInfo').textContent='0%';
  };
  xhr.onerror = () => toast('Netzwerkfehler beim Download', false);
  xhr.send();
};

// Relocate & Sync
$('btnRelocate').onclick = async () => {
  const from = $('relFrom').value, to = $('relTo').value;
  if (from === to) return toast('Quelle und Ziel müssen unterschiedlich sein', false);
  try {
    const res = await fetch(CONFIG.BASE + CONFIG.EP.relocate(from,to), { method:'POST', headers: CONFIG.headers() });
    const msg = await res.text();
    $('opsInfo').textContent = `Relocate: ${res.status} ${msg || ''}`;
    toast(res.ok ? 'Relocate OK' : 'Relocate Fehler', res.ok);
  } catch {
    toast('Relocate Netzwerkfehler', false);
  }
};
$('btnSync').onclick = async () => {
  const db = $('dbSelect').value;
  try {
    const res = await fetch(CONFIG.BASE + CONFIG.EP.sync(db), { method:'POST', headers: CONFIG.headers() });
    const msg = await res.text();
    $('opsInfo').textContent = `Sync: ${res.status} ${msg || ''}`;
    toast(res.ok ? 'Sync OK' : 'Sync Fehler', res.ok);
  } catch {
    toast('Sync Netzwerkfehler', false);
  }
};

// Daten laden & anzeigen
let lastData = [];
function renderTable(rows){
  const tbl = $('dataTable');
  tbl.innerHTML = '';
  if (!rows || !rows.length) { tbl.innerHTML = '<tr><td class="muted">Keine Daten</td></tr>'; $('stats').textContent = '0 Zeilen'; return; }
  const cols = [...new Set(rows.flatMap(r => Object.keys(r)))];
  const thead = document.createElement('thead');
  thead.innerHTML = '<tr>' + cols.map(c=>`<th>${c}</th>`).join('') + '</tr>';
  tbl.appendChild(thead);
  const tbody = document.createElement('tbody');
  for (const r of rows) {
    const tr = document.createElement('tr');
    tr.innerHTML = cols.map(c=>`<td>${String(r[c] ?? '')}</td>`).join('');
    tbody.appendChild(tr);
  }
  tbl.appendChild(tbody);
  $('stats').textContent = `${rows.length} Zeilen · ${cols.length} Spalten`;
}
$('btnLoadData').onclick = async () => {
  const db = $('dbSelect').value;
  const filter = $('filterText').value.trim();
  try {
    const res = await fetch(CONFIG.BASE + CONFIG.EP.data(db, filter), { headers: CONFIG.headers() });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    lastData = Array.isArray(data) ? data : [];
    renderTable(lastData);
    toast('Daten geladen');
  } catch {
    toast('Fehler beim Laden der Daten', false);
  }
};
// CSV Export
$('btnExport').onclick = () => {
  if (!lastData.length) return toast('Keine Daten zum Export', false);
  const cols = [...new Set(lastData.flatMap(r => Object.keys(r)))];
  const lines = [cols.join(',')];
  for (const r of lastData) lines.push(cols.map(c=>JSON.stringify(r[c] ?? '')).join(','));
  const blob = new Blob([lines.join('\n')], { type:'text/csv' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob); a.download = 'export.csv'; a.click();
  setTimeout(()=> URL.revokeObjectURL(a.href), 1500);
};

// CORS Hinweis
if (new URL(CONFIG.BASE, location.href).origin !== location.origin) {
  toast('Hinweis: CORS im Backend freischalten (Origin erlauben)');
}
