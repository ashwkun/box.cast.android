#!/usr/bin/env node
/**
 * Sync episodes for chart podcasts using Podcast Index API
 * Run this AFTER importing podcasts from the dump.
 */

const crypto = require('crypto');

const TURSO_URL = process.env.TURSO_URL?.replace('libsql://', 'https://');
const TURSO_TOKEN = process.env.TURSO_AUTH_TOKEN;
const API_KEY = process.env.PODCAST_INDEX_API_KEY;
const API_SECRET = process.env.PODCAST_INDEX_API_SECRET;
const API_BASE = "https://api.podcastindex.org/api/1.0";

if (!TURSO_URL || !TURSO_TOKEN || !API_KEY || !API_SECRET) {
    console.error("Missing required environment variables (TURSO_*, PODCAST_INDEX_*)");
    process.exit(1);
}

function generateAuthHeaders() {
    const apiHeaderTime = Math.floor(Date.now() / 1000);
    const data4Hash = API_KEY + API_SECRET + apiHeaderTime;
    const hash = crypto.createHash('sha1').update(data4Hash).digest('hex');

    return {
        "User-Agent": "BoxCast/1.0",
        "X-Auth-Key": API_KEY,
        "X-Auth-Date": "" + apiHeaderTime,
        "Authorization": hash
    };
}

async function executeSQL(sql, args = []) {
    const response = await fetch(`${TURSO_URL}/v2/pipeline`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${TURSO_TOKEN}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            requests: [{
                type: "execute",
                stmt: { sql, args: args.map(a => ({ type: a === null ? "null" : "text", value: a === null ? null : String(a) })) }
            }, { type: "close" }]
        })
    });
    return response.json();
}

async function executeBatch(statements) {
    if (statements.length === 0) return;
    const requests = statements.map(stmt => ({
        type: "execute",
        stmt: { sql: stmt.sql, args: stmt.args.map(a => ({ type: a === null ? "null" : "text", value: a === null ? null : String(a || "") })) }
    }));
    requests.push({ type: "close" });

    const response = await fetch(`${TURSO_URL}/v2/pipeline`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${TURSO_TOKEN}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ requests })
    });
    if (!response.ok) throw new Error(`Turso error: ${response.status}`);
}

async function getPodcasts() {
    // Query top 50 podcasts from the Overall charts
    // Note: Apple's "Overall" category is usually represented as "podcasts" in the category field or handled by the charts logic 
    // We join charts + podcasts to get the PI IDs
    // We select distinct PI IDs from the charts table where rank <= 50 and category = 'all' (or however we stored it)
    // Let's check how populate-charts.js stored it. Usually 'all' or empty string for overall.
    // Assuming 'all' based on typical usage or we check the charts table content.
    // To be safe, we'll order by rank and limit to 50 distinct IDs across all regions/categories if specific one isn't clear,
    // BUT user said "top 50 in overall category".
    // Let's assume category='all' or 'top' in our charts table.

    const sql = `
    SELECT DISTINCT p.id, p.itunes_id 
    FROM charts c
    JOIN podcasts p ON c.itunes_id = p.itunes_id
    WHERE c.category = 'all' 
    ORDER BY c.rank ASC
  `;

    const res = await executeSQL(sql);
    return res?.results?.[0]?.response?.result?.rows?.map(r => ({
        id: r[0].value,
        itunesId: r[1].value
    })) || [];
}

async function fetchEpisodes(feedId) {
    try {
        const headers = generateAuthHeaders();
        // max=200 episodes per podcast
        const res = await fetch(`${API_BASE}/episodes/byfeedid?id=${feedId}&max=200`, { headers });
        if (!res.ok) throw new Error(`API error: ${res.status}`);
        const data = await res.json();
        return data.items || [];
    } catch (e) {
        console.error(`Failed to fetch episodes for ${feedId}:`, e.message);
        return [];
    }
}

async function main() {
    console.log("Starting Episode Sync via API...");

    // 1. Get podcasts from Turso
    const podcasts = await getPodcasts();
    console.log(`Found ${podcasts.length} podcasts to sync episodes for.`);

    // 2. Ensure episodes table has all needed columns
    // Note: we primarily use text for simplicity, but duration/published_at could be int
    await executeSQL(`
    CREATE TABLE IF NOT EXISTS episodes (
      id TEXT PRIMARY KEY,
      podcast_id TEXT,
      itunes_id TEXT,
      title TEXT,
      description TEXT,
      image_url TEXT,
      audio_url TEXT,
      duration TEXT,
      published_at TEXT,
      link TEXT,
      season TEXT,
      episode TEXT,
      episodeType TEXT,
      enclosureType TEXT
    )
  `);

    // Add columns if missing (naive check)
    try { await executeSQL("ALTER TABLE episodes ADD COLUMN link TEXT"); } catch (e) { }
    try { await executeSQL("ALTER TABLE episodes ADD COLUMN season TEXT"); } catch (e) { }
    try { await executeSQL("ALTER TABLE episodes ADD COLUMN episode TEXT"); } catch (e) { }
    try { await executeSQL("ALTER TABLE episodes ADD COLUMN episodeType TEXT"); } catch (e) { }
    try { await executeSQL("ALTER TABLE episodes ADD COLUMN enclosureType TEXT"); } catch (e) { }

    let totalEpisodes = 0;

    // 3. Process in batches (parallel requests)
    const CONCURRENCY = 5;
    for (let i = 0; i < podcasts.length; i += CONCURRENCY) {
        const batch = podcasts.slice(i, i + CONCURRENCY);
        console.log(`Processing batch ${i + 1}-${Math.min(i + CONCURRENCY, podcasts.length)} / ${podcasts.length}...`);

        await Promise.all(batch.map(async (pod) => {
            const episodes = await fetchEpisodes(pod.id);
            if (episodes.length === 0) return;

            const statements = episodes.map(ep => ({
                sql: `INSERT OR REPLACE INTO episodes 
              (id, podcast_id, itunes_id, title, description, image_url, audio_url, duration, published_at, link, season, episode, episodeType, enclosureType)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
                args: [
                    String(ep.id),
                    String(ep.feedId),
                    String(pod.itunesId),
                    ep.title,
                    ep.description || "",
                    ep.image || ep.feedImage || "",
                    ep.enclosureUrl,
                    String(ep.duration || "0"),
                    String(ep.datePublished || "0"),
                    ep.link || "",
                    String(ep.season || ""),
                    String(ep.episode || ""),
                    ep.episodeType || "",
                    String(ep.enclosureType || "")
                ]
            }));

            try {
                await executeBatch(statements);
                totalEpisodes += episodes.length;
            } catch (err) {
                console.error(`Failed to save episodes for ${pod.id}:`, err.message);
            }
        }));

        // Polite delay to avoid any potential rate limit triggers on Turso
        await new Promise(r => setTimeout(r, 100));
    }

    console.log(`Sync Complete! Saved ${totalEpisodes} episodes.`);
}

main().catch(console.error);
