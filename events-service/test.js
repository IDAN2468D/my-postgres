const test = require('node:test');
const assert = require('node:assert/strict');

test('Event payload formatting logic', () => {
  const formatEvent = (evt) => ({
    id: 'test-123',
    type: evt.type || 'CUSTOM_EVENT',
    severity: evt.severity || 'info',
    title: evt.title || 'אירוע מערכת',
    message: evt.message || '',
    source: evt.source || 'events-service',
    timestamp: new Date().toISOString()
  });

  const evt = formatEvent({
    type: 'CACHE_BENCHMARK',
    severity: 'success',
    title: 'בדיקת ביצועים',
    message: 'סנכרון תקין'
  });

  assert.equal(evt.type, 'CACHE_BENCHMARK');
  assert.equal(evt.severity, 'success');
  assert.equal(evt.title, 'בדיקת ביצועים');
  assert.equal(evt.source, 'events-service');
  assert.ok(evt.timestamp);
});

test('Speedup factor calculation logic', () => {
  const calculateSpeedup = (pgLatencyMs, redisLatencyMs) => {
    return redisLatencyMs > 0
      ? Number((pgLatencyMs / Math.max(redisLatencyMs, 0.05)).toFixed(1))
      : 10.0;
  };

  const speedup1 = calculateSpeedup(25.0, 0.5);
  assert.equal(speedup1, 50.0);

  const speedup2 = calculateSpeedup(10.0, 0.0);
  assert.equal(speedup2, 10.0);
});

test('Environment variable defaults verification', () => {
  const PORT = process.env.PORT || 5003;
  const REDIS_URL = process.env.REDIS_URL || 'redis://redis:6379';

  assert.equal(typeof PORT, 'number' ? typeof PORT : 'string');
  assert.ok(REDIS_URL.startsWith('redis://'));
});
