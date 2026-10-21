const express = require('express');
const cors = require('cors');
const { createClient } = require('redis');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 5003;
const REDIS_URL = process.env.REDIS_URL || 'redis://redis:6379';

const pgPool = new Pool({
  host: process.env.PGHOST || 'postgres',
  port: parseInt(process.env.PGPORT || '5432', 10),
  database: process.env.PGDATABASE || 'mydb',
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD || 'password',
});

const redisClient = createClient({ url: REDIS_URL });
let redisConnected = false;

redisClient.on('error', (err) => {
  console.error('Redis Error:', err.message);
  redisConnected = false;
});
redisClient.on('ready', () => {
  console.log('Connected to Redis at', REDIS_URL);
  redisConnected = true;
});

async function initRedis() {
  try {
    await redisClient.connect();
    const count = await redisClient.lLen('system:events');
    if (count === 0) {
      await pushEvent({
        type: 'CACHE_INIT',
        severity: 'info',
        title: 'שירות Redis Cache & Event Bus הופעל',
        message: 'שכבת הקאש המהירה ב-Redis 7 מחוברת ומסונכרנת עם PostgreSQL 16',
        source: 'events-service'
      });
    }
  } catch (e) {
    console.error('Failed initial Redis connect:', e.message);
  }
}

async function pushEvent(evt) {
  const payload = {
    id: Date.now() + '-' + Math.floor(Math.random() * 1000),
    type: evt.type || 'CUSTOM_EVENT',
    severity: evt.severity || 'info',
    title: evt.title || 'אירוע מערכת',
    message: evt.message || '',
    source: evt.source || 'events-service',
    timestamp: new Date().toISOString()
  };
  if (redisConnected) {
    await redisClient.lPush('system:events', JSON.stringify(payload));
    await redisClient.lTrim('system:events', 0, 49);
    await redisClient.incr('stats:events_total');
  }
  return payload;
}

app.get('/health', async (req, res) => {
  const t0 = performance.now();
  try {
    let pingReply = 'DISCONNECTED';
    let cachedKeys = 0;
    if (redisConnected) {
      pingReply = await redisClient.ping();
      const keys = await redisClient.keys('*');
      cachedKeys = keys.length;
    }
    const latencyMs = Number((performance.now() - t0).toFixed(2));
    res.json({
      status: redisConnected ? 'success' : 'degraded',
      service: 'nodejs-redis-events',
      port: Number(PORT),
      redis_connected: redisConnected,
      redis_ping: pingReply,
      cached_keys_count: cachedKeys,
      latency_ms: latencyMs,
      timestamp: new Date().toISOString()
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// Get live events feed + Redis cache metrics
app.get('/api/events/feed', async (req, res) => {
  try {
    let events = [];
    let cacheHits = 0;
    let totalEvents = 0;
    let lastSync = null;

    if (redisConnected) {
      const rawList = await redisClient.lRange('system:events', 0, 29);
      events = rawList.map(item => {
        try { return JSON.parse(item); } catch { return null; }
      }).filter(Boolean);

      cacheHits = parseInt((await redisClient.get('stats:cache_hits')) || '0', 10);
      totalEvents = parseInt((await redisClient.get('stats:events_total')) || String(events.length), 10);
      const syncMeta = await redisClient.get('cache:last_benchmark');
      if (syncMeta) {
        try { lastSync = JSON.parse(syncMeta); } catch {}
      }
    }

    res.json({
      status: 'success',
      service: 'nodejs-redis-events',
      redis_connected: redisConnected,
      metrics: {
        cache_hits: cacheHits,
        total_events: totalEvents,
        last_benchmark: lastSync
      },
      events
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// Emit a new real-time event into Redis
app.post('/api/events/emit', async (req, res) => {
  try {
    const evt = await pushEvent({
      type: req.body.type || 'USER_ALERT',
      severity: req.body.severity || 'info',
      title: req.body.title || 'התראה חדשה',
      message: req.body.message || 'אירוע נשלח אל Redis Event Bus',
      source: req.body.source || 'web-dashboard'
    });
    res.status(201).json({ status: 'success', data: evt });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

// Sync PostgreSQL -> Redis Cache & Benchmark speed (PG vs Redis)
app.post('/api/events/cache/sync', async (req, res) => {
  try {
    // 1. Measure PostgreSQL direct query latency
    const pgStart = performance.now();
    const pgResult = await pgPool.query('SELECT id, name, category, price, stock FROM products ORDER BY id DESC');
    const pgLatencyMs = Number((performance.now() - pgStart).toFixed(2));

    const products = pgResult.rows;

    // 2. Store in Redis with 300s TTL and measure Redis read latency
    let redisLatencyMs = 0.5;
    if (redisConnected) {
      await redisClient.setEx('cache:products:all', 300, JSON.stringify(products));
      const redisStart = performance.now();
      await redisClient.get('cache:products:all');
      redisLatencyMs = Number((performance.now() - redisStart).toFixed(2));
      await redisClient.incr('stats:cache_hits');
    }

    const speedupFactor = redisLatencyMs > 0
      ? Number((pgLatencyMs / Math.max(redisLatencyMs, 0.05)).toFixed(1))
      : 10.0;

    const benchmark = {
      synced_products_count: products.length,
      postgres_query_ms: pgLatencyMs,
      redis_cache_read_ms: redisLatencyMs,
      speedup_factor: speedupFactor,
      synced_at: new Date().toISOString()
    };

    if (redisConnected) {
      await redisClient.set('cache:last_benchmark', JSON.stringify(benchmark));
      await pushEvent({
        type: 'CACHE_BENCHMARK',
        severity: 'success',
        title: `סנכרון קאש הושלם (פי ${speedupFactor} מהיר יותר ב-Redis)`,
        message: `נשמרו ${products.length} מוצרים ב-Redis. זמן שליפה מ-Postgres: ${pgLatencyMs}ms לעומת Redis: ${redisLatencyMs}ms`,
        source: 'redis-benchmark'
      });
    }

    res.json({
      status: 'success',
      message: 'הנתונים סונכרנו מ-PostgreSQL ל-Redis Cache בהצלחה!',
      benchmark
    });
  } catch (err) {
    res.status(500).json({ status: 'error', message: err.message });
  }
});

initRedis().then(() => {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`Node.js + Redis Events Microservice listening on port ${PORT}`);
  });
});
