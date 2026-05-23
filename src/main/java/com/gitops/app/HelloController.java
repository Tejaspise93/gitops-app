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

    private static final List<String> HOW_I_LOOK_RIGHT_NOW = List.of(
        "(•_•)\n<) )╯ kubectl delete pod\n / \\",
        "(╥_╥)\nWatching production logs...",
        "(☉_☉)\nReading Kubernetes events",
        "(╯°□°)╯︵ ┻━┻",
        "┬─┬ノ( º _ ºノ)",
        "(•_•)\n<)   )╯ Pipeline failed\n /    \\",
        "(•_•)\n( •_•)>⌐■-■\n(⌐■_■)",
        "(☕_☕)\nDeploying on Friday evening",
        "(ノಠ益ಠ)ノ彡┻━┻",
        "ヽ(｀Д´)ﾉ\nJenkins is stuck again",
        "(╥﹏╥)\nArgoCD says: OutOfSync",
        "(⊙_☉)\nSomeone pushed directly to main",
        "(¬_¬)\n\"It worked in staging\"",
        "(ಠ_ಠ)\nWho changed the YAML?",
        "(☠_☠)\nOOMKilled again",
        "(ง'̀-'́)ง\nFighting production incidents",
        "(；￣Д￣)\nTerraform plan looks suspicious",
        "(☉_☉)\nCPU usage: 99%",
        "(っ◕‿◕)っ\nPipeline finally passed",
        "(ಥ﹏ಥ)\nMerge conflict in values.yaml",
        "(• ε •)\nRestarted the pod. Problem solved temporarily.",
        "(╬ಠ益ಠ)\nCrashLoopBackOff",
        "(¬‿¬)\nBlaming DNS with confidence",
        "(ᵔᴥᵔ)\nAll pods are healthy",
        "(☕‿☕)\nMonitoring Grafana dashboards",
        "(⊙﹏⊙)\nkubectl apply in production",
        "(☞ﾟヮﾟ)☞\nGitOps all the things",
        "(⌐■_■)\nZero downtime deployment",
        "(；一_一)\nInvestigating why Jenkins worked yesterday",
        "(╯︵╰,)\nHelm upgrade failed"
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

        String clusterTruth  = FUN_FACTS.get(RANDOM.nextInt(FUN_FACTS.size()));
        String currentMood   = HOW_I_LOOK_RIGHT_NOW.get(RANDOM.nextInt(HOW_I_LOOK_RIGHT_NOW.size()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message",          "Hello from the GitOps Pipeline!");
        response.put("application",      buildProperties.getName());
        response.put("version",          buildProperties.getVersion());
        response.put("built_at",         builtAt);
        response.put("server_time",      serverTime);
        response.put("status",           "running");
        response.put("cluster_truth",    clusterTruth);
        response.put("current_mood",     currentMood);

        return ResponseEntity.ok(response);
    }
}