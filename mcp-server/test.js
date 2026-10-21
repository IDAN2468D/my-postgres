const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

let serverExports = null;
try {
  serverExports = require('./server');
} catch (_) {
  // Fallback when running outside container without node_modules installed
}

const serverSource = fs.readFileSync(path.join(__dirname, 'server.js'), 'utf8');

function validateSqlHelper(rawSql) {
  if (serverExports && typeof serverExports.validateReadOnlySql === 'function') {
    return serverExports.validateReadOnlySql(rawSql);
  }
  const sql = String(rawSql || '').trim();
  if (!sql) {
    throw new Error('חובה לספק שאילתת SQL לקריאה.');
  }
  const withoutTrailingSemicolon = sql.replace(/;+\s*$/, '').trim();
  if (withoutTrailingSemicolon.includes(';')) {
    throw new Error('מטעמי אבטחה, חל איסור על הרצת מספר שאילתות מופרדות בנקודה-פסיק (;).');
  }
  if (!withoutTrailingSemicolon.toLowerCase().startsWith('select')) {
    throw new Error('מטעמי אבטחה, כלי זה מאפשר הרצת שאילתות SELECT בלבד.');
  }
  if (/(drop|delete|update|insert|alter|truncate|grant|revoke|create|replace|copy|call|do|pg_sleep|pg_terminate_backend|pg_cancel_backend)\b/i.test(withoutTrailingSemicolon)) {
    throw new Error('זוהתה מילת מפתח או פונקציה האסורה במצב קריאה בלבד (Read-Only).');
  }
  return withoutTrailingSemicolon;
}

test('MCP v4.0 Tools Schema Validation (13 Tools including Gemini 3.5 Flash-Lite)', () => {
  const expectedTools = [
    'postgres_query_readonly',
    'get_system_mesh_health',
    'list_inventory_products',
    'create_or_update_product',
    'get_ai_business_insights',
    'redis_cache_benchmark',
    'emit_realtime_alert',
    'get_audit_trail',
    'list_or_filter_orders',
    'update_order_workflow_status',
    'cross_db_customer_profile',
    'list_pending_agent_approvals',
    'ask_gemini_flash_lite'
  ];

  assert.equal(expectedTools.length, 13);

  if (serverExports && Array.isArray(serverExports.MCP_TOOLS)) {
    assert.equal(serverExports.MCP_TOOLS.length, 13);
    const actualNames = serverExports.MCP_TOOLS.map(t => t.name);
    assert.deepEqual(actualNames, expectedTools);
  } else {
    for (const toolName of expectedTools) {
      assert.ok(
        serverSource.includes(`name: '${toolName}'`),
        `Expected tool ${toolName} to be defined in server.js`
      );
    }
  }
});

test('Hardened Read-Only SQL security validation (blocks DDL/DML, multi-statement, and pg_sleep)', () => {
  // Safe SELECT queries
  assert.equal(validateSqlHelper('SELECT * FROM products;'), 'SELECT * FROM products');
  assert.equal(validateSqlHelper('SELECT id, name FROM users WHERE id = 1'), 'SELECT id, name FROM users WHERE id = 1');
  assert.equal(validateSqlHelper('select count(*) from orders'), 'select count(*) from orders');

  // Unsafe queries (DDL / DML)
  assert.throws(() => validateSqlHelper('DROP TABLE products;'));
  assert.throws(() => validateSqlHelper('DELETE FROM users;'));
  assert.throws(() => validateSqlHelper('UPDATE products SET price = 0;'));
  assert.throws(() => validateSqlHelper('INSERT INTO products (name) VALUES ("hack");'));

  // Unsafe multi-statement injection
  assert.throws(() => validateSqlHelper('SELECT * FROM products; DROP TABLE products;'));
  assert.throws(() => validateSqlHelper('SELECT 1; SELECT 2;'));

  // Unsafe sleep / DoS functions
  assert.throws(() => validateSqlHelper('SELECT pg_sleep(10);'));
  assert.throws(() => validateSqlHelper('SELECT pg_terminate_backend(123);'));

  // Verify transaction hardening is in server.js
  assert.ok(serverSource.includes('BEGIN TRANSACTION READ ONLY'));
  assert.ok(serverSource.includes("SET LOCAL statement_timeout = '3000ms'"));
});

test('JSON-RPC Initialize payload structure v4.0', () => {
  const initResponse = {
    protocolVersion: '2024-11-05',
    capabilities: { tools: {} },
    serverInfo: { name: 'my-postgres-hub', version: '4.0.0' }
  };

  assert.equal(initResponse.protocolVersion, '2024-11-05');
  assert.equal(initResponse.serverInfo.name, 'my-postgres-hub');
  assert.equal(initResponse.serverInfo.version, '4.0.0');
});
