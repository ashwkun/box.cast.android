#!/usr/bin/env node
/**
 * Import podcasts and episodes from CSV files to Turso
 * Used by GitHub Actions workflow after extracting from PI dump
 */

const fs = require('fs');

const TURSO_URL = process.env.TURSO_URL?.replace('libsql://', 'https://');
const TURSO_TOKEN = process.env.TURSO_AUTH_TOKEN;

if (!TURSO_URL || !TURSO_TOKEN) {
    console.error("Missing TURSO_URL or TURSO_AUTH_TOKEN");
    process.exit(1);
}

function parseCSVLine(line) {
    const result = [];
    let current = '';
    let inQuotes = false;

    for (let i = 0; i < line.length; i++) {
        const char = line[i];
        if (char === '"') {
            if (inQuotes && line[i + 1] === '"') {
                current += '"';
                i++;
            } else {
                inQuotes = !inQuotes;
            }
        } else if (char === ',' && !inQuotes) {
            result.push(current);
            current = '';
        } else {
            current += char;
        }
    }
    result.push(current);
    return result;
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

    if (!response.ok) {
        throw new Error(`Turso error: ${response.status}`);
    }
    return response.json();
}

async function executeSQL(sql, args = []) {
    return executeBatch([{ sql, args }]);
}

async function importPodcasts() {
    const content = fs.readFileSync('podcasts_export.csv', 'utf-8');
    const lines = content.split('\n').filter(l => l.trim());

    console.log(`Importing ${lines.length} podcasts...`);

    // Clear existing
    await executeSQL("DELETE FROM podcasts", []);

    const BATCH_SIZE = 25;
    let imported = 0;

    for (let i = 0; i < lines.length; i += BATCH_SIZE) {
        const batch = lines.slice(i, i + BATCH_SIZE);
        const statements = [];

        for (const line of batch) {
            const [id, itunesId, title, author, description, imageUrl, feedUrl, categories, language] = parseCSVLine(line);
            if (!id) continue;

            statements.push({
                sql: "INSERT OR REPLACE INTO podcasts (id, itunes_id, title, author, description, image_url, feed_url, categories, language) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                args: [id, itunesId, title?.substring(0, 500), author?.substring(0, 200), description?.substring(0, 2000), imageUrl, feedUrl, categories, language]
            });
        }

        if (statements.length > 0) {
            await executeBatch(statements);
            imported += statements.length;
            if (imported % 500 === 0) console.log(`  Imported ${imported} podcasts...`);
        }
    }

    console.log(`Done: ${imported} podcasts imported`);
    return imported;
}

async function importEpisodes() {
    const content = fs.readFileSync('episodes_export.csv', 'utf-8');
    const lines = content.split('\n').filter(l => l.trim());

    console.log(`Importing ${lines.length} episodes...`);

    // Clear existing
    await executeSQL("DELETE FROM episodes", []);

    const BATCH_SIZE = 25;
    let imported = 0;

    // Track episodes per podcast to limit to 200
    const episodeCount = {};

    for (let i = 0; i < lines.length; i += BATCH_SIZE) {
        const batch = lines.slice(i, i + BATCH_SIZE);
        const statements = [];

        for (const line of batch) {
            const [id, podcastId, itunesId, title, description, imageUrl, audioUrl, duration, publishedAt] = parseCSVLine(line);
            if (!id || !podcastId) continue;

            // Limit to 200 episodes per podcast
            episodeCount[podcastId] = (episodeCount[podcastId] || 0) + 1;
            if (episodeCount[podcastId] > 200) continue;

            statements.push({
                sql: "INSERT OR REPLACE INTO episodes (id, podcast_id, itunes_id, title, description, image_url, audio_url, duration, published_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                args: [id, podcastId, itunesId, title?.substring(0, 500), description?.substring(0, 2000), imageUrl, audioUrl, duration || "0", publishedAt || "0"]
            });
        }

        if (statements.length > 0) {
            await executeBatch(statements);
            imported += statements.length;
            if (imported % 5000 === 0) console.log(`  Imported ${imported} episodes...`);
        }
    }

    console.log(`Done: ${imported} episodes imported`);
    return imported;
}

async function main() {
    console.log("Starting PI data import from CSV files...");

    const podcasts = await importPodcasts();
    const episodes = await importEpisodes();

    console.log(`\nImport complete: ${podcasts} podcasts, ${episodes} episodes`);
}

main().catch(err => {
    console.error("Import failed:", err);
    process.exit(1);
});
