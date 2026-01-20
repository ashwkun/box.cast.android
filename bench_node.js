const https = require('https');

const url = "https://boxcast-api.boxboxcric.workers.dev/trending?limit=50&country=in";
const options = {
    headers: {
        'X-App-Key': 'boxcast-app-c47580b27dd94ec1'
    }
};

console.log(`Starting Benchmark: ${url}`);
const start = Date.now();

https.get(url, options, (res) => {
    let chunkCount = 0;

    res.on('data', (chunk) => {
        const now = Date.now();
        const delta = now - start;
        const size = chunk.length;

        let label = `Chunk ${chunkCount}`;
        if (chunkCount === 0) label = "TTB (First Byte)";

        console.log(`[+${delta}ms] ${label} - Size: ${size} bytes`);
        chunkCount++;
    });

    res.on('end', () => {
        const end = Date.now();
        console.log(`[+${end - start}ms] Complete. Total Chunks: ${chunkCount}`);
    });
}).on('error', (e) => {
    console.error(e);
});
