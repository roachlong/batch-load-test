package org.cockroachlabs.simulator.transaction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RawSqlService {

    @Inject
    DataSource dataSource;

    public int combinedTransaction(Parent parent, List<Integer> children) throws SQLException {
        String sql = """
        with prnt as (
            insert into parent (parentkey, type, numericid, characterid, nextcycle)
            values (?, ?, ?, ?, ?)
            on conflict (parentkey)
            do update set nextcycle = excluded.nextcycle
            returning *
        ), chld as (
            insert into child (parentkey, childid)
            select prnt.parentkey, v.column1
            from prnt, (values\s
        """;

        List<String> values = new ArrayList<>();
        for (Integer child: children) {
            String format = String.format("(%s)", child.toString());
            values.add(format);
        }
        sql += String.join(", ", values);

        sql += """
            ) v
            on conflict (parentkey, childid)
            do update set parentkey = excluded.parentkey
            returning *
        ), del as (
            delete from child
            where parentkey = (select parentkey from prnt)
              and childid not in (select childid from chld)
            returning *
        )
        select (select count(1) from prnt) +
               (select count(1) from chld) +
               (select count(1) from del) as modifications;
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parent.parentKey);
            stmt.setString(2, parent.type);
            stmt.setLong(3, parent.numericId);
            stmt.setString(4, parent.characterId);
            stmt.setInt(5, parent.nextCycle);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1); // your modifications count
            }
            return 0;
        }
    }
}

