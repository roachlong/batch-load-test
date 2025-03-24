package org.cockroachlabs.simulator.batch;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
public class Record extends PanacheEntityBase {

    private static final String[] AREA = {"Network", "Hardware", "Software", "IT", "Cloud", "Data", "VoIP", "Computer", "Printing"};
    private static final String[] SERVICE = {"Management", "Support", "Services", "Security", "Encryption", "Training", "Analytics", "Monitoring"};
    private static final Random random = new Random();

    @Id
    @Column(name = "process_id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;

    @Column(nullable = false)
    public String processName;
    public Instant updatedOn = Instant.now();
    public Instant createdOn = Instant.now();
    public Boolean leader = Boolean.FALSE;

    public Record() {
        this.processName = AREA[random.nextInt(AREA.length)] + "_" + SERVICE[random.nextInt(SERVICE.length)];
        this.leader = Math.random() <= 0.1;
    }

    public Record(String processName) {
        this.processName = processName;
        this.leader = Math.random() <= 0.1;
    }

    public static Optional<Record> findByName(String name) {
        return Record.find("processName", name).firstResultOptional();
    }

    public static List<Record> findContainName(String name) {
        return Record.list("processName like ?1", "%" + name + "%");
    }

    public static void multiValueInsert(List<Record> records) {
        String sql = """
        insert into record (process_id, processname, updatedon, createdon, leader) values
        """;

        List<String> values = new ArrayList<>();
        for (Record record: records) {
            String format = String.format(
                    "(gen_random_uuid(), '%s', '%s', '%s', %s)",
                    record.processName,
                    record.updatedOn.toString(),
                    record.createdOn.toString(),
                    record.leader.toString()
            );
            values.add(format);
        }
        sql += String.join(", ", values) + ";";
        getEntityManager().createNativeQuery(sql).executeUpdate();
    }
}
