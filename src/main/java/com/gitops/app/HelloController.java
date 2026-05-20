package com.gitops.app;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * HelloController
 *
 * Exposes two REST endpoints:
 *   GET /health  → returns {"status": "ok"} with HTTP 200
 *   GET /hello   → returns {"message": "..."} with HTTP 200
 *
 * NOTE on /health vs /actuator/health:
 *   - This /health endpoint is a simple custom endpoint we own fully.
 *   - /actuator/health is Spring Boot Actuator's built-in endpoint that
 *     reports deeper health (DB connectivity, disk space, etc.).
 *   - In a real Kubernetes setup, /actuator/health is used for liveness
 *     and readiness probes. Our custom /health is kept minimal on purpose
 *     so it's always fast and has zero dependencies — useful as a
 *     lightweight load balancer ping or smoke test.
 */
@RestController
public class HelloController {

    /*
     * @RestController = @Controller + @ResponseBody
     *
     * @Controller marks this as a Spring MVC controller (web layer bean).
     * @ResponseBody tells Spring to serialize the return value directly
     * into the HTTP response body as JSON (via Jackson, which is bundled
     * with spring-boot-starter-web), instead of treating the return
     * value as a view name to resolve.
     *
     * So: returning a Map<String, String> → Jackson serializes it to JSON
     * automatically. No need to manually write ObjectMapper code.
     */

    /**
     * GET /health
     *
     * Lightweight custom health check endpoint.
     * Returns HTTP 200 with a JSON body: {"status": "ok"}
     *
     * Use cases:
     *   - Load balancer health checks (e.g., AWS ALB, NGINX upstream check)
     *   - Simple smoke test after a deployment ("did the app start?")
     *   - Docker HEALTHCHECK instruction target
     *
     * ResponseEntity<Map<String, String>> gives us explicit control over:
     *   - The HTTP status code (HttpStatus.OK = 200)
     *   - The response headers (not used here, but available)
     *   - The response body (our Map, serialized to JSON by Jackson)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        /*
         * ResponseEntity.ok() is a static factory method that sets
         * HTTP 200 and wraps the body. Equivalent to:
         *   new ResponseEntity<>(response, HttpStatus.OK)
         * but more readable and idiomatic in Spring Boot.
         */
        return ResponseEntity.ok(response);
    }

    /**
     * GET /hello
     *
     * Simple greeting endpoint — demonstrates the app is serving
     * dynamic responses. Intentionally minimal; the real value of
     * this project is in the pipeline and infrastructure around it.
     *
     * Returns HTTP 200 with: {"message": "Hello from GitOps pipeline!"}
     */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello from GitOps pipeline!");
        /*
         * Returning a Map instead of a plain String keeps the response
         * as structured JSON rather than a raw string value.
         * {"message": "Hello"} is a proper JSON object.
         * "Hello" alone is a valid JSON primitive but harder to extend
         * later without breaking API consumers.
         */
        return ResponseEntity.ok(response);
    }

}