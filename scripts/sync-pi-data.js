#!/usr/bin/env node
/**
 * Sync Podcast Index data for Apple chart podcasts
 * Fetches podcast details + 200 episodes for each unique chart podcast
 */

const TURSO_URL = process.env.TURSO_URL?.replace('libsql://', 'https://');
const TURSO_TOKEN = process.env.TURSO_AUTH_TOKEN;
const PI_API_KEY = process.env.PODCAST_INDEX_API_KEY;
const PI_API_SECRET = process.env.PODCAST_INDEX_API_SECRET;

if (!TURSO_URL || !TURSO_TOKEN) {
    console.error("Missing TURSO_URL or TURSO_AUTH_TOKEN");
    process.exit(1);
}

if (!PI_API_KEY || !PI_API_SECRET) {
    console.error("Missing PODCAST_INDEX_API_KEY or PODCAST_INDEX_API_SECRET");
    process.exit(1);
}

const crypto = require('crypto');

function generatePIAuthHeaders() {
    const authDate = Math.floor(Date.now() / 1000);
    const hash = crypto.createHash('sha1')
        .update(PI_API_KEY + PI_API_SECRET + authDate)
        .digest('hex');

    return {
        "X-Auth-Key": PI_API_KEY,
        "X-Auth-Date": authDate.toString(),
        "Authorization": hash,
        "User-Agent": "BoxCast/1.0"
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

async function querySQL(sql, args = []) {
    const result = await executeSQL(sql, args);
    const rows = result?.results?.[0]?.response?.result?.rows || [];
    return rows.map(row => row.map(col => col?.value));
}

async function fetchPodcastByItunesId(itunesId) {
    const url = `https://api.podcastindex.org/api/1.0/podcasts/byitunesid?id=${itunesId}`;
    const response = await fetch(url, { headers: generatePIAuthHeaders() });
    if (!response.ok) return null;
    const data = await response.json();
    return data.feed;
}

async function fetchEpisodes(feedId, max = 200) {
    const url = `https://api.podcastindex.org/api/1.0/episodes/byfeedid?id=${feedId}&max=${max}`;
    const response = await fetch(url, { headers: generatePIAuthHeaders() });
    if (!response.ok) return [];
    const data = await response.json();
    return data.items || [];
}

async function main() {
    console.log("Starting PI sync for chart podcasts...");

    // Get unique iTunes IDs from charts
    const rows = await querySQL("SELECT DISTINCT itunes_id FROM charts");
    const itunesIds = rows.map(r => r[0]).filter(Boolean);
    console.log(`Found ${itunesIds.length} unique podcasts in charts`);

    // Clear existing data
    console.log("Clearing existing podcasts and episodes...");
    await executeSQL("DELETE FROM episodes");
    await executeSQL("DELETE FROM podcasts");

    let podcastCount = 0;
    let episodeCount = 0;

    for (let i = 0; i < itunesIds.length; i++) {
        const itunesId = itunesIds[i];

        try {
            // Fetch podcast from PI
            const podcast = await fetchPodcastByItunesId(itunesId);
            if (!podcast) {
                console.log(`  [${i + 1}/${itunesIds.length}] iTunes ${itunesId}: Not found in PI`);
                continue;
            }

            // Insert podcast
            await executeSQL(
                "INSERT OR REPLACE INTO podcasts (id, itunes_id, title, author, description, image_url, feed_url, categories, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                [
                    podcast.id,
                    itunesId,
                    podcast.title?.substring(0, 500),
                    podcast.author?.substring(0, 200),
                    podcast.description?.substring(0, 2000),
                    podcast.image || podcast.artwork,
                    podcast.url,
                    podcast.categories ? Object.values(podcast.categories).join(', ') : null,
                    podcast.language
                ]
            );
            podcastCount++;

            // Fetch and insert episodes
            const episodes = await fetchEpisodes(podcast.id, 200);
            for (const ep of episodes) {
                await executeSQL(
                    "INSERT OR REPLACE INTO episodes (id, podcast_id, itunes_id, title, description, image_url, audio_url, duration, published_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    [
                        ep.id,
                        podcast.id,
                        itunesId,
                        ep.title?.substring(0, 500),
                        ep.description?.substring(0, 2000),
                        ep.image || ep.feedImage,
                        ep.enclosureUrl,
                        ep.duration || 0,
                        ep.datePublished || 0
                    ]
                );
                episodeCount++;
            }

            console.log(`  [${i + 1}/${itunesIds.length}] ${podcast.title?.substring(0, 30)}... - ${episodes.length} episodes`);

            // Rate limiting: 1 request per 100ms
            await new Promise(r => setTimeout(r, 100));

        } catch (e) {
            console.error(`  [${i + 1}/${itunesIds.length}] Error for iTunes ${itunesId}:`, e.message);
        }
    }

    console.log(`\nDone! Synced ${podcastCount} podcasts and ${episodeCount} episodes.`);
}

main().catch(err => {
    console.error("Sync failed:", err);
    process.exit(1);
});
