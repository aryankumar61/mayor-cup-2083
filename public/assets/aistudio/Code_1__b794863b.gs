// Rajdevi Mayor Cup 2083 - Player Dashboard backend (login + approval)
// Google Sheet > Extensions > Apps Script > paste > Deploy > Web app (Execute as: Me, Access: Anyone)
// FIRST RUN: select function "setup" and Run once (authorizes + creates sheets and default logins).
const SALT = 'rmc2083', FOLDER = 'RajdeviMayorCup2083_Photos', TTL = 21600;
const PH = ['id','ward','name','father','dob','mobile','blood','address','role','bat','bowl','jersey','cap','photo','status','by','updated','citizen','paper','papermime','citizenimg','citizenmime'];
const UH = ['username','hash','role','ward','name'];
const MH = ['no','stage','a','b','ra','wa','oa','rb','wb','ob','result','date','note'];
const AUTH = {ok:false, error:'auth'}, NOPE = {ok:false, error:'Not allowed'};

function setup(){ players_(); users_(); matches_(); cfg_(); papersSh_(); }
function tab_(n, head, seed){
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  let s = ss.getSheetByName(n);
  if (!s){ s = ss.insertSheet(n); s.appendRow(head); s.setFrozenRows(1); if (seed) seed(s); }
  return s;
}
const players_ = () => {
  const s = tab_('Players', PH), h = s.getRange(1, 1, 1, PH.length).getValues()[0];
  if (h.join() !== PH.join()) s.getRange(1, 1, 1, PH.length).setValues([PH]);
  return s;
};
const users_ = () => tab_('Users', UH, s => {
  s.appendRow(['admin', hash_('admin@2083'), 'admin', '', 'Administrator']);
  for (let w = 1; w <= 9; w++) s.appendRow(['ward' + w, hash_('ward' + w + '@2083'), 'captain', w, 'Ward ' + w + ' Captain']);
});
const matches_ = () => tab_('Matches', MH);
const papersSh_ = () => tab_('Papers', ['ward','file','mime','by','updated']);
const papers_ = () => rows_(papersSh_());
const cfg_ = () => tab_('Settings', ['key','value']);
function rows_(s){
  const v = s.getDataRange().getValues(), h = v.shift();
  return v.filter(r => r[0] !== '').map(r => { const o = {}; h.forEach((k,i) => o[k] = r[i]); return o; });
}
function hash_(p){
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, SALT + p)
    .map(b => ('0' + (b & 255).toString(16)).slice(-2)).join('');
}
function out_(o){ return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON); }
function doGet(){ return out_({ok:true, msg:'Rajdevi Mayor Cup API'}); }
function doPost(e){
  const lock = LockService.getScriptLock(); lock.waitLock(20000);
  try { return out_(route_(JSON.parse(e.postData.contents))); }
  catch (err){ return out_({ok:false, error:String(err)}); }
  finally { lock.releaseLock(); }
}
function sess_(t){ const c = t && CacheService.getScriptCache().get(t); return c ? JSON.parse(c) : null; }

