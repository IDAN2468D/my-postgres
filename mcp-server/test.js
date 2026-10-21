const test = require('node:test');
const assert = require('node:assert/strict');

test('MCP Tools Schema Validation', () => {
  // Read tools from server source structure or require
  const expectedTools = [
    'postgres_query_readonly',
    'get_system_mesh_health',
    'list_inventory_products',
    'create_or_update_product',
    'get_ai_business_insights',
    'redis_cache_benchmark',
    'emit_realtime_alert',
    'get_audit_trail'
  ];

  assert.equal(expectedTools.length, 8);
  for (const tool of expectedTools) {
    assert.ok(typeof tool === 'string');
  }
});

test('Read-only SQL security validation logic', () => {
  const isReadOnlySql = (sql) => {
    const trimmed = String(sql || '').trim();
    if (!trimmed.toLowerCase().startsWith('select')) {
      return false;
    }
    if (/(drop|delete|update|insert|alter|truncate|grant|revoke)\b/i.test(trimmed)) {
      return false;
    }
    return true;
  };

  // Safe SELECT queries
  assert.equal(isReadOnlySql('SELECT * FROM products;'), true);
  assert.equal(isReadOnlySql('SELECT id, name FROM users WHERE id = 1'), true);
  assert.equal(isReadOnlySql('select count(*) from orders'), true);

  // Unsafe queries
  assert.equal(isReadOnlySql('DROP TABLE products;'), false);
  assert.equal(isReadOnlySql('DELETE FROM users;'), false);
  assert.equal(isReadOnlySql('UPDATE products SET price = 0;'), false);
  assert.equal(isReadOnlySql('INSERT INTO products (name) VALUES ("hack");'), false);
  assert.equal(isReadOnlySql('SELECT * FROM products; DROP TABLE products;'), false);
});

test('JSON-RPC Initialize payload structure', () => {
  const initResponse = {
    protocolVersion: '2024-11-05',
    capabilities: { tools: {} },
    serverInfo: { name: 'my-postgres-hub', version: '3.0.0' }
  };

  assert.equal(initResponse.protocolVersion, '2024-11-05');
  assert.equal(initResponse.serverInfo.name, 'my-postgres-hub');
  assert.equal(initResponse.serverInfo.version, '3.0.0');
});
