package org.cockroachlabs.simulator.batch;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
public class Batch extends PanacheEntityBase implements Serializable {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    public String type;
    public Instant startTime;
    public String status;
    public Integer duration;
    public Long elapsed;
    public Integer batchSize;
    public Integer connections;
    public Integer statements;
    public Integer records;
    public Double throughput;

    public static List<Batch> findAllStats() {
        String sql = """
        select *
        from batch
        as of system time follower_read_timestamp()
        order by starttime desc;
        """;
        return getEntityManager().createNativeQuery(sql, Batch.class).getResultList();
    }

    public static void updateStatistics(Batch batch) {
        String sql = """
        update batch
        set status = :status,
            elapsed = :elapsed,
            statements = statements + :statements,
            records = records + :records,
            throughput = (records + :records) / :elapsed
        where id = :id;
        """;
        batch.elapsed = batch.startTime.until(Instant.now(), ChronoUnit.SECONDS);
        if (batch.elapsed > batch.duration * 60) {
            batch.elapsed = batch.duration * 60L;
        }
        getEntityManager().createNativeQuery(sql)
                .setParameter("status", batch.status)
                .setParameter("elapsed", batch.elapsed)
                .setParameter("statements", batch.statements)
                .setParameter("records", batch.records)
                .setParameter("id", batch.id)
                .executeUpdate();
        batch.statements = 0;
        batch.records = 0;
    }
}
