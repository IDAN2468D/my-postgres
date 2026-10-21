#!/usr/bin/env node
/**
 * Zero-dependency MCP Stdio Bridge for Antigravity / Claude / Cursor
 * Forwards JSON-RPC 2.0 MCP requests over stdio to the running my-postgres-hub MCP container on localhost:5004
 */
const readline = require('readline');

const MCP_HTTP_RPC = process.env.MCP_HTTP_RPC || 'http://localhost:5004/mcp/rpc';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  terminal: false
});

rl.on('line', async (line) => {
  if (!line.trim()) return;
  let msg;
  try {
    msg = JSON.parse(line);
  } catch (e) {
    return;
  }

  // Notifications don't expect a response
  if (msg.id === undefined || msg.id === null) {
    return;
  }

  try {
    const res = await fetch(MCP_HTTP_RPC, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(msg)
    });
    const data = await res.json();
    process.stdout.write(JSON.stringify(data) + '\n');
  } catch (err) {
    process.stdout.write(JSON.stringify({
      jsonrpc: '2.0',
      id: msg.id,
      error: {
        code: -32000,
        message: 'Failed to connect to my-postgres-hub MCP container on port 5004: ' + err.message
      }
    }) + '\n');
  }
});
