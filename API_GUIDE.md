# BoxCast API Guide

## Base URL
```
https://boxcast-api.boxboxcric.workers.dev
```

## Authentication
All requests require the `X-App-Key` header:
```
X-App-Key: boxcast-app-c47580b27dd94ec1
```

---

## Endpoints

### 1. Trending Podcasts
```bash
GET /trending

curl -H "X-App-Key: boxcast-app-c47580b27dd94ec1" \
  "https://boxcast-api.boxboxcric.workers.dev/trending"
```

**Response Fields:**
- `feeds[].id` – Podcast Index feed ID
- `feeds[].title` – Show name
- `feeds[].author` – Publisher
- `feeds[].description` – Full description
- `feeds[].artwork` – High-res cover (1400x1400)
- `feeds[].categories` – Genre map
- `feeds[].itunesId` – Apple Podcasts ID (for cross-reference)

---

### 2. Search Podcasts
```bash
GET /search?q={query}

curl -H "X-App-Key: boxcast-app-c47580b27dd94ec1" \
  "https://boxcast-api.boxboxcric.workers.dev/search?q=technology"
```

---

### 3. Get Episodes
```bash
GET /episodes?id={feedId}

curl -H "X-App-Key: boxcast-app-c47580b27dd94ec1" \
  "https://boxcast-api.boxboxcric.workers.dev/episodes?id=3370240"
```

**Response Fields:**
- `items[].id` – Episode ID
- `items[].title` – Episode title
- `items[].description` – Full episode notes
- `items[].enclosureUrl` – Audio stream URL
- `items[].duration` – Length in seconds
- `items[].datePublished` – Unix timestamp
- `items[].chaptersUrl` – Chapters JSON (if available)

---

### 4. Single Podcast Details
```bash
GET /podcast?id={feedId}

curl -H "X-App-Key: boxcast-app-c47580b27dd94ec1" \
  "https://boxcast-api.boxboxcric.workers.dev/podcast?id=3370240"
```

---

## Podcast Index API Reference
Our proxy wraps these Podcast Index endpoints:

| Proxy Route | Podcast Index Endpoint |
|-------------|------------------------|
| `/trending` | `/podcasts/trending?max=20` |
| `/search` | `/search/byterm?q=...&max=20` |
| `/episodes` | `/episodes/byfeedid?id=...&max=50&fulltext` |
| `/podcast` | `/podcasts/byfeedid?id=...` |

Full Podcast Index docs: https://podcastindex-org.github.io/docs-api/
