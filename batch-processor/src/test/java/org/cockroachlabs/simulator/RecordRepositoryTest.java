package org.cockroachlabs.simulator;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.EntityNotFoundException;
import org.cockroachlabs.simulator.batch.Record;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecordRepositoryTest {

    @Test
    @TestTransaction
    public void shouldCreateAndFindAnEntity() throws SQLException {
        long count = org.cockroachlabs.simulator.batch.Record.count();
        int listAll = org.cockroachlabs.simulator.batch.Record.listAll().size();
        assertEquals(count, listAll);
        org.cockroachlabs.simulator.batch.Record entity = new org.cockroachlabs.simulator.batch.Record("test");

        org.cockroachlabs.simulator.batch.Record.persist(entity);
        assertNotNull(entity.id);

        assertEquals(count + 1, org.cockroachlabs.simulator.batch.Record.count());

        entity = org.cockroachlabs.simulator.batch.Record.findById(entity.id);
        entity = org.cockroachlabs.simulator.batch.Record.findByName(entity.processName).orElseThrow(EntityNotFoundException::new);
        assertEquals("test", entity.processName);
        assertFalse(org.cockroachlabs.simulator.batch.Record.findContainName("test").isEmpty());

        org.cockroachlabs.simulator.batch.Record.deleteById(entity.id);
        assertEquals(count, Record.count());
    }
}
