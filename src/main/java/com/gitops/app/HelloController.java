package com.gitops.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class HelloController {

    @Autowired
    private BuildProperties buildProperties;

    // -------------------------------------------------------------------------
    // shown on /hello — rotates randomly on each request.
    // -------------------------------------------------------------------------
    private static final List<String> FUN_FACTS = List.of(
        "Works on my machine. Production disagrees.",
        "This deployment had zero downtime. Emotionally, however, the team is unstable.",
        "Jenkins built this image at 2 AM because someone merged directly into main.",
        "Kubernetes saw one pod struggling and said: 'Fine, I'll make six more.'",
        "This app survived a rolling deployment, three restarts, and one engineer saying 'just one quick change'.",
        "The only thing more temporary than this container is the intern who wrote the first Dockerfile.",
        "ArgoCD noticed drift before the developer did.",
        "Somewhere, a DevOps engineer is explaining why restarting the pod is not a permanent fix.",
        "This application has been deployed successfully 47 times. Nobody knows why build #32 worked.",
        "There is a 93% chance this issue will be solved by checking the logs properly.",
        "kubectl delete pod is not a troubleshooting strategy. It is a lifestyle.",
        "This service is highly available. The engineer maintaining it is not.",
        "The pipeline was green locally.",
        "If this endpoint is slow, blame DNS first. It's always DNS.",
        "Prometheus is collecting metrics. Grafana is making them look important.",
        "This container is immutable. The debugging process is not.",
        "Someone definitely hardcoded a value somewhere. We just haven't found it yet.",
        "This deployment was approved after extensive testing in production.",
        "Helm charts: because YAML alone wasn't painful enough.",
        "The cluster is healthy. The team chat is not.",
        "This app is running as non-root because security said so, not because developers wanted to.",
        "Every successful deployment increases confidence right before the next outage.",
        "GitOps means the YAML breaks itself automatically now.",
        "CI/CD: turning small mistakes into production incidents faster than ever.",
        "The pod is restarting because it believes in continuous improvement.",
        "There are only two hard things in DevOps: naming things, cache invalidation, and Kubernetes networking.",
        "This endpoint was load tested exactly once. During production traffic.",
        "Terraform will recreate everything if you anger it enough.",
        "One does not simply kubectl into production.",
        "The application is stateless. The developers are not."
    );

    private static final Random RANDOM = new Random();

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {

        String builtAt = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                .withZone(ZoneId.of("UTC"))
                .format(buildProperties.getTime());

        String serverTime = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String funFact = FUN_FACTS.get(RANDOM.nextInt(FUN_FACTS.size()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message",     "Hello from the GitOps Pipeline!");
        response.put("application", buildProperties.getName());
        response.put("version",     buildProperties.getVersion());
        response.put("built_at",    builtAt);
        response.put("server_time", serverTime);
        response.put("status",      "running");
        response.put("cluster_truth",    funFact);

        return ResponseEntity.ok(response);
    }
}