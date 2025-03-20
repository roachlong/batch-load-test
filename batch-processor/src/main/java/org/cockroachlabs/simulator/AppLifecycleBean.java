package org.cockroachlabs.simulator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import org.jboss.logging.Logger;
import io.quarkus.arc.profile.IfBuildProfile;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.io.File;

@ApplicationScoped
@IfBuildProfile("test")
public class AppLifecycleBean {
    private static final Logger LOGGER = Logger.getLogger(AppLifecycleBean.class);

    private static final DockerComposeContainer<?> database =
                new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yml"))
                        .withExposedService("crdb", 26257, Wait.forListeningPort());

    void onStart(@Observes StartupEvent ev) {
        LOGGER.info("The application is starting...");
        database.start();
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOGGER.info("The application is stopping...");
        database.stop();
    }
}

