const API = '/api';
let allListings = [], allRequests = [], toastTimer;
const $ = s => document.querySelector(s);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
async function api(path, options={}) {
  const res = await fetch(API + path, {headers:{'Content-Type':'application/json'}, ...options});
  if (!res.ok) { let msg = 'Something went wrong.'; try { const e=await res.json(); msg=e.message||e.error||msg; } catch {} throw new Error(msg); }
  if (res.status===204) return null;
  return res.json();
}
function notify(message) {
  const el=$('#toast'); el.textContent=message; el.classList.add('show');
  clearTimeout(toastTimer); toastTimer=setTimeout(()=>el.classList.remove('show'),3200);
}
function foodEmoji(category='') {
  return ({'Prepared meals':'🍱','Fruits & vegetables':'🥦','Bakery':'🥖','Dairy':'🥛','Packaged food':'🥫','Other':'🍽️'})[category] || '🍽️';
}
function prettyStatus(s){return (s||'AVAILABLE').toLowerCase().replace(/^\w/,c=>c.toUpperCase());}
async function loadAll() {
  try {
    [allListings,allRequests] = await Promise.all([api('/listings'),api('/requests')]);
    renderStats(); renderListings(); renderRequests();
  } catch(e) { notify('Cannot connect to the server. Start the Java application first.'); }
}
async function loadStats() {
  try {
    const d=await api('/dashboard');
    $('#totalListings').textContent=d.totalListings;
    $('#availableListings').textContent=d.availableListings;
    $('#totalRequests').textContent=d.totalRequests;
    $('#quantityRescued').textContent=Number(d.quantityRescued).toLocaleString();
  } catch {}
}
function renderStats() {
  const collected=allListings.filter(x=>x.status==='COLLECTED').reduce((s,x)=>s+Number(x.quantity||0),0);
  $('#totalListings').textContent=allListings.length;
  $('#availableListings').textContent=allListings.filter(x=>x.status==='AVAILABLE').length;
  $('#totalRequests').textContent=allRequests.length;
  $('#quantityRescued').textContent=collected.toLocaleString();
}
function renderListings() {
  const term=$('#searchInput').value.toLowerCase(), cat=$('#categoryFilter').value;
  const items=allListings.filter(x=>(!cat||x.category===cat)&&[x.title,x.location,x.providerName,x.category].join(' ').toLowerCase().includes(term));
  $('#listingGrid').innerHTML=items.length?items.map(x=>`
    <article class="food-card">
      <div class="food-card-top"><span class="food-symbol">${foodEmoji(x.category)}</span><span class="status ${x.status.toLowerCase()}">${esc(prettyStatus(x.status))}</span></div>
      <h3>${esc(x.title)}</h3>
      <div class="food-meta">${esc(x.category||'Food')} · ${esc(x.quantity)} ${esc(x.unit||'items')}<br>⌖ ${esc(x.location)}<br>◷ Pickup by: ${esc(x.pickupDeadline||'Contact provider')}<br>Storage: ${esc(x.storageDetails||'Not specified')}<br>Allergens: ${esc(x.allergens||'Not specified')}<br>Expiry: ${esc(x.expiryInfo||'Not specified')}</div>
      <div class="food-card-bottom"><strong>${esc(x.providerName||'Community provider')}</strong><button class="small-btn" data-request="${x.id}" ${x.status!=='AVAILABLE'?'disabled':''}>${x.status==='AVAILABLE'?'Request pickup':'Not available'}</button></div>
    </article>`).join(''):'<div class="empty">No matching food listings yet. Be the first to share surplus food.</div>';
  document.querySelectorAll('[data-request]').forEach(b=>b.addEventListener('click',()=>openRequest(Number(b.dataset.request))));
}
function renderRequests() {
  $('#requestList').innerHTML=allRequests.length?allRequests.map(r=>{
    const food=allListings.find(x=>x.id===r.listingId);
    const transitions = r.status==='PENDING' ? ['APPROVED','REJECTED'] : r.status==='APPROVED' ? ['COLLECTED','REJECTED'] : [];
    const statusControl = transitions.length ? `<select data-status="${r.id}" aria-label="Update request status"><option value="">Update status…</option>${transitions.map(status=>`<option value="${status}">${esc(prettyStatus(status))}</option>`).join('')}</select>` : '';
    return `<article class="request-row"><div><strong>${esc(food?.title||'Food listing')} · ${esc(r.organisation)}</strong><p>Requested by ${esc(r.recipientName)} · ${esc(r.message||'No extra message')}</p></div><div class="request-actions"><span class="status ${r.status.toLowerCase()}">${esc(prettyStatus(r.status))}</span>${statusControl}</div></article>`;
  }).join(''):'<div class="empty">No pickup requests yet. Requests will appear here when recipients respond to a listing.</div>';
  document.querySelectorAll('[data-status]').forEach(sel=>sel.addEventListener('change',async()=>{
    if(!sel.value)return;
    try{await api(`/requests/${sel.dataset.status}/status`,{method:'PATCH',body:JSON.stringify({status:sel.value})});notify('Request status updated.');await loadAll();}
    catch(e){notify(e.message);sel.value='';}
  }));
}
function openRequest(id) {
  const item=allListings.find(x=>x.id===id); if(!item)return;
  $('#requestForm').elements.listingId.value=id;
  $('#modalFoodName').textContent=`${item.title} · ${item.quantity} ${item.unit} · ${item.location}`;
  $('#requestModal').classList.remove('hidden');
}
$('#closeModal').addEventListener('click',()=>$('#requestModal').classList.add('hidden'));
$('#requestModal').addEventListener('click',e=>{if(e.target.id==='requestModal')e.currentTarget.classList.add('hidden')});
$('#listingForm').addEventListener('submit',async e=>{
  e.preventDefault(); const f=new FormData(e.currentTarget);
  const data=Object.fromEntries(f.entries()); data.quantity=Number(data.quantity); data.safetyChecked=f.has('safetyChecked');
  try{await api('/listings',{method:'POST',body:JSON.stringify(data)});e.currentTarget.reset();notify('Your food listing is live!');await loadAll();location.hash='listings';}
  catch(err){notify(err.message);}
});
$('#requestForm').addEventListener('submit',async e=>{
  e.preventDefault(); const data=Object.fromEntries(new FormData(e.currentTarget).entries());data.listingId=Number(data.listingId);
  try{await api('/requests',{method:'POST',body:JSON.stringify(data)});e.currentTarget.reset();$('#requestModal').classList.add('hidden');notify('Pickup request sent!');await loadAll();}
  catch(err){notify(err.message);}
});
$('#searchInput').addEventListener('input',renderListings);
$('#categoryFilter').addEventListener('change',renderListings);
$('#refreshBtn').addEventListener('click',loadAll);
loadAll();
