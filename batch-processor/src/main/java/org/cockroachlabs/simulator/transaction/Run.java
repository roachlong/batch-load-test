package org.cockroachlabs.simulator.transaction;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Entity
public class Run extends PanacheEntityBase implements Serializable {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    public String type;
    public Instant startTime;
    public String status;
    public Integer duration;
    public Long elapsed;
    public Integer size;
    public Integer connections;
    public Long transactions;
    public Long statements;
    public Long modifications;
    public Double throughput;

    public static List<Run> findAllStats() {
        String sql = """
        select *
        from run
        as of system time follower_read_timestamp()
        order by starttime desc;
        """;
        return getEntityManager().createNativeQuery(sql, Run.class).getResultList();
    }

    public static void updateStatistics(Run run) {
        String sql = """
        update run
        set status = :status,
            elapsed = :elapsed,
            transactions = transactions + :transactions,
            statements = statements + :statements,
            modifications = modifications + :modifications,
            throughput = (modifications + :modifications) / :elapsed
        where id = :id;
        """;
        run.elapsed = run.startTime.until(Instant.now(), ChronoUnit.SECONDS);
        if (run.elapsed > run.duration * 60) {
            run.elapsed = run.duration * 60L;
        }
        getEntityManager().createNativeQuery(sql)
                .setParameter("status", run.status)
                .setParameter("elapsed", run.elapsed)
                .setParameter("transactions", run.transactions)
                .setParameter("statements", run.statements)
                .setParameter("modifications", run.modifications)
                .setParameter("id", run.id)
                .executeUpdate();
        run.transactions = 0L;
        run.statements = 0L;
        run.modifications = 0L;
    }
}
