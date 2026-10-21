const express = require('express');
const cors = require('cors');
const { Pool } = require('pg');

const app = express();
app.use(cors());
app.use(express.json());

const PORT = process.env.PORT || 5004;
const ANALYTICS_URL = process.env.ANALYTICS_URL || 'http://analytics-service:5002';
const EVENTS_URL = process.env.EVENTS_URL || 'http://events-service:5003';
const WEBAPP_URL = process.env.WEBAPP_URL || 'http://webapp:5001';

const pgPool = new Pool({
  host: process.env.PGHOST || 'postgres',
  port: parseInt(process.env.PGPORT || '5432', 10),
  database: process.env.PGDATABASE || 'mydb',
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD || 'password',
});

// 8 Official MCP Tools Definitions
const MCP_TOOLS = [
  {
    name: 'postgres_query_readonly',
    title: 'הרצת שאילתת SQL לקריאה בלבד (SELECT)',
    description: 'Execute a safe read-only SQL SELECT query against the PostgreSQL 16 database (mydb) and return rows as JSON.',
    inputSchema: {
      type: 'object',
      properties: {
        sql: {
          type: 'string',
          description: 'SQL SELECT query (e.g. SELECT * FROM products ORDER BY price DESC LIMIT 5)'
        }
      },
      required: ['sql']
    },
    sampleArgs: { sql: 'SELECT id, name, category, price, stock FROM products ORDER BY price DESC;' }
  },
  {
    name: 'get_system_mesh_health',
    title: 'בדיקת בריאות וזמני תגובה של כל המיקרו-סרביסים',
    description: 'Inspect live health status and latency (ms) across PostgreSQL, Spring Boot JWT, Python FastAPI Analytics, and Node.js Redis Events.',
    inputSchema: {
      type: 'object',
      properties: {}
    },
    sampleArgs: {}
  },
  {
    name: 'list_inventory_products',
    title: 'שליפת קטלוג מוצרים וסינון מלאי',
    description: 'Query products from PostgreSQL with optional category filter or low-stock alert filter (stock <= 5).',
    inputSchema: {
      type: 'object',
      properties: {
        category: { type: 'string', description: 'Optional category name in Hebrew (e.g. אלקטרוניקה)' },
        lowStockOnly: { type: 'boolean', description: 'If true, only return products with stock <= 5' }
      }
    },
    sampleArgs: { lowStockOnly: false }
  },
  {
    name: 'create_or_update_product',
    title: 'הוספה או עדכון מוצר במסד הנתונים',
    description: 'Insert a new product or update an existing product price/stock in PostgreSQL and record an Audit Log entry.',
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: 'Product name' },
        category: { type: 'string', description: 'Product category' },
        price: { type: 'number', description: 'Price in ILS (₪)' },
        stock: { type: 'integer', description: 'Stock quantity' }
      },
      required: ['name', 'price']
    },
    sampleArgs: { name: 'מיקרופון שולחני USB Pro', category: 'שמע', price: 420.0, stock: 18 }
  },
  {
    name: 'get_ai_business_insights',
    title: 'תובנות AI והמרת מטבעות (Python FastAPI)',
    description: 'Fetch intelligent inventory recommendations, top customers, and multi-currency revenue (ILS/USD/EUR/GBP) from the Python FastAPI microservice.',
    inputSchema: {
      type: 'object',
      properties: {}
    },
    sampleArgs: {}
  },
  {
    name: 'redis_cache_benchmark',
    title: 'סנכרון קאש ומבחן ביצועים (PostgreSQL מול Redis)',
    description: 'Sync product catalog into Redis 7 and benchmark PostgreSQL query time vs Redis in-memory read latency.',
    inputSchema: {
      type: 'object',
      properties: {}
    },
    sampleArgs: {}
  },
  {
    name: 'emit_realtime_alert',
    title: 'שידור התראה בזמן אמת ל-Redis Event Bus',
    description: 'Publish a real-time alert notification to the Node.js + Redis Event Bus.',
    inputSchema: {
      type: 'object',
      properties: {
        title: { type: 'string', description: 'Alert title in Hebrew' },
        message: { type: 'string', description: 'Alert details message' },
        severity: { type: 'string', enum: ['info', 'success', 'warning', 'error'] }
      },
      required: ['title', 'message']
    },
    sampleArgs: { title: 'בדיקת MCP Live', message: 'התראה נשלחה בהצלחה מתוך שרת ה-MCP!', severity: 'success' }
  },
  {
    name: 'get_audit_trail',
    title: 'שליפת יומן פעילות ואבטחה (Audit Logs)',
    description: 'Fetch the most recent security and CRUD audit logs from PostgreSQL.',
    inputSchema: {
      type: 'object',
      properties: {
        limit: { type: 'integer', description: 'Number of audit records to return (default 10)' }
      }
    },
    sampleArgs: { limit: 10 }
  }
];

