package org.cockroachlabs.simulator.transaction;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Entity
public class Parent extends PanacheEntityBase {

    private static final String[] AREA = {"Network", "Hardware", "Software", "IT", "Cloud", "Data", "VoIP", "Computer", "Printing"};
    private static final String[] SERVICE = {"Management", "Support", "Services", "Security", "Encryption", "Training", "Analytics", "Monitoring"};
    public static final Random random = new Random();

    @Id
    @Column(nullable = false, updatable = false)
    public String parentKey;

    @Column(nullable = false)
    public String type;
    @Column(nullable = false)
    public Long numericId;
    public String characterId;
    @Column(nullable = false)
    public Integer nextCycle;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "relationship.parentKey")
    public Set<Child> children;

    public Parent() {
        this(RandomStringUtils.randomAlphanumeric(40));
    }

    public Parent(String parentKey) {
        this.parentKey = parentKey;
        this.type = AREA[random.nextInt(AREA.length)] + "_" + SERVICE[random.nextInt(SERVICE.length)];
        this.numericId = Math.round(Math.random() * 1000000000000000000L);
        this.characterId = RandomStringUtils.randomAlphanumeric(50);
        this.nextCycle = random.nextInt(100);
        this.children = IntStream.range(0, random.nextInt(10) + 1)
                .mapToObj(x -> new Child(new Relationship(parentKey, x+1)))
                .collect(Collectors.toSet());
    }

    public boolean rotateChildren() {
        if (nextCycle <= 0) {
            children.clear();
            return false;
        }
        else if (random.nextInt(100) < 20) {
            nextCycle--;
        }
        children.forEach(child -> child.relationship.childId++);
        return true;
    }

    public static int insertOnConflict(Parent parent) {
        String sql = """
        insert into parent (parentkey, type, numericid, characterid, nextcycle)
        values (:parentKey, :type, :numericId, :characterId, :nextCycle)
        on conflict (parentkey)
        do update set nextcycle = excluded.nextcycle;
        """;

        return getEntityManager().createNativeQuery(sql)
                .setParameter("parentKey", parent.parentKey)
                .setParameter("type", parent.type)
                .setParameter("numericId", parent.numericId)
                .setParameter("characterId", parent.characterId)
                .setParameter("nextCycle", parent.nextCycle)
                .executeUpdate();
    }
}
