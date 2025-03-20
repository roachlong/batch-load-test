package org.cockroachlabs.simulator;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.apache.commons.lang3.SerializationUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Path("/service")
public class DataLoadResource {

    @Inject
    Template testForm;

    @Inject
    Template statsTemplate;

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showTestForm() {
        return testForm.instance();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTestStatistics() {
        final List<Stats> stats = Stats.findAllStats();
        return statsTemplate.data("stats", stats);
    }

    @POST
    @Path("/")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public void executeNewTest(@FormParam("type") @DefaultValue("BATCH") String type,
                               @FormParam("duration") @DefaultValue("5") Integer duration,
                               @FormParam("size") @DefaultValue("128") Integer size,
                               @FormParam("connections") @DefaultValue("20") Integer connections) {
        Stats stats = new Stats();
        stats.type = type;
        stats.startTime = Instant.now();
        stats.status = "RUNNING";
        stats.duration = duration;
        stats.elapsed = 0L;
        stats.batchSize = type.equals("SINGLE") ? 1 : size;
        stats.connections = connections;
        stats.statements = 0;
        stats.records = 0;
        stats.throughput = 0.0;
        Stats.persist(stats);
        for (int i = 0; i < connections; i++) {
            CompletableFuture.runAsync(() -> {
                executeLoad(SerializationUtils.clone(stats));
            });
        }
    }

    private void executeLoad(Stats stats) {
        CompletableFuture.runAsync(() -> updateStats(stats));
        Instant endTime = stats.startTime.plusSeconds(stats.duration * 60);
        while (Instant.now().isBefore(endTime)) {
            switch (stats.type) {
                case "SINGLE":
                    singleRun();
                    stats.statements += 1;
                    stats.records += 1;
                    break;
                case "BULK":
                    bulkRun(stats.batchSize);
                    stats.statements += stats.batchSize;
                    stats.records += stats.batchSize;
                    break;
                case "BATCH":
                    batchRun(stats.batchSize);
                    stats.statements += 1;
                    stats.records += stats.batchSize;
                    break;
                default:
                    // ignore
            }
        }
        stats.status = "DONE";
    }

    @Transactional
    public void singleRun() {
        Record.persist(new Record());
    }

    @Transactional
    public void bulkRun(int batchSize) {
        Record.persist(IntStream.range(0, batchSize)
                .mapToObj(x -> new Record())
                .collect(Collectors.toList())
        );
    }

    @Transactional
    public void batchRun(int batchSize) {
        Record.multiValueInsert(IntStream.range(0, batchSize)
                .mapToObj(x -> new Record())
                .collect(Collectors.toList())
        );
    }

    @Transactional
    public void updateStats(Stats stats) {
        try {
            Thread.sleep(30000);
        }
        catch (InterruptedException ignore) {
        }
        Stats.updateStatistics(stats);
        if (Instant.now().isBefore(stats.startTime.plusSeconds(stats.duration * 60 + 1))) {
            CompletableFuture.runAsync(() -> updateStats(stats));
        }
    }
}
