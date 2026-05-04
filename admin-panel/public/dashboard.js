const W='https://boxcast-telemetry.boxboxcric.workers.dev/query',AK='boxcast_secure_telemetry_key_2026';
let charts={},dashPwd=localStorage.getItem('boxcast_dash_pwd')||'';

async function q(sql){try{const r=await fetch(W,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${AK}`},body:JSON.stringify({query:sql})});const j=await r.json();return j.success?j.data:[];}catch(e){console.error(e);return[];}}
function isProd(){return document.getElementById('toggleProd').checked}
function ck(k,t){return isProd()?(k===`prod_${t}`||k===t):(k===`prod_${t}`||k===`debug_${t}`||k===t)}
function mc(label,val,color,icon){return`<div class="glass-sm p-3 text-center metric-glow transition"><div class="text-xl sm:text-2xl font-bold ${color||''}">${val}</div><div class="text-[10px] text-slate-500 mt-1 uppercase tracking-wider">${icon?`<i class="fa-solid fa-${icon} mr-1"></i>`:''}${label}</div></div>`}
function barRow(items,colorClass){if(!items.length)return'<div class="text-[11px] text-slate-600 text-center py-3">No data yet</div>';const mx=items[0].v||1;return items.map((r,i)=>{const w=Math.max(6,r.v/mx*100);return`<div class="flex items-center gap-2"><span class="text-[10px] text-slate-600 w-4 text-right font-mono">${i+1}</span><div class="flex-1 bg-slate-800/40 rounded-lg overflow-hidden h-7 flex items-center"><div class="bar-fill h-full ${colorClass||'bg-brand-600/30'} rounded-lg flex items-center px-2.5" style="width:${w}%"><span class="text-[11px] text-slate-300 truncate">${r.label}</span></div></div><span class="text-[11px] font-semibold text-slate-500 w-16 text-right">${r.display}</span></div>`}).join('')}

function switchSec(id,btn){document.querySelectorAll('.nav-pill').forEach(b=>b.classList.remove('active'));document.querySelectorAll('.sec').forEach(s=>s.classList.remove('active'));btn.classList.add('active');document.getElementById(`sec-${id}`).classList.add('active');if(id==='analytics')loadAnalytics()}

function chart(id,type,labels,datasets,opts){if(charts[id])charts[id].destroy();const ctx=document.getElementById(id)?.getContext('2d');if(!ctx)return;charts[id]=new Chart(ctx,{type,data:{labels,datasets},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{labels:{color:'#64748b',font:{size:10}}}},scales:{x:{ticks:{color:'#334155',font:{size:9}},grid:{color:'rgba(255,255,255,0.02)'}},y:{ticks:{color:'#334155',font:{size:9}},grid:{color:'rgba(255,255,255,0.02)'},beginAtZero:true}},...opts}})}

// ═══════════════════════════════════════════
//  LOAD ALL ANALYTICS (single page)
// ═══════════════════════════════════════════
async function loadAnalytics(){
    const pf=isProd()?`AND app_version NOT LIKE '%-debug'`:'';
    const [hb,agg,pi,cur,fnl]=await Promise.all([
        q(`SELECT last_seen_date as d, COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date>=date('now','-14 days') ${pf} GROUP BY d ORDER BY d ASC`),
        q(`SELECT date_partition as d, metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE date_partition>=date('now','-7 days') GROUP BY d,k ORDER BY d ASC`),
        q(`SELECT podcast_id as p, metric_key as k, SUM(metric_value) as v FROM podcast_intelligence WHERE date_partition>=date('now','-7 days') GROUP BY p,k ORDER BY v DESC`),
        q(`SELECT metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE date_partition>=date('now','-7 days') AND k LIKE '%curated_%' GROUP BY k`),
        q(`SELECT metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE k LIKE '%funnel_%' OR k LIKE '%play_milestone_%' GROUP BY k`)
    ]);

    const today=new Date().toISOString().split('T')[0];

    // ── Process aggregates ──
    const m7={};
    const dayMetrics={};
    agg.forEach(r=>{
        const rk=r.k.replace(/^(prod_|debug_)/,'');
        if(isProd()&&r.k.startsWith('debug_'))return;
        m7[rk]=(m7[rk]||0)+r.v;
        if(!dayMetrics[r.d])dayMetrics[r.d]={};
        dayMetrics[r.d][rk]=(dayMetrics[r.d][rk]||0)+r.v;
    });

    // ── Process heartbeats for user breakdown ──
    const dauMap={};
    hb.forEach(r=>{dauMap[r.d]=r.c});
    const labels7=Object.keys(dauMap).slice(-7);
    const dauV=labels7.map(d=>dauMap[d]||0);
    const todayDAU=dauMap[today]||dauV[dauV.length-1]||0;

    // New installs per day
    const newMap={};
    labels7.forEach(d=>{newMap[d]=(dayMetrics[d]?.['new_install']||0)});
    const todayNew=newMap[today]||0;
    const todayReturn=Math.max(0,todayDAU-todayNew);

    // Total unique active in last 7 days
    const wau=await q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date>=date('now','-7 days') ${pf}`);
    const totalActive7d=wau[0]?.c||0;

    // Churn: devices seen 8-14 days ago but NOT in last 7 days
    const churnData=await q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date>=date('now','-14 days') AND last_seen_date<date('now','-7 days') ${pf} AND device_id NOT IN (SELECT DISTINCT device_id FROM daily_heartbeats WHERE last_seen_date>=date('now','-7 days') ${pf})`);
    const churned=churnData[0]?.c||0;

    // Per-day churn approximation (devices seen on day X but not seen after)
    const churnByDay=[];
    for(let i=0;i<7;i++){
        const d=labels7[i];if(!d)continue;
        const cr=await q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date='${d}' ${pf} AND device_id NOT IN (SELECT DISTINCT device_id FROM daily_heartbeats WHERE last_seen_date>'${d}' ${pf})`);
        churnByDay.push(cr[0]?.c||0);
    }

    // Playback/engagement
    const todayPlay=(dayMetrics[today]?.['total_playback_sec']||0)/3600;
    const todayEng=(dayMetrics[today]?.['total_engagement_sec']||0)/3600;
    const todayEps=m7['play_episode_started']||0;

    // ═══ 1. PULSE ═══
    document.getElementById('pulse').innerHTML=`
        <div class="grid grid-cols-2 lg:grid-cols-5 gap-2">
            ${mc('Today Users',todayDAU,'text-white','users')}
            ${mc('New',todayNew,'text-emerald-400','user-plus')}
            ${mc('Returning',todayReturn,'text-blue-400','rotate-left')}
            ${mc('Listening',todayPlay.toFixed(1)+'h','text-purple-400','headphones')}
            ${mc('7d Active',totalActive7d,'text-amber-400','calendar-week')}
        </div>
        <div class="flex items-center gap-3 mt-2 text-[11px]">
            <span class="text-slate-600"><i class="fa-solid fa-skull-crossbones text-red-500/60 mr-1"></i>${churned} devices churned (last 7d)</span>
        </div>`;

    // ═══ 2. CHARTS ═══
    const shortL=labels7.map(d=>d.slice(5));
    
    // Create gradients for lines/bars
    const ctxDau = document.getElementById('c-dau')?.getContext('2d');
    const gradDau = ctxDau?.createLinearGradient(0, 0, 0, 300);
    if(gradDau){ gradDau.addColorStop(0, 'rgba(139,92,246,0.25)'); gradDau.addColorStop(1, 'rgba(139,92,246,0.0)'); }

    const defaultChartOpts = {
        plugins: {
            legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 10, family: 'Inter' }, usePointStyle: true, padding: 15 } },
            tooltip: { backgroundColor: 'rgba(15, 23, 42, 0.9)', titleColor: '#f8fafc', bodyColor: '#cbd5e1', padding: 10, cornerRadius: 8, displayColors: true }
        },
        scales: {
            x: { grid: { display: false }, ticks: { color: '#64748b', font: { size: 10, family: 'Inter' } } },
            y: { border: { display: false }, grid: { color: 'rgba(255,255,255,0.03)', drawTicks: false }, ticks: { color: '#64748b', font: { size: 10, family: 'Inter' }, padding: 10 }, beginAtZero: true }
        },
        interaction: { mode: 'index', intersect: false }
    };

    chart('c-dau','line',shortL,[{label:'DAU',data:dauV,borderColor:'#8b5cf6',backgroundColor:gradDau||'rgba(139,92,246,0.1)',fill:true,tension:0.4,borderWidth:2,pointRadius:0,pointHoverRadius:6,pointBackgroundColor:'#8b5cf6'}], defaultChartOpts);

    const engArr=labels7.map(d=>(dayMetrics[d]?.['total_engagement_sec']||0)/3600);
    const playArr=labels7.map(d=>(dayMetrics[d]?.['total_playback_sec']||0)/3600);
    chart('c-listen','bar',shortL,[
        {label:'Foreground',data:engArr,backgroundColor:'#8b5cf6',borderRadius:4, borderSkipped: false},
        {label:'Background',data:playArr,backgroundColor:'#38bdf8',borderRadius:4, borderSkipped: false}
    ], {...defaultChartOpts, scales: { ...defaultChartOpts.scales, x: { ...defaultChartOpts.scales.x, stacked: true }, y: { ...defaultChartOpts.scales.y, stacked: true } }});

    const newArr=labels7.map(d=>newMap[d]||0);
    const retArr=labels7.map((d,i)=>Math.max(0,(dauV[i]||0)-(newArr[i]||0)));
    chart('c-newret','bar',shortL,[
        {label:'Returning',data:retArr,backgroundColor:'#3b82f6',borderRadius:4, borderSkipped: false},
        {label:'New',data:newArr,backgroundColor:'#10b981',borderRadius:4, borderSkipped: false}
    ], {...defaultChartOpts, scales: { ...defaultChartOpts.scales, x: { ...defaultChartOpts.scales.x, stacked: true }, y: { ...defaultChartOpts.scales.y, stacked: true } }});

    chart('c-churn','bar',shortL,[{label:'Devices Lost',data:churnByDay,backgroundColor:'#ef4444',borderRadius:4, borderSkipped: false}], defaultChartOpts);

    // ═══ 3. CONTENT ═══
    const podPlays={},podTime={};
    pi.forEach(r=>{
        const rk=r.k.replace(/^(prod_|debug_)/,'');
        if(isProd()&&r.k.startsWith('debug_'))return;
        if(rk==='podcast_plays')podPlays[r.p]=(podPlays[r.p]||0)+r.v;
        if(rk.startsWith('play_time_sec'))podTime[r.p]=(podTime[r.p]||0)+r.v;
    });
    const topPlays=Object.entries(podPlays).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([p,v])=>({label:p.substring(0,30),v,display:v+' plays'}));
    const topTime=Object.entries(podTime).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([p,v])=>({label:p.substring(0,30),v,display:Math.round(v/60)+'m'}));
    document.getElementById('ct-plays').innerHTML=barRow(topPlays,'bg-brand-600/25');
    document.getElementById('ct-time').innerHTML=barRow(topTime,'bg-cyan-600/25');

    // ═══ 4. CURATED ═══
    const cm={};cur.forEach(r=>{const rk=r.k.replace(/^(prod_|debug_)/,'');if(isProd()&&r.k.startsWith('debug_'))return;cm[rk]=(cm[rk]||0)+r.v});
    const cImp=cm['curated_block_impression']||0,cTap=cm['curated_card_tapped']||0,cPlay=cm['curated_episode_played']||0;
    document.getElementById('cur-metrics').innerHTML=[mc('Impressions',cImp,'text-indigo-400','eye'),mc('Taps',cTap,'text-blue-400','hand-pointer'),mc('Plays',cPlay,'text-emerald-400','play'),mc('Tap Rate',cImp>0?((cTap/cImp)*100).toFixed(1)+'%':'-','text-amber-400','percent')].join('');

    // Vibes
    const vibeKeys=Object.keys(cm).filter(k=>k.startsWith('curated_vibe_impression_'));
    const vibes=vibeKeys.map(k=>{const id=k.replace('curated_vibe_impression_','');return{id,imp:cm[k]||0,taps:cm[`curated_tap_vibe_${id}`]||0,plays:cm[`curated_play_vibe_${id}`]||0}}).sort((a,b)=>b.taps-a.taps);
    if(vibes.length){const mx=vibes[0].taps||1;document.getElementById('cur-vibes').innerHTML=vibes.map(v=>{const w=Math.max(8,v.taps/mx*100);const rate=v.imp>0?(v.taps/v.imp*100).toFixed(1):0;return`<div class="glass-sm p-3"><div class="flex items-center justify-between mb-1.5"><span class="text-xs font-medium capitalize">${v.id.replace(/_/g,' ')}</span><span class="text-[10px] text-slate-600">${rate}%</span></div><div class="w-full bg-slate-800/40 rounded-full h-2 mb-1.5"><div class="bar-fill h-full bg-gradient-to-r from-brand-600 to-indigo-500 rounded-full" style="width:${w}%"></div></div><div class="flex gap-3 text-[10px] text-slate-500"><span><i class="fa-solid fa-eye mr-0.5 text-slate-700"></i>${v.imp}</span><span><i class="fa-solid fa-hand-pointer mr-0.5 text-blue-600"></i>${v.taps}</span><span><i class="fa-solid fa-play mr-0.5 text-emerald-600"></i>${v.plays}</span></div></div>`}).join('')}else{document.getElementById('cur-vibes').innerHTML='<div class="text-[11px] text-slate-600 text-center py-3">No curated data yet</div>'}

    document.getElementById('cur-pos').innerHTML=[mc('Pos 0-2',cm['curated_tap_pos_0_2']||0,'text-emerald-400',''),mc('Pos 3-5',cm['curated_tap_pos_3_5']||0,'text-yellow-400',''),mc('Pos 6+',cm['curated_tap_pos_6_plus']||0,'text-red-400','')].join('');

    // Curated pods
    const cpMap={};pi.forEach(r=>{const rk=r.k.replace(/^(prod_|debug_)/,'');if(rk==='curated_taps'||rk==='curated_plays'){if(!cpMap[r.p])cpMap[r.p]={t:0,p:0};if(rk==='curated_taps')cpMap[r.p].t+=r.v;if(rk==='curated_plays')cpMap[r.p].p+=r.v}});
    const cpList=Object.entries(cpMap).sort((a,b)=>(b[1].t+b[1].p)-(a[1].t+a[1].p)).slice(0,6).map(([p,v])=>({label:p.substring(0,28),v:v.t+v.p,display:`${v.t}t/${v.p}p`}));
    document.getElementById('cur-pods').innerHTML=barRow(cpList,'bg-emerald-600/20');

    // ═══ 5. FEATURES ═══
    document.getElementById('feat-grid').innerHTML=[
        mc('Episodes',m7['play_episode_started']||0,'','play'),
        mc('Subscribes',m7['action_subscribe']||0,'text-emerald-400','plus'),
        mc('Unsubs',m7['action_unsubscribe']||0,'text-red-400','minus'),
        mc('Searches',m7['discovery_search']||0,'text-blue-400','magnifying-glass'),
        mc('Skip Fwd',m7['feature_skip_forward']||0,'','forward'),
        mc('Skip Back',m7['feature_skip_backward']||0,'','backward'),
        mc('Speed',m7['feature_playback_speed']||0,'','gauge-high'),
        mc('Sleep',((m7['feature_sleep_timer']||0)+(m7['feature_sleep_timer_fixed']||0)+(m7['feature_sleep_timer_eoe']||0)),'','moon'),
        mc('Downloads',m7['feature_download']||0,'','download'),
        mc('Likes',m7['action_like']||0,'text-pink-400','heart'),
        mc('Unlikes',m7['action_unlike']||0,'','heart-crack'),
        mc('Mark Done',m7['action_mark_complete']||0,'','check'),
    ].join('');

    // ═══ 6. SESSION ═══
    document.getElementById('sess-grid').innerHTML=[
        mc('Transitions',m7['play_episodes_this_session']||0,'','shuffle'),
        mc('Restored',m7['play_session_restored']||0,'text-emerald-400','clock-rotate-left'),
        mc('Resumed',m7['play_session_resumed']||0,'text-blue-400','rotate-right'),
        mc('Dismissed',m7['action_player_dismissed']||0,'text-red-400','xmark'),
        mc('Sleep Fire',m7['feature_sleep_timer_fired']||0,'text-purple-400','bell-slash'),
    ].join('');

    // ═══ 7. NAVIGATION ═══
    const sk=Object.keys(m7).filter(k=>k.startsWith('screen_')).sort((a,b)=>m7[b]-m7[a]);
    const hEl=document.getElementById('nav-heat');
    if(!sk.length){hEl.innerHTML='<div class="text-[11px] text-slate-600 col-span-full text-center py-3">No data</div>'}
    else{const mx=m7[sk[0]]||1;hEl.innerHTML=sk.map(k=>{const n=k.replace('screen_','').replace(/_/g,' ');const v=m7[k];const i=Math.max(.1,v/mx);return`<div class="text-center p-3 rounded-xl transition" style="background:rgba(99,102,241,${i*0.35})"><div class="text-lg font-bold">${v}</div><div class="text-[10px] text-slate-300 mt-0.5 capitalize">${n}</div></div>`}).join('')}

    // ═══ 8. FUNNEL ═══
    const fm={};fnl.forEach(r=>{const rk=r.k.replace(/^(prod_|debug_)/,'');if(isProd()&&r.k.startsWith('debug_'))return;fm[rk]=(fm[rk]||0)+r.v});
    document.getElementById('fn-onboard').innerHTML=[mc('Genres',fm['funnel_onboarding_genres_picked']||0,'text-blue-400',''),mc('Completed',fm['funnel_onboarding_completed']||0,'text-emerald-400',''),mc('Skipped',fm['funnel_onboarding_skipped']||0,'text-red-400','')].join('');
    document.getElementById('fn-activ').innerHTML=[mc('First Plays',fm['funnel_first_play']||0,'',''),mc('< 1min',fm['funnel_first_play_under_1m']||0,'text-emerald-400',''),mc('1-5min',fm['funnel_first_play_1_5m']||0,'text-yellow-400',''),mc('5min+',fm['funnel_first_play_over_5m']||0,'text-red-400','')].join('');

    // ═══ 9. FRICTION ═══
    document.getElementById('fric-grid').innerHTML=`
        <div class="glass p-4 border border-red-500/10"><div class="text-[10px] text-red-400 uppercase font-bold mb-1"><i class="fa-solid fa-hand-pointer mr-1"></i>Rage Taps</div><div class="text-2xl font-bold">${m7['rage_tap']||0}</div></div>
        <div class="glass p-4 border border-orange-500/10"><div class="text-[10px] text-orange-400 uppercase font-bold mb-1"><i class="fa-solid fa-magnifying-glass mr-1"></i>Failed Searches</div><div class="text-2xl font-bold">${(m7['failed_search']||0)+(m7['friction_search_empty']||0)}</div></div>
        <div class="glass p-4 border border-yellow-500/10"><div class="text-[10px] text-yellow-400 uppercase font-bold mb-1"><i class="fa-solid fa-bug mr-1"></i>Playback Errors</div><div class="text-2xl font-bold">${(m7['crash_report']||0)+(m7['friction_playback_error']||0)}</div></div>`;
}

