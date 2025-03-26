package org.cockroachlabs.simulator.batch;

import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import org.apache.commons.lang3.SerializationUtils;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
        batch.size = type.equals("SINGLE") ? 1 : size;
        batch.connections = connections;
        batch.transactions = 0L;
        batch.statements = 0L;
        batch.records = 0L;
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
            int records = 0;
            switch (batch.type) {
                case "SINGLE":
                    records = singleRun();
                    if (records > 0) {
                        batch.transactions += 1;
                        batch.statements += 1;
                        batch.records += records;
                    }
                    break;
                case "BATCH":
                    records = batchRun(batch.size);
                    if (records > 0) {
                        batch.transactions += 1;
                        batch.statements += batch.size;
                        batch.records += records;
                    }
                    break;
                case "MULTI-VALUE":
                    records = multiValueRun(batch.size);
                    if (records > 0) {
                        batch.transactions += 1;
                        batch.statements += 1;
                        batch.records += records;
                    }
                    break;
                default:
                    // ignore
            }
        }
        batch.status = "DONE";
    }

    @Transactional
    public int singleRun() {
        var retries = 0;
        while (retries < 10) {
            try {
                Record.persist(new Record());
                return 1;
            } catch (PersistenceException e) {
                retries++;
                try {
                    Thread.sleep(1000L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return 0;
    }

    @Transactional
    public int batchRun(int size) {
        var retries = 0;
        while (retries < 10) {
            try {
                Record.persist(IntStream.range(0, size)
                        .mapToObj(x -> new Record())
                        .toList()
                );
                return size;
            } catch (PersistenceException e) {
                retries++;
                try {
                    Thread.sleep(1000L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return 0;
    }

    @Transactional
    public int multiValueRun(int size) {
        var retries = 0;
        while (retries < 10) {
            try {
                return Record.multiValueInsert(IntStream.range(0, size)
                        .mapToObj(x -> new Record())
                        .toList()
                );
            } catch (PersistenceException e) {
                retries++;
                try {
                    Thread.sleep(1000L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return 0;
    }

    @Transactional
    public void updateStats(Batch batch) {
        try {
            Thread.sleep(30000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var retries = 0;
        while (retries < 10) {
            try {
                Batch.updateStatistics(batch);
                break;
            } catch (PersistenceException e) {
                retries++;
                try {
                    Thread.sleep(1000L * retries);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (Instant.now().isBefore(batch.startTime.plusSeconds(batch.duration * 60 + 30))) {
            CompletableFuture.runAsync(() -> updateStats(batch));
        }
    }
}
