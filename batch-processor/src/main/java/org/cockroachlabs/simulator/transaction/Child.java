package org.cockroachlabs.simulator.transaction;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_parent_child", columnList = "parentkey, childid")
})
public class Child extends PanacheEntityBase {

    @EmbeddedId
    @Column(nullable = false, updatable = false)
    public Relationship relationship;

    public Child() {
    }

    public Child(Relationship relationship) {
        this.relationship = relationship;
    }

    public static int insertOnConflict(String parentKey, List<Integer> children) {
        String sql = """
        insert into child (parentkey, childid)
        select :parentKey, v.column1
        from (values\s
        """;

        List<String> values = new ArrayList<>();
        for (Integer child: children) {
            String format = String.format("(%s)", child.toString());
            values.add(format);
        }
        sql += String.join(", ", values) + """
            ) v on conflict (parentkey, childid)
            do update set parentkey = excluded.parentkey;
        """;

        return getEntityManager().createNativeQuery(sql)
                .setParameter("parentKey", parentKey)
                .executeUpdate();
    }

    public static int deleteUnused(String parentKey, List<Integer> children) {
        String sql = """
        delete from child
        where parentkey = :parentKey
        and childid not in (
        """;

        List<String> values = children.stream().map(String::valueOf).toList();
        sql += String.join(", ", values) + ");";

        return getEntityManager().createNativeQuery(sql)
                .setParameter("parentKey", parentKey)
                .executeUpdate();
    }
}
