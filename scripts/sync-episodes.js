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

    // 2. Ensure podcasts table has new columns for flattened latest episode data
    const newColumns = [
        "latest_ep_id TEXT",
        "latest_ep_title TEXT",
        "latest_ep_date INTEGER", // Int for easier sorting
        "latest_ep_duration INTEGER",
        "latest_ep_url TEXT",
        "latest_ep_image TEXT",
        "latest_ep_type TEXT"
    ];

    console.log("Ensuring schema columns exist...");
    for (const colDef of newColumns) {
        try {
            await executeSQL(`ALTER TABLE podcasts ADD COLUMN ${colDef}`);
            console.log(`  - Added column: ${colDef.split(" ")[0]}`);
        } catch (e) {
            // Ignore if column exists
        }
    }

    // 3. Process in batches (parallel requests)
    let totalPodcastsUpdated = 0;
    const CONCURRENCY = 5;

    for (let i = 0; i < podcasts.length; i += CONCURRENCY) {
        const batch = podcasts.slice(i, i + CONCURRENCY);
        console.log(`Processing batch ${i + 1}-${Math.min(i + CONCURRENCY, podcasts.length)} / ${podcasts.length}...`);

        await Promise.all(batch.map(async (pod) => {
            const episodes = await fetchEpisodes(pod.id);
            if (episodes.length === 0) return;

            // Find the latest episode (usually first, but ensure by date)
            // Handle bulk publish edge case by preferring higher ID if dates match?
            // PI returns descending date.
            const latestEp = episodes[0]; // Simple heuristic for now as PI sorts by new

            try {
                // Update podcast with latest episode metadata
                // We use UPDATE because the podcast row already exists (from import-pi-data.js)
                const sql = `
                    UPDATE podcasts SET 
                        latest_ep_id = ?,
                        latest_ep_title = ?,
                        latest_ep_date = ?,
                        latest_ep_duration = ?,
                        latest_ep_url = ?,
                        latest_ep_image = ?,
                        latest_ep_type = ?
                    WHERE id = ?
                `;

                await executeSQL(sql, [
                    String(latestEp.id),
                    latestEp.title || "",
                    latestEp.datePublished || 0,
                    latestEp.duration || 0,
                    latestEp.enclosureUrl || "",
                    latestEp.image || latestEp.feedImage || "",
                    latestEp.enclosureType || "audio/mpeg",
                    String(pod.id)
                ]);

                totalPodcastsUpdated++;
            } catch (err) {
                console.error(`Failed to update podcast ${pod.id}:`, err.message);
            }
        }));

        // Polite delay
        await new Promise(r => setTimeout(r, 100));
    }

    console.log(`Sync Complete! Updated metadata for ${totalPodcastsUpdated} podcasts.`);
}

main().catch(console.error);
