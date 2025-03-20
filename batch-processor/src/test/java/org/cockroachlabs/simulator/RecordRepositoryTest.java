package org.cockroachlabs.simulator;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class RecordRepositoryTest {

    @Test
    @TestTransaction
    public void shouldCreateAndFindAnEntity() throws SQLException {
        long count = Record.count();
        int listAll = Record.listAll().size();
        assertEquals(count, listAll);
        Record entity = new Record("test");

        Record.persist(entity);
        assertNotNull(entity.id);

        assertEquals(count + 1, Record.count());

        entity = Record.findById(entity.id);
        entity = Record.findByName(entity.processName).orElseThrow(EntityNotFoundException::new);
        assertEquals("test", entity.processName);
        assertFalse(Record.findContainName("test").isEmpty());

        Record.deleteById(entity.id);
        assertEquals(count, Record.count());
    }
}