async function executeMcpTool(toolName, args = {}) {
  const t0 = performance.now();

  switch (toolName) {
    case 'postgres_query_readonly': {
      const sql = String(args.sql || '').trim();
      if (!sql.toLowerCase().startsWith('select')) {
        throw new Error('מטעמי אבטחה, כלי זה מאפשר הרצת שאילתות SELECT בלבד.');
      }
      if (/(drop|delete|update|insert|alter|truncate|grant|revoke)\b/i.test(sql)) {
        throw new Error('זוהתה מילת מפתח האסורה במצב קריאה בלבד (Read-Only).');
      }
      const res = await pgPool.query(sql);
      return {
        tool: toolName,
        rowCount: res.rowCount,
        rows: res.rows,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'get_system_mesh_health': {
      const checks = await Promise.all([
        (async () => {
          const s = performance.now();
          await pgPool.query('SELECT 1');
          return { service: 'postgres-16', port: 5433, status: 'UP', latencyMs: Number((performance.now() - s).toFixed(2)) };
        })().catch(e => ({ service: 'postgres-16', port: 5433, status: 'DOWN', error: e.message })),

        fetch(`${WEBAPP_URL}/api/health`).then(r => r.json()).then(d => ({
          service: 'spring-boot-jwt', port: 5001, status: d.status === 'success' ? 'UP' : 'DOWN'
        })).catch(e => ({ service: 'spring-boot-jwt', port: 5001, status: 'DOWN', error: e.message })),

        fetch(`${ANALYTICS_URL}/health`).then(r => r.json()).then(d => ({
          service: 'python-fastapi-analytics', port: 5002, status: d.status === 'success' ? 'UP' : 'DOWN', latencyMs: d.latency_ms
        })).catch(e => ({ service: 'python-fastapi-analytics', port: 5002, status: 'DOWN', error: e.message })),

        fetch(`${EVENTS_URL}/health`).then(r => r.json()).then(d => ({
          service: 'nodejs-redis-events', port: 5003, status: d.status === 'success' ? 'UP' : 'DEGRADED', latencyMs: d.latency_ms, cachedKeys: d.cached_keys_count
        })).catch(e => ({ service: 'nodejs-redis-events', port: 5003, status: 'DOWN', error: e.message }))
      ]);
      return {
        tool: toolName,
        meshStatus: checks.every(c => c.status === 'UP') ? 'HEALTHY' : 'PARTIAL',
        services: checks,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'list_inventory_products': {
      let query = 'SELECT id, name, category, price, stock, created_at FROM products WHERE 1=1';
      const params = [];
      if (args.category && args.category !== 'ALL') {
        params.push(args.category);
        query += ` AND LOWER(category) = LOWER($${params.length})`;
      }
      if (args.lowStockOnly) {
        query += ' AND stock <= 5';
      }
      query += ' ORDER BY id DESC';
      const res = await pgPool.query(query, params);
      return {
        tool: toolName,
        count: res.rowCount,
        products: res.rows,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'create_or_update_product': {
      const name = String(args.name || '').trim();
      const category = String(args.category || 'כללי').trim();
      const price = Number(args.price || 0);
      const stock = args.stock !== undefined ? parseInt(args.stock, 10) : 10;
      if (!name || price <= 0) {
        throw new Error('חובה לספק שם מוצר ומחיר חיובי');
      }
      const existing = await pgPool.query('SELECT id FROM products WHERE LOWER(name) = LOWER($1) LIMIT 1', [name]);
      let saved;
      let actionType;
      if (existing.rowCount > 0) {
        const id = existing.rows[0].id;
        const upd = await pgPool.query(
          'UPDATE products SET category = $1, price = $2, stock = $3 WHERE id = $4 RETURNING *',
          [category, price, stock, id]
        );
        saved = upd.rows[0];
        actionType = 'MCP_UPDATE_PRODUCT';
      } else {
        const ins = await pgPool.query(
          'INSERT INTO products (name, category, price, stock) VALUES ($1, $2, $3, $4) RETURNING *',
          [name, category, price, stock]
        );
        saved = ins.rows[0];
        actionType = 'MCP_CREATE_PRODUCT';
      }
      await pgPool.query(
        'INSERT INTO audit_logs (action, entity_type, entity_id, details, performed_by, created_at) VALUES ($1, $2, $3, $4, $5, NOW())',
        [actionType, 'PRODUCT', saved.id, `בוצע דרך MCP: ${saved.name} (₪${saved.price}, מלאי: ${saved.stock})`, 'MCP Agent 🤖']
      );
      return {
        tool: toolName,
        action: actionType,
        product: saved,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'get_ai_business_insights': {
      const r = await fetch(`${ANALYTICS_URL}/api/analytics/insights`);
      const data = await r.json();
      return {
        tool: toolName,
        analytics: data,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'redis_cache_benchmark': {
      const r = await fetch(`${EVENTS_URL}/api/events/cache/sync`, { method: 'POST' });
      const data = await r.json();
      return {
        tool: toolName,
        benchmarkResult: data,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'emit_realtime_alert': {
      const r = await fetch(`${EVENTS_URL}/api/events/emit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          type: 'MCP_ALERT',
          title: args.title || 'התראת MCP',
          message: args.message || '',
          severity: args.severity || 'info',
          source: 'mcp-server'
        })
      });
      const data = await r.json();
      return {
        tool: toolName,
        event: data.data,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    case 'get_audit_trail': {
      const limit = Math.min(50, Math.max(1, parseInt(args.limit || 10, 10)));
      const res = await pgPool.query(
        'SELECT id, action, entity_type, entity_id, details, performed_by, created_at FROM audit_logs ORDER BY created_at DESC LIMIT $1',
        [limit]
      );
      return {
        tool: toolName,
        count: res.rowCount,
        logs: res.rows,
        executionMs: Number((performance.now() - t0).toFixed(2))
      };
    }

    default:
      throw new Error(`כלי MCP לא מוכר: ${toolName}`);
  }
}

app.get('/health', async (req, res) => {
  res.json({
    status: 'success',
    service: 'mcp-postgres-server',
    protocolVersion: '2024-11-05',
    port: Number(PORT),
    tools_count: MCP_TOOLS.length,
    tools: MCP_TOOLS.map(t => t.name)
  });
});

app.get('/mcp/tools', (req, res) => {
  res.json({
    status: 'success',
    protocol: 'Model Context Protocol (MCP)',
    serverName: 'my-postgres-hub',
    version: '3.0.0',
    tools: MCP_TOOLS
  });
});

app.post('/mcp/execute', async (req, res) => {
  const { name, arguments: args } = req.body || {};
  if (!name) {
    return res.status(400).json({ status: 'error', message: 'Missing tool name' });
  }
  try {
    const result = await executeMcpTool(name, args || {});
    res.json({
      status: 'success',
      tool: name,
      result,
      mcpContent: [
        {
          type: 'text',
          text: JSON.stringify(result, null, 2)
        }
      ]
    });
  } catch (err) {
    res.status(400).json({
      status: 'error',
      tool: name,
      message: err.message
    });
  }
});

// Full JSON-RPC 2.0 MCP endpoint
app.post('/mcp/rpc', async (req, res) => {
  const { jsonrpc, id, method, params } = req.body || {};
  try {
    if (method === 'initialize') {
      return res.json({
        jsonrpc: '2.0',
        id,
        result: {
          protocolVersion: '2024-11-05',
          capabilities: { tools: {} },
          serverInfo: { name: 'my-postgres-hub', version: '3.0.0' }
        }
      });
    }
    if (method === 'tools/list') {
      return res.json({
        jsonrpc: '2.0',
        id,
        result: {
          tools: MCP_TOOLS.map(({ name, description, inputSchema }) => ({ name, description, inputSchema }))
        }
      });
    }
    if (method === 'tools/call') {
      const output = await executeMcpTool(params.name, params.arguments || {});
      return res.json({
        jsonrpc: '2.0',
        id,
        result: {
          content: [{ type: 'text', text: JSON.stringify(output, null, 2) }]
        }
      });
    }
    res.json({ jsonrpc: '2.0', id, result: {} });
  } catch (err) {
    res.json({
      jsonrpc: '2.0',
      id,
      error: { code: -32000, message: err.message }
    });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`MCP Server (my-postgres-hub) listening on port ${PORT}`);
});