function route_(d){
  if (d.action === 'login'){
    const un = String(d.user || '').trim().toLowerCase();
    const u = rows_(users_()).find(r => String(r.username) === un && r.hash === hash_(String(d.pass)));
    if (!u) return {ok:false, error:'Wrong username or password'};
    const t = Utilities.getUuid(), s = {user:u.username, role:u.role, ward:u.ward, name:u.name};
    CacheService.getScriptCache().put(t, JSON.stringify(s), TTL);
    return {ok:true, token:t, session:s};
  }
  const S = sess_(d.token), admin = S && S.role === 'admin';
  switch (d.action){
    case 'list': return list_(S);
    case 'logout': CacheService.getScriptCache().remove(d.token); return {ok:true};
    case 'changePass':
      if (!S) return AUTH;
      if (!d.pass || String(d.pass).length < 6) return {ok:false, error:'Password min 6 characters'};
      return setUser_({username:S.user, pass:d.pass, keep:true});
    case 'savePlayer': return S ? savePlayer_(d.player, S) : AUTH;
    case 'setStatus': {
      if (!S) return AUTH; if (!admin) return NOPE;
      const s = players_(), r = findRow_(s, d.id); if (r < 2) return {ok:false, error:'Not found'};
      s.getRange(r, PH.indexOf('status') + 1).setValue(d.status);
      return {ok:true};
    }
    case 'deletePlayer': {
      if (!S) return AUTH; if (!admin) return NOPE;
      const s = players_(), r = findRow_(s, d.id);
      if (r > 1){
        const pp = rows_(s).find(x => String(x.id) === String(d.id));
        [pp && pp.paper, pp && pp.citizenimg].forEach(id => { if (id) try { DriveApp.getFileById(id).setTrashed(true); } catch (e) {} });
        s.deleteRow(r);
      }
      return {ok:true};
    }
    case 'saveCfg': {
      if (!S) return AUTH; if (!admin) return NOPE;
      const c = cfg_(); c.clearContents(); c.appendRow(['key','value']);
      Object.keys(d.cfg || {}).forEach(k => c.appendRow([k, "'" + d.cfg[k]]));
      return {ok:true};
    }
    case 'saveMatch': {
      if (!S) return AUTH; if (!admin) return NOPE;
      const m = d.match, s = matches_();
      if (!m.no || +m.a === +m.b) return {ok:false, error:'Match no and two different teams required'};
      const r = findRow_(s, m.no);
      const vals = MH.map(k => k === 'date' && m[k] ? "'" + m[k] : (m[k] === undefined ? '' : m[k]));
      if (r > 1) s.getRange(r, 1, 1, MH.length).setValues([vals]); else s.appendRow(vals);
      return {ok:true};
    }
    case 'deleteMatch': {
      if (!S) return AUTH; if (!admin) return NOPE;
      const s = matches_(), r = findRow_(s, d.no); if (r > 1) s.deleteRow(r);
      return {ok:true};
    }
    case 'getDoc': {
      if (!S) return AUTH;
      let ward, fid, mime;
      if (d.kind === 'sifaris'){
        const x = papers_().find(r => String(r.ward) === String(d.id));
        if (!x) return {ok:false, error:'No sifaris uploaded'};
        ward = x.ward; fid = x.file; mime = x.mime;
      } else {
        const p = rows_(players_()).find(r => String(r.id) === String(d.id));
        if (!p || !p.citizenimg) return {ok:false, error:'No photo attached'};
        ward = p.ward; fid = p.citizenimg; mime = p.citizenmime;
      }
      if (!admin && !(S.role === 'captain' && String(ward) === String(S.ward))) return NOPE;
      const b = DriveApp.getFileById(fid).getBlob();
      return {ok:true, mime:mime || b.getContentType(), data:Utilities.base64Encode(b.getBytes())};
    }
    case 'saveSifaris': {
      if (!S) return AUTH;
      const w = S.role === 'captain' ? +S.ward : +d.ward;
      if (!w || !d.paperData || String(d.paperData).indexOf('data:') !== 0) return {ok:false, error:'Choose a file'};
      const sh = papersSh_(), r = findRow_(sh, w), old = r > 1 ? sh.getRange(r, 1, 1, 5).getValues()[0] : null;
      if (S.role === 'captain' && old) return {ok:false, error:'Sifaris already uploaded. Ask admin to change it.'};
      const x = saveDoc_(d.paperData, 'sifaris_ward' + w);
      if (old && old[1]) try { DriveApp.getFileById(old[1]).setTrashed(true); } catch (e) {}
      const vals = [w, x.id, x.mime, S.user, new Date().toISOString()];
      if (r > 1) sh.getRange(r, 1, 1, 5).setValues([vals]); else sh.appendRow(vals);
      return {ok:true};
    }
    case 'listUsers':
      if (!S) return AUTH; if (!admin) return NOPE;
      return {ok:true, users: rows_(users_()).map(u => ({username:u.username, role:u.role, ward:u.ward, name:u.name}))};
    case 'saveUser': return !S ? AUTH : !admin ? NOPE : setUser_(d.user);
    case 'deleteUser': {
      if (!S) return AUTH; if (!admin) return NOPE;
      if (d.username === 'admin') return {ok:false, error:'Cannot delete main admin'};
      const s = users_(), v = s.getDataRange().getValues();
      for (let i = 1; i < v.length; i++) if (String(v[i][0]) === d.username){ s.deleteRow(i + 1); break; }
      return {ok:true};
    }
  }
  return {ok:false, error:'Unknown action'};
}
function findRow_(s, id){
  const ids = s.getRange(1, 1, s.getLastRow(), 1).getValues().map(r => String(r[0]));
  return ids.indexOf(String(id)) + 1;
}
function setUser_(u){
  const un = String(u.username || '').trim().toLowerCase();
  if (!/^[a-z0-9_.-]{3,30}$/.test(un)) return {ok:false, error:'Username: 3-30 letters/numbers'};
  const s = users_(), v = s.getDataRange().getValues();
  let row = -1; for (let i = 1; i < v.length; i++) if (String(v[i][0]) === un){ row = i + 1; break; }
  if (row < 0 && (!u.pass || String(u.pass).length < 6)) return {ok:false, error:'Password min 6 characters'};
  const old = row > 0 ? v[row - 1] : [];
  const rec = u.keep ? [un, hash_(String(u.pass)), old[2], old[3], old[4]]
    : [un, u.pass ? hash_(String(u.pass)) : old[1], u.role === 'admin' ? 'admin' : 'captain', u.role === 'admin' ? '' : (+u.ward || ''), u.name || un];
  if (row > 0) s.getRange(row, 1, 1, 5).setValues([rec]); else s.appendRow(rec);
  return {ok:true};
}
function list_(S){
  const all = rows_(players_()), admin = S && S.role === 'admin';
  const pub = ['id','ward','name','role','bat','bowl','jersey','cap','photo','status'];
  const players = [];
  all.forEach(p => {
    const mine = S && S.role === 'captain' && String(p.ward) === String(S.ward);
    if (admin || mine) players.push(p);
    else if (p.status === 'approved'){ const o = {}; pub.forEach(k => o[k] = p[k]); players.push(o); }
  });
  const cfg = {}; rows_(cfg_()).forEach(r => cfg[r.key] = r.value);
  const papers = papers_().filter(x => admin || (S && S.role === 'captain' && String(x.ward) === String(S.ward))).map(x => x.ward);
  return {ok:true, players, matches: rows_(matches_()), cfg, papers};
}
function savePlayer_(p, S){
  const s = players_(), r = p.id ? findRow_(s, p.id) : 0;
  const old = r > 1 ? rows_(s).find(x => String(x.id) === String(p.id)) : null;
  if (S.role === 'captain'){
    if (old && String(old.ward) !== String(S.ward)) return NOPE;
    p.ward = S.ward; p.status = 'pending';
  } else p.status = p.status || (old ? old.status : 'approved');
  if (!p.id) p.id = 'P' + Date.now();
  const hasNew = p.citizenData && String(p.citizenData).indexOf('data:') === 0;
  if (S.role === 'captain'){
    if (!papers_().find(x => String(x.ward) === String(S.ward))) return {ok:false, error:'Upload ward sifaris first (one time)'};
    if (!String(p.citizen || '').trim()) return {ok:false, error:'Citizenship number required'};
  }
  p.paper = old ? old.paper : ''; p.papermime = old ? old.papermime : '';   // legacy columns, unused
  if (!hasNew){ p.citizenimg = old ? old.citizenimg : ''; p.citizenmime = old ? old.citizenmime : ''; }
  if (S.role === 'captain' && !hasNew && !p.citizenimg) return {ok:false, error:'Citizenship card photo required'};
  if (p.photo && String(p.photo).indexOf('data:') === 0) p.photo = savePhoto_(p);
  if (hasNew){
    const x = saveDoc_(p.citizenData, 'citizen_ward' + p.ward + '_' + p.id);
    if (old && old.citizenimg) try { DriveApp.getFileById(old.citizenimg).setTrashed(true); } catch (e) {}
    p.citizenimg = x.id; p.citizenmime = x.mime;
  }
  p.by = old ? old.by : S.user; p.updated = new Date().toISOString();
  const vals = PH.map(k => (k === 'dob' || k === 'mobile' || k === 'citizen') && p[k] ? "'" + p[k] : (p[k] === undefined ? '' : p[k]));
  if (r > 1) s.getRange(r, 1, 1, PH.length).setValues([vals]); else s.appendRow(vals);
  return {ok:true, id:p.id, photo:p.photo, status:p.status};
}
function savePhoto_(p){
  const it = DriveApp.getFoldersByName(FOLDER), f = it.hasNext() ? it.next() : DriveApp.createFolder(FOLDER);
  const m = p.photo.match(/^data:(.*?);base64,(.*)$/);
  const file = f.createFile(Utilities.newBlob(Utilities.base64Decode(m[2]), m[1], 'ward' + p.ward + '_' + p.id + '.jpg'));
  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  return 'https://drive.google.com/thumbnail?id=' + file.getId() + '&sz=w500';
}

// Documents (citizenship photos, ward sifaris) are stored PRIVATE in Drive (not link-shared);
// only admin / own-ward captain can fetch them through the getDoc action.
function saveDoc_(dataUrl, name){
  const it = DriveApp.getFoldersByName(FOLDER + '_Papers'), f = it.hasNext() ? it.next() : DriveApp.createFolder(FOLDER + '_Papers');
  const m = dataUrl.match(/^data:(.*?);base64,(.*)$/);
  const ext = m[1].indexOf('pdf') >= 0 ? 'pdf' : 'jpg';
  const file = f.createFile(Utilities.newBlob(Utilities.base64Decode(m[2]), m[1], name + '.' + ext));
  return {id:file.getId(), mime:m[1]};
}
