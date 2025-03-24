package org.cockroachlabs.simulator.batch;

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

@Path("/batch/service")
public class DataLoadResource {

    @Inject
    Template batchForm;

    @Inject
    Template batchTemplate;

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showBatchForm() {
        return batchForm.instance();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTestStatistics() {
        final List<Batch> stats = Batch.findAllStats();
        return batchTemplate.data("stats", stats);
    }

    @POST
    @Path("/")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public void executeNewTest(@FormParam("type") @DefaultValue("BATCH") String type,
                               @FormParam("duration") @DefaultValue("5") Integer duration,
                               @FormParam("size") @DefaultValue("128") Integer size,
                               @FormParam("connections") @DefaultValue("4") Integer connections) {
        Batch batch = new Batch();
        batch.type = type;
        batch.startTime = Instant.now();
        batch.status = "RUNNING";
        batch.duration = duration;
        batch.elapsed = 0L;
        batch.batchSize = type.equals("SINGLE") ? 1 : size;
        batch.connections = connections;
        batch.statements = 0;
        batch.records = 0;
        batch.throughput = 0.0;
        Batch.persist(batch);
        for (int i = 0; i < connections; i++) {
            CompletableFuture.runAsync(() -> {
                executeLoad(SerializationUtils.clone(batch));
            });
        }
    }

    private void executeLoad(Batch batch) {
        CompletableFuture.runAsync(() -> updateStats(batch));
        Instant endTime = batch.startTime.plusSeconds(batch.duration * 60);
        while (Instant.now().isBefore(endTime)) {
            switch (batch.type) {
                case "SINGLE":
                    singleRun();
                    batch.statements += 1;
                    batch.records += 1;
                    break;
                case "BATCH":
                    batchRun(batch.batchSize);
                    batch.statements += batch.batchSize;
                    batch.records += batch.batchSize;
                    break;
                case "MULTI-VALUE":
                    multiValueRun(batch.batchSize);
                    batch.statements += 1;
                    batch.records += batch.batchSize;
                    break;
                default:
                    // ignore
            }
        }
        batch.status = "DONE";
    }

    @Transactional
    public void singleRun() {
        Record.persist(new Record());
    }

    @Transactional
    public void batchRun(int batchSize) {
        Record.persist(IntStream.range(0, batchSize)
                .mapToObj(x -> new Record())
                .collect(Collectors.toList())
        );
    }

    @Transactional
    public void multiValueRun(int batchSize) {
        Record.multiValueInsert(IntStream.range(0, batchSize)
                .mapToObj(x -> new Record())
                .collect(Collectors.toList())
        );
    }

    @Transactional
    public void updateStats(Batch batch) {
        try {
            Thread.sleep(30000);
        }
        catch (InterruptedException ignore) {
        }
        Batch.updateStatistics(batch);
        if (Instant.now().isBefore(batch.startTime.plusSeconds(batch.duration * 60 + 1))) {
            CompletableFuture.runAsync(() -> updateStats(batch));
        }
    }
}
