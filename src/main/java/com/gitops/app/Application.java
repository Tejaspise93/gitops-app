package com.gitops.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application Entry Point
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration      : marks this class as a source of bean definitions
 *   - @EnableAutoConfiguration : tells Spring Boot to auto-configure beans
 *                                based on dependencies found on the classpath
 *                                (e.g., sees spring-web on classpath → auto-configures Tomcat)
 *   - @ComponentScan      : scans this package and all sub-packages for
 *                           Spring-managed components (@RestController, @Service, etc.)
 *
 * This is the class Maven's spring-boot-maven-plugin will set as
 * Main-Class in the fat JAR's MANIFEST.MF, making it executable
 * via: java -jar gitops-app.jar
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        /*
         * SpringApplication.run() bootstraps the entire Spring context:
         *   1. Creates ApplicationContext
         *   2. Registers all beans found via @ComponentScan
         *   3. Triggers auto-configuration
         *   4. Starts the embedded Tomcat server on port 8080 (default)
         *   5. Begins accepting HTTP requests
         */
        SpringApplication.run(Application.class, args);
    }

}