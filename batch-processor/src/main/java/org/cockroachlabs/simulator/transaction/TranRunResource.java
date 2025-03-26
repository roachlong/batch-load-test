package org.cockroachlabs.simulator.transaction;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.SerializationUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

@Path("/transaction/service")
public class TranRunResource {

    @Inject
    Template tranForm;

    @Inject
    Template tranTemplate;

    @Inject
    RawSqlService sqlService;

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance showTranForm() {
        return tranForm.instance();
    }

    @GET
    @Path("/stats")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getTestStatistics() {
        final List<Run> stats = Run.findAllStats();
        return tranTemplate.data("stats", stats);
    }

    @POST
    @Path("/")
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public void executeNewTest(@FormParam("type") @DefaultValue("COMBINED") String type,
                               @FormParam("duration") @DefaultValue("5") Integer duration,
                               @FormParam("size") @DefaultValue("128") Integer size,
                               @FormParam("connections") @DefaultValue("4") Integer connections) {
        Run run = new Run();
        run.type = type;
        run.startTime = Instant.now();
        run.status = "RUNNING";
        run.duration = duration;
        run.elapsed = 0L;
        run.size = size;
        run.connections = connections;
        run.transactions = 0L;
        run.statements = 0L;
        run.modifications = 0L;
        run.throughput = 0.0;
        Run.persist(run);
        for (int i = 0; i < connections; i++) {
            CompletableFuture.runAsync(() -> {
                executeRun(SerializationUtils.clone(run));
            });
        }
    }

    private void executeRun(Run run) {
        CompletableFuture.runAsync(() -> updateStats(run));
        Instant endTime = run.startTime.plusSeconds(run.duration * 60);
        List<Parent> parents = new ArrayList<>(
                IntStream.range(0, run.size)
                        .mapToObj(x -> new Parent())
                        .toList()
        );
        while (Instant.now().isBefore(endTime)) {
            Parent parent = parents.get(Parent.random.nextInt(parents.size()));
            List<Integer> children = parent.children.stream()
                    .map(child -> child.relationship.childId)
                    .toList();
            long modifications = 0;
            switch (run.type) {
                case "INDIVIDUAL":
                    modifications = individualRun(parent, children);
                    if (modifications > 0) {
                        run.transactions += 1;
                        run.statements += 3;
                        run.modifications += modifications;
                    }
                    break;
                case "COMBINED":
                    modifications = combinedRun(parent, children);
                    if (modifications > 0) {
                        run.transactions += 1;
                        run.statements += 1;
                        run.modifications += modifications;
                    }
                    break;
                default:
                    // ignore
            }
            if (!parent.rotateChildren() && parents.remove(parent)) {
                modifications = clearChildRecords(parent);
                if (modifications > 0) {
                    run.transactions += 1;
                    run.statements += 1;
                    run.modifications += modifications;
                }
                parents.add(new Parent());
            }
        }
        run.status = "DONE";
    }

    @Transactional
    public long individualRun(Parent parent, List<Integer> children) {
        var retries = 0;
        while (retries < 10) {
            try {
                long modifications = Parent.insertOnConflict(parent);
                modifications += Child.insertOnConflict(parent.parentKey, children);
                modifications += Child.deleteUnused(parent.parentKey, children);
                return modifications;
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

    public long combinedRun(Parent parent, List<Integer> children) {
        var retries = 0;
        while (retries < 10) {
            try {
                return sqlService.combinedTransaction(parent, children);
            } catch (SQLException e) {
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
    public long clearChildRecords(Parent parent) {
        var retries = 0;
        while (retries < 10) {
            try {
                return Child.delete("relationship.parentKey", parent.parentKey);
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
    public void updateStats(Run run) {
        try {
            Thread.sleep(30000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        var retries = 0;
        while (retries < 10) {
            try {
                Run.updateStatistics(run);
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
        if (Instant.now().isBefore(run.startTime.plusSeconds(run.duration * 60 + 30))) {
            CompletableFuture.runAsync(() -> updateStats(run));
        }
    }
}
