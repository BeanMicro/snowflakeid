package snowflakeid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnowflakeIdGeneratorTest {

    private static final long EPOCH = 1759536000000L;
    private static final long MAX_MACHINE_ID = ~(-1L << 10L);
    private static final long SEQUENCE_BITS = 12;

    @Test
    void testDefaultEpoch() {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(123);
        long id = idGenerator.nextId(null);
        assertEquals(503808, id);
    }

    @ParameterizedTest
    @CsvSource({
            "2025-10-04T00:00:00, 123,, 503808, 0",
            "2037-05-28T14:24:45.448, 378,, 1541815603606036480, 0",
            "2037-05-28T14:24:45.448, 378, 0, 1541815603606036481, 1",
            "2037-05-28T14:24:45.448, 378, 1, 1541815603606036482, 2"
    })
    void testIdGeneration(String datetime, int machineId, Integer previousSequenceNumber, long expectedId, int expectedSequenceNumber) {
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(datetime + "Z");
        Instant instant = offsetDateTime.toInstant();

        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(machineId);
        if (previousSequenceNumber != null) {
            idGenerator.setSequence(previousSequenceNumber);
        }
        long id = idGenerator.nextId(instant);

        assertEquals(expectedId, id);
        assertEquals(expectedSequenceNumber, idGenerator.getSequence());
    }

    @Test
    void testMachineIdOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new SnowflakeIdGenerator(MAX_MACHINE_ID + 1));
    }
}