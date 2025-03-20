package org.cockroachlabs.simulator;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
public class Stats extends PanacheEntityBase implements Serializable {

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

    public static List<Stats> findAllStats() {
        String sql = """
        select *
        from stats
        as of system time follower_read_timestamp()
        order by starttime desc;
        """;
        return getEntityManager().createNativeQuery(sql, Stats.class).getResultList();
    }

    public static void updateStatistics(Stats stats) {
        String sql = """
        update stats
        set status = :status,
            elapsed = :elapsed,
            statements = statements + :statements,
            records = records + :records,
            throughput = (records + :records) / :elapsed
        where id = :id;
        """;
        stats.elapsed = stats.startTime.until(Instant.now(), ChronoUnit.SECONDS);
        if (stats.elapsed > stats.duration * 60) {
            stats.elapsed = stats.duration * 60L;
        }
        getEntityManager().createNativeQuery(sql)
                .setParameter("status", stats.status)
                .setParameter("elapsed", stats.elapsed)
                .setParameter("statements", stats.statements)
                .setParameter("records", stats.records)
                .setParameter("id", stats.id)
                .executeUpdate();
        stats.statements = 0;
        stats.records = 0;
    }
}
