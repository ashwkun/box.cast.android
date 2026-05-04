const W='https://boxcast-telemetry.boxboxcric.workers.dev/query',AK='boxcast_secure_telemetry_key_2026';
let charts={},dashPwd=localStorage.getItem('boxcast_dash_pwd')||'';

async function q(sql){try{const r=await fetch(W,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${AK}`},body:JSON.stringify({query:sql})});const j=await r.json();return j.success?j.data:[];}catch(e){console.error(e);return[];}}
function isProd(){return document.getElementById('toggleProd').checked}
function ck(k,t){return isProd()?(k===`prod_${t}`||k===t):(k===`prod_${t}`||k===`debug_${t}`||k===t)}
function mc(label,val,color,icon){return`<div class="glass-sm p-3 text-center metric-glow transition"><div class="text-xl sm:text-2xl font-bold ${color||''}">${val}</div><div class="text-[10px] text-slate-500 mt-1 uppercase tracking-wider">${icon?`<i class="fa-solid fa-${icon} mr-1"></i>`:''}${label}</div></div>`}
function trendBarRow(items,colorClass){if(!items.length)return'<div class="text-[11px] text-slate-600 text-center py-3">No data yet</div>';const mx=Math.max(...items.map(i=>i.v))||1;return`<div class="flex items-end justify-between h-32 w-full pt-4 px-1 pb-1">`+items.map((r,i)=>{const h=Math.max(2,(r.v/mx)*100);return`<div class="flex flex-col items-center flex-1 h-full justify-end"><span class="text-[9px] sm:text-[10px] text-slate-300 font-bold mb-1.5">${r.display}</span><div class="w-[80%] max-w-[28px] ${colorClass||'bg-brand-500'} rounded-t-sm transition-all" style="height:${h}%"></div><span class="text-[8px] sm:text-[9px] text-slate-500 mt-2 whitespace-nowrap font-mono">${r.label}</span></div>`}).join('')+`</div>`}
function rankBarRow(items,colorClass){if(!items.length)return'<div class="text-[11px] text-slate-600 text-center py-3">No data yet</div>';const mx=Math.max(...items.map(i=>i.v))||1;return items.map((r,i)=>{const w=Math.max(2,r.v/mx*100);return`<div class="flex items-center gap-2 mb-1.5"><span class="text-[10px] text-slate-600 w-4 text-right font-mono">${i+1}</span><div class="flex-1 bg-slate-800/40 rounded-lg overflow-hidden h-7 flex items-center relative"><div class="h-full ${colorClass||'bg-brand-600/30'} rounded-lg transition-all" style="width:${w}%"></div><span class="absolute left-3 right-3 text-[11px] text-slate-300 truncate">${r.label}</span></div><span class="text-[11px] font-semibold text-slate-500 w-14 text-right">${r.display}</span></div>`}).join('')}
function doubleBarRow(items,c1,c2){if(!items.length)return'<div class="text-[11px] text-slate-600 text-center py-3">No data yet</div>';const mx=Math.max(...items.map(i=>i.v1+i.v2))||1;return`<div class="flex items-end justify-between h-32 w-full pt-4 px-1 pb-1">`+items.map((r,i)=>{const h1=(r.v1/mx)*100;const h2=(r.v2/mx)*100;return`<div class="flex flex-col items-center flex-1 h-full justify-end"><span class="text-[9px] sm:text-[10px] text-slate-300 font-bold mb-1.5">${r.display}</span><div class="w-[80%] max-w-[28px] flex flex-col justify-end" style="height:100%"><div class="w-full ${c2} rounded-t-sm transition-all" style="height:${h2}%"></div><div class="w-full ${c1} transition-all" style="height:${h1}%"></div></div><span class="text-[8px] sm:text-[9px] text-slate-500 mt-2 whitespace-nowrap font-mono">${r.label}</span></div>`}).join('')+`</div>`}

function renderBarChart(id, labels, data, opts={}) {
    const { valSuffix='', colorClass='bg-brand-500', shadowColor='rgba(139,92,246,0.3)', textColor='text-brand-400' } = opts;
    const mx = Math.max(...data) || 1;
    let html = `<div class="flex items-end justify-between h-40 w-full pt-4 px-2">`;
    labels.forEach((lbl, i) => {
        const val = data[i];
        const h = Math.max(2, (val / mx) * 100);
        const d = new Date(lbl);
        const dayStr = d.toLocaleDateString('en-US', { weekday: 'short' });
        let displayVal = val;
        if (val > 0 && val % 1 !== 0) displayVal = val.toFixed(1);
        html += `<div class="flex flex-col items-center flex-1 h-full justify-end group">
            <span class="text-[14px] sm:text-[16px] ${textColor} font-bold mb-2 transition-all group-hover:-translate-y-1 ${val===0?'opacity-30':''}">${displayVal}${valSuffix}</span>
            <div class="w-full max-w-[40px] bg-slate-800/30 group-hover:bg-slate-800/50 rounded-t-md transition-all relative flex flex-col justify-end" style="height:${h}%">
                <div class="w-full ${colorClass} rounded-t-md transition-all shadow-[0_0_15px_${shadowColor}] ${val===0?'opacity-10':'opacity-90 group-hover:opacity-100'}" style="height:100%"></div>
            </div>
            <span class="text-[10px] text-slate-500 mt-3 uppercase tracking-widest font-semibold">${dayStr}</span>
        </div>`;
    });
    html += `</div>`;
    document.getElementById(id).innerHTML = html;
}

function switchSec(id,btn){document.querySelectorAll('.nav-pill').forEach(b=>b.classList.remove('active'));document.querySelectorAll('.sec').forEach(s=>s.classList.remove('active'));btn.classList.add('active');document.getElementById(`sec-${id}`).classList.add('active');if(id==='analytics')loadAnalytics()}

// ═══════════════════════════════════════════
//  LOAD ALL ANALYTICS (single page)
// ═══════════════════════════════════════════
async function loadAnalytics(){
    const pf=isProd()?`AND app_version NOT LIKE '%-debug'`:'';
    const [hb,agg,pi,cur,fnl,ltUsers,ltSilent,ltAgg]=await Promise.all([
        q(`SELECT last_seen_date as d, COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date>=date('now','-14 days') ${pf} GROUP BY d ORDER BY d ASC`),
        q(`SELECT date_partition as d, metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE date_partition>=date('now','-7 days') GROUP BY d,k ORDER BY d ASC`),
        q(`SELECT podcast_id as p, metric_key as k, SUM(metric_value) as v FROM podcast_intelligence WHERE date_partition>=date('now','-7 days') GROUP BY p,k ORDER BY v DESC`),
        q(`SELECT metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE date_partition>=date('now','-7 days') AND k LIKE '%curated_%' GROUP BY k`),
        q(`SELECT metric_key as k, SUM(metric_value) as v FROM daily_aggregates WHERE k LIKE '%funnel_%' OR k LIKE '%play_milestone_%' OR k LIKE 'notification_%' GROUP BY k`),
        q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats ${pf?'WHERE '+pf.replace('AND ',''):''}`),
        q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date<date('now','-7 days') ${pf} AND device_id NOT IN (SELECT DISTINCT device_id FROM daily_heartbeats WHERE last_seen_date>=date('now','-7 days') ${pf})`),
        q(`SELECT metric_key as k, SUM(metric_value) as v FROM daily_aggregates GROUP BY k`)
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
    
    const labels7 = [];
    for(let i=6; i>=0; i--) {
        const d = new Date(new Date().setDate(new Date().getDate() - i));
        labels7.push(d.toISOString().split('T')[0]);
    }
    
    const dauV=labels7.map(d=>dauMap[d]||0);
    const todayDAU=dauMap[today]||0;

    // New installs per day
    const newMap={};
    labels7.forEach(d=>{newMap[d]=(dayMetrics[d]?.['new_install']||0)});
    const todayNew=newMap[today]||0;
    const todayReturn=Math.max(0,todayDAU-todayNew);

    // Total unique active in last 7 days
    const wau=await q(`SELECT COUNT(DISTINCT device_id) as c FROM daily_heartbeats WHERE last_seen_date>=date('now','-7 days') ${pf}`);
    const totalActive7d=wau[0]?.c||0;

    // Playback/engagement
    const todayPlay=(dayMetrics[today]?.['total_playback_sec']||0)/3600;
    const todayEng=(dayMetrics[today]?.['total_engagement_sec']||0)/3600;
    const todayListenTotal=todayPlay+todayEng;
    const todayEps=m7['play_episode_started']||0;

    // ═══ LIFETIME METRICS ═══
    const totalLifetimeUsers=ltUsers[0]?.c||0;
    const goneSilent=ltSilent[0]?.c||0;
    const netActive=Math.max(0,totalLifetimeUsers-goneSilent);

    // Lifetime aggregates
    const ltMap={};
    ltAgg.forEach(r=>{
        const rk=r.k.replace(/^(prod_|debug_)/,'');
        if(isProd()&&r.k.startsWith('debug_'))return;
        ltMap[rk]=(ltMap[rk]||0)+r.v;
    });
    const totalInstalls=ltMap['new_install']||0;
    const totalSessions=ltMap['session_started']||ltMap['app_open']||0;
    const totalListenSec=(ltMap['total_playback_sec']||0)+(ltMap['total_engagement_sec']||0);
    const totalListenHrs=totalListenSec/3600;

    // Averages (anchored to May 4, 2026)
    const launchDate=new Date('2026-05-04');
    const nowDate=new Date();
    const daysElapsed=Math.max(1,Math.floor((nowDate-launchDate)/(1000*60*60*24))+1);
    const instD=(totalInstalls/daysElapsed).toFixed(1);
    const instW=(totalInstalls/daysElapsed*7).toFixed(1);
    const instM=(totalInstalls/daysElapsed*30).toFixed(0);
    const listenD=(totalListenHrs/daysElapsed).toFixed(1);
    const listenW=(totalListenHrs/daysElapsed*7).toFixed(1);
    const listenM=(totalListenHrs/daysElapsed*30).toFixed(0);

    // ═══ 1a. DAILY VELOCITY ═══
    document.getElementById('pulse-daily').innerHTML=`
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-2">
            ${mc('Today Users',todayDAU,'text-white','users')}
            ${mc('New Installs',todayNew,'text-emerald-400','user-plus')}
            ${mc('Returning',todayReturn,'text-blue-400','rotate-left')}
            ${mc('Listening',todayListenTotal.toFixed(1)+'h','text-purple-400','headphones')}
        </div>`;

    // ═══ 1b. OVERALL HEALTH ═══
    document.getElementById('pulse-lifetime').innerHTML=`
        <div class="grid grid-cols-2 lg:grid-cols-3 gap-2 mb-2">
            ${mc('Lifetime Users',totalLifetimeUsers,'text-white','users-rectangle')}
            ${mc('Net Active',netActive,'text-emerald-400','user-check')}
            ${mc('Gone Silent',goneSilent,'text-red-400','skull-crossbones')}
        </div>
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-2 mt-2">
            ${mc('7d Active',totalActive7d,'text-amber-400','calendar-week')}
            ${mc('Total Sessions',totalSessions,'text-cyan-400','arrow-pointer')}
            <div class="glass-sm p-3 text-center metric-glow transition">
                <div class="text-sm sm:text-base font-bold text-indigo-400">${instD}/d &bull; ${instW}/w &bull; ${instM}/m</div>
                <div class="text-[10px] text-slate-500 mt-1 uppercase tracking-wider"><i class="fa-solid fa-download mr-1"></i>Avg Installs</div>
            </div>
            <div class="glass-sm p-3 text-center metric-glow transition">
                <div class="text-sm sm:text-base font-bold text-purple-400">${listenD}h/d &bull; ${listenW}h/w &bull; ${listenM}h/m</div>
                <div class="text-[10px] text-slate-500 mt-1 uppercase tracking-wider"><i class="fa-solid fa-headphones mr-1"></i>Avg Listening</div>
            </div>
        </div>`;

    // ═══ 2. CHARTS (HTML BARS) ═══
    const shortL=labels7.map(d=>d.slice(5));
    
    renderBarChart('c-dau', labels7, dauV, { colorClass: 'bg-brand-500', shadowColor: 'rgba(139,92,246,0.3)', textColor: 'text-brand-400' });

    const engArr=labels7.map(d=>(dayMetrics[d]?.['total_engagement_sec']||0)/3600);
    const playArr=labels7.map(d=>(dayMetrics[d]?.['total_playback_sec']||0)/3600);
    
    renderBarChart('c-listen-fg', labels7, engArr, { valSuffix: 'h', colorClass: 'bg-purple-500', shadowColor: 'rgba(168,85,247,0.3)', textColor: 'text-purple-400' });
    renderBarChart('c-listen-bg', labels7, playArr, { valSuffix: 'h', colorClass: 'bg-blue-500', shadowColor: 'rgba(59,130,246,0.3)', textColor: 'text-blue-400' });

    const newArr=labels7.map(d=>newMap[d]||0);
    const retArr=labels7.map((d,i)=>Math.max(0,(dauV[i]||0)-(newArr[i]||0)));
    
    renderBarChart('c-new', labels7, newArr, { colorClass: 'bg-emerald-500', shadowColor: 'rgba(16,185,129,0.3)', textColor: 'text-emerald-400' });
    renderBarChart('c-ret', labels7, retArr, { colorClass: 'bg-blue-500', shadowColor: 'rgba(59,130,246,0.3)', textColor: 'text-blue-400' });

    // ═══ 3. CONTENT ═══
    const podPlays={},podTime={};
    pi.forEach(r=>{
        const rk=r.k.replace(/^(prod_|debug_)/,'');
        if(isProd()&&r.k.startsWith('debug_'))return;
        if(rk==='podcast_plays')podPlays[r.p]=(podPlays[r.p]||0)+r.v;
        if(rk.startsWith('play_time_sec'))podTime[r.p]=(podTime[r.p]||0)+r.v;
    });
    const topPlays=Object.entries(podPlays).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([p,v])=>({label:p,v,display:v+' plays'}));
    const topTime=Object.entries(podTime).sort((a,b)=>b[1]-a[1]).slice(0,8).map(([p,v])=>({label:p,v,display:Math.round(v/60)+'m'}));
    document.getElementById('ct-plays').innerHTML=rankBarRow(topPlays,'bg-brand-600/25');
    document.getElementById('ct-time').innerHTML=rankBarRow(topTime,'bg-cyan-600/25');

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

    // ═══ 8.5 NOTIFICATIONS ═══
    const pGrant=fm['notification_permission_granted']||0,pDeny=fm['notification_permission_denied']||0;
    const pRate=(pGrant+pDeny)>0?Math.round((pGrant/(pGrant+pDeny))*100)+'%':'-';
    document.getElementById('fn-push').innerHTML=[mc('Granted',pGrant,'text-emerald-400','check'),mc('Denied',pDeny,'text-red-400','xmark'),mc('Push Taps',fm['notification_push_tapped']||0,'text-blue-400','hand-pointer')].join('');
    document.getElementById('fn-inapp').innerHTML=[mc('Seen',fm['notification_inapp_seen']||0,'text-purple-400','eye'),mc('Tapped',fm['notification_inapp_tapped']||0,'text-amber-400','hand-pointer')].join('');

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
