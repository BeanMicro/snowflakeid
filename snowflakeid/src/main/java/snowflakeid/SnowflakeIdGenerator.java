package snowflakeid;

import java.time.Instant;

public class SnowflakeIdGenerator {

    private final long defaultEpoch = 1759536000000L;
    private final long machineIdBits = 10L;
    private final long maxMachineId = ~(-1L << machineIdBits);
    private final long sequenceBits = 12L;

    private long machineId;

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    private long sequence = -1L;

    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long machineId) {
        if (machineId > maxMachineId || machineId < 0) {
            throw new IllegalArgumentException(String.format("Machine Id can't be greater than %d or less than 0", maxMachineId));
        }
        this.machineId = machineId;
    }

    public synchronized long nextId(Instant instant) {
        long timestamp;
        if (instant != null) {
            timestamp = instant.toEpochMilli();
        } else {
            timestamp = defaultEpoch;
        }

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        sequence++;
//        TODO: consider this logic to make sure unique Id
//        if (lastTimestamp == timestamp) {
//            sequence = (sequence + 1) & maxSequence();
//            if (sequence == 0) {
//                timestamp = tilNextMillis(lastTimestamp);
//            }
//        } else {
//            sequence = 0;
//        }
//        lastTimestamp = timestamp;

        return ((timestamp - defaultEpoch) << sequenceBits + machineIdBits) | (machineId << sequenceBits) | sequence;
    }

    protected long maxSequence() {
        return ~(-1L << sequenceBits);
    }

    protected long timeGen() {
        return System.currentTimeMillis();
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = lastTimestamp;
        while (timestamp <= lastTimestamp) {
            timestamp++;
        }
        return timestamp;
    }
}