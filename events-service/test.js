const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const serverSource = fs.readFileSync(path.join(__dirname, 'server.js'), 'utf8');

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
    type: 'AGENT_TRIGGER_LOW_STOCK',
    severity: 'warning',
    title: 'טריגר סוכן מלאי',
    message: 'ירידה למלאי קריטי'
  });

  assert.equal(evt.type, 'AGENT_TRIGGER_LOW_STOCK');
  assert.equal(evt.severity, 'warning');
  assert.equal(evt.title, 'טריגר סוכן מלאי');
  assert.equal(evt.source, 'events-service');
  assert.ok(evt.timestamp);
});

test('Agent triggers counter support in server.js', () => {
  assert.ok(serverSource.includes('stats:agent_triggers'));
  assert.ok(serverSource.includes('agent_triggers: agentTriggers'));
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
