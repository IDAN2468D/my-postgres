package com.idan.postgresapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idan.postgresapp.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/mesh")
public class MicroservicesGatewayController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditLogService auditLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Value("${ANALYTICS_URL:http://analytics-service:5002}")
    private String analyticsUrl;

    @Value("${EVENTS_URL:http://events-service:5003}")
    private String eventsUrl;

    @Value("${MCP_URL:http://mcp-server:5004}")
    private String mcpUrl;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMeshStatus() {
        List<Map<String, Object>> services = new ArrayList<>();

        // 1. PostgreSQL 16
        long t0 = System.nanoTime();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            double ms = Math.round((System.nanoTime() - t0) / 1_000_00.0) / 10.0;
            services.add(Map.of(
                    "id", "postgres",
                    "name", "PostgreSQL 16 DB",
                    "port", 5433,
                    "tech", "PostgreSQL Alpine",
                    "status", "UP",
                    "latency_ms", ms
            ));
        } catch (Exception e) {
            services.add(Map.of("id", "postgres", "name", "PostgreSQL 16 DB", "port", 5433, "status", "DOWN"));
        }

        // 2. Java Spring Boot 3 (Self)
        services.add(Map.of(
                "id", "webapp",
                "name", "Java Spring Boot + JWT",
                "port", 5001,
                "tech", "Java 17 / Spring 3.2",
                "status", "UP",
                "latency_ms", 0.4
        ));

        // 3. Python FastAPI AI Analytics
        services.add(probeHttpService("analytics", "Python AI Analytics", 5002, "FastAPI + Uvicorn", analyticsUrl + "/health"));

        // 4. Node.js + Redis 7 Events & Cache
        services.add(probeHttpService("events", "Redis Cache & Events", 5003, "Node.js 20 + Redis 7", eventsUrl + "/health"));

        // 5. MCP Server (Model Context Protocol)
        services.add(probeHttpService("mcp", "MCP Server Hub", 5004, "JSON-RPC 2.0 / MCP", mcpUrl + "/health"));

        boolean allUp = services.stream().allMatch(s -> "UP".equals(s.get("status")));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("mesh_health", allUp ? "HEALTHY" : "DEGRADED");
        response.put("services", services);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> probeHttpService(String id, String name, int port, String tech, String url) {
        long start = System.nanoTime();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            double ms = Math.round((System.nanoTime() - start) / 1_000_00.0) / 10.0;
            if (res.statusCode() == 200) {
                return Map.of(
                        "id", id,
                        "name", name,
                        "port", port,
                        "tech", tech,
                        "status", "UP",
                        "latency_ms", ms
                );
            }
        } catch (Exception ignored) {}
        return Map.of("id", id, "name", name, "port", port, "tech", tech, "status", "DOWN", "latency_ms", -1);
    }

    @GetMapping("/analytics")
    public ResponseEntity<Object> getAnalyticsInsights() {
        return forwardGet(analyticsUrl + "/api/analytics/insights");
    }

    @GetMapping("/currency")
    public ResponseEntity<Object> getConvertedCurrency(@RequestParam(defaultValue = "USD") String currency) {
        return forwardGet(analyticsUrl + "/api/analytics/currency?currency=" + currency);
    }

    @GetMapping("/events")
    public ResponseEntity<Object> getEventsFeed() {
        return forwardGet(eventsUrl + "/api/events/feed");
    }

    @PostMapping("/cache-sync")
    public ResponseEntity<Object> triggerCacheSync() {
        auditLogService.log("REDIS_CACHE_SYNC", "SYSTEM", null, "סנכרון ובדיקת ביצועי קאש PostgreSQL מול Redis 7");
        return forwardPost(eventsUrl + "/api/events/cache/sync", "{}");
    }

    @PostMapping("/events/emit")
    public ResponseEntity<Object> emitEvent(@RequestBody Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            return forwardPost(eventsUrl + "/api/events/emit", json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/mcp/tools")
    public ResponseEntity<Object> getMcpTools() {
        return forwardGet(mcpUrl + "/mcp/tools");
    }

    @PostMapping("/mcp/execute")
    public ResponseEntity<Object> executeMcpTool(@RequestBody Map<String, Object> body) {
        try {
            String toolName = String.valueOf(body.getOrDefault("name", "unknown"));
            auditLogService.log("MCP_TOOL_CALL", "MCP", null, "הופעל כלי MCP מהדשבורד: " + toolName);
            String json = objectMapper.writeValueAsString(body);
            return forwardPost(mcpUrl + "/mcp/execute", json);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private ResponseEntity<Object> forwardGet(String targetUrl) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            Object parsed = objectMapper.readValue(res.body(), Object.class);
            return ResponseEntity.status(res.statusCode()).body(parsed);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private ResponseEntity<Object> forwardPost(String targetUrl, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            Object parsed = objectMapper.readValue(res.body(), Object.class);
            return ResponseEntity.status(res.statusCode()).body(parsed);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