// ═══ NOTIFICATIONS ═══
function initNotify(){['inputTitle','inputBody','inputImage','inputType','inputRoute'].forEach(id=>{const el=document.getElementById(id);if(el){el.addEventListener('input',pvUpdate);el.addEventListener('change',pvUpdate)}});updateAuthUI()}
function pvUpdate(){const t=document.getElementById('inputTitle').value||'BoxCast Update';const b=document.getElementById('inputBody').value||'';const type=document.getElementById('inputType').value;const img=document.getElementById('inputImage').value;const route=document.getElementById('inputRoute').value;function md(s){let h=s.replace(/</g,'&lt;').replace(/>/g,'&gt;');h=h.replace(/\*\*(.*?)\*\*/g,'<b>$1</b>');h=h.replace(/\*(.*?)\*/g,'<i>$1</i>');h=h.replace(/\n/g,'<br/>');return h}document.getElementById('pvTitle').textContent=t;document.getElementById('pvBody').innerHTML=md(b);document.getElementById('iaTitle').textContent=t;document.getElementById('iaBody').innerHTML=md(b);const n=document.getElementById('phoneNotif');if(type==='push'||type==='both')n.classList.add('show');else n.classList.remove('show');const ia=document.getElementById('phoneInApp');if(type==='in-app'||type==='both')ia.classList.add('show');else ia.classList.remove('show');const pw=document.getElementById('pvImgW'),iw=document.getElementById('iaImgW');if(img&&isUrl(img)){document.getElementById('pvImg').src=img;pw.classList.add('show');document.getElementById('iaImg').src=img;iw.classList.add('show')}else{pw.classList.remove('show');iw.classList.remove('show')}document.getElementById('iaAction').style.display=route?'inline-block':'none'}
function isUrl(s){try{new URL(s);return true}catch(_){return false}}
function openAuth(){document.getElementById('authModal').classList.remove('hidden');document.getElementById('authModal').classList.add('flex');document.getElementById('inputToken').value=dashPwd}
function closeAuth(){document.getElementById('authModal').classList.add('hidden');document.getElementById('authModal').classList.remove('flex')}
function saveAuth(){const t=document.getElementById('inputToken').value.trim();if(t){localStorage.setItem('boxcast_dash_pwd',t);dashPwd=t;updateAuthUI();closeAuth()}}
function updateAuthUI(){const b=document.getElementById('authBtn');if(dashPwd){b.innerHTML='<i class="fa-solid fa-lock-open"></i><span>OK</span>';b.className='text-[11px] bg-emerald-500/10 text-emerald-400 px-2.5 py-1 rounded-lg border border-emerald-500/15 flex items-center gap-1'}else{b.innerHTML='<i class="fa-solid fa-lock"></i><span>Key</span>';b.className='text-[11px] bg-slate-800 text-slate-500 px-2.5 py-1 rounded-lg border border-slate-800 flex items-center gap-1'}}
function decrypt(b64,pwd){try{const bytes=Uint8Array.from(atob(b64),c=>c.charCodeAt(0));let r='';for(let i=0;i<bytes.length;i++)r+=String.fromCharCode(bytes[i]^pwd.charCodeAt(i%pwd.length));return r}catch(e){return''}}
async function handleSend(e){e.preventDefault();if(!dashPwd){openAuth();return}const btn=document.getElementById('sendBtn');btn.disabled=true;btn.innerHTML='<i class="fa-solid fa-circle-notch fa-spin"></i> ...';try{const cfg=await(await fetch('config.json')).json();const pat=decrypt(cfg.encrypted_token,dashPwd);if(!pat.startsWith('ghp_'))throw new Error('Bad key');const p={ref:'master',inputs:{title:document.getElementById('inputTitle').value,body:document.getElementById('inputBody').value.replace(/\n/g,'\\n'),type:document.getElementById('inputType').value,route:document.getElementById('inputRoute').value||'',image:document.getElementById('inputImage').value||'',target:document.getElementById('inputTarget').value||'all_users'}};const r=await fetch('https://api.github.com/repos/ashwkun/box.cast.android/actions/workflows/manual_notify.yml/dispatches',{method:'POST',headers:{'Accept':'application/vnd.github.v3+json','Authorization':`token ${pat}`,'Content-Type':'application/json'},body:JSON.stringify(p)});if(r.ok){const st=document.getElementById('statusText');st.classList.remove('opacity-0');setTimeout(()=>st.classList.add('opacity-0'),4000)}else{throw new Error((await r.json()).message||'Failed')}}catch(err){alert('Error: '+err.message);if(err.message.includes('Bad key')){localStorage.removeItem('boxcast_dash_pwd');dashPwd='';updateAuthUI();openAuth()}}finally{btn.disabled=false;btn.innerHTML='<i class="fa-regular fa-paper-plane"></i> Send'}}

function initApp(){loadAnalytics();initNotify()}
