package snowflakeid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeId {
    private static final long DEFAULT_EPOCH = 1759536000000L;
    private static final byte MACHINE_ID_BITS = 10;
    private static final byte SEQUENCE_BITS = 12;
    private static final short MAX_MACHINE_ID = ~(-1 << MACHINE_ID_BITS);
    private static final byte MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final byte TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final short SEQUENCE_MASK = ~(-1 << SEQUENCE_BITS);

    private Clock clock;
    private int machineId;

    protected long epoch;
    private AtomicLong lastTsBasedSequence;

    private static class SingletonHelper {
        private static final SnowflakeId INSTANCE = new SnowflakeId();
    }

    public static SnowflakeId getInstance() {
        return SingletonHelper.INSTANCE;
    }

    protected SnowflakeId() {
        this.epoch = DEFAULT_EPOCH;
        this.machineId = getMachineId();
        this.lastTsBasedSequence = new AtomicLong(0);
        this.clock = Clock.systemUTC();
    }

    public long getEpoch() {
        return epoch;
    }

    public long nextId() {
        long lastTimestamp = lastTsBasedSequence.get() >> SEQUENCE_BITS;
        long now = getCurrentTimestamp();

        if (now < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds", lastTimestamp - now));
        }
        long curr = updateLastTimestampBasedSequenceAndGet(lastTimestamp, now);

        return ((curr >> SEQUENCE_BITS) << TIMESTAMP_LEFT_SHIFT) |
                (machineId << MACHINE_ID_SHIFT) |
                curr & SEQUENCE_MASK;
    }

    long updateLastTimestampBasedSequenceAndGet(long lastTimestamp, long currentTimestamp) {
        if (lastTimestamp < currentTimestamp) {
            return lastTsBasedSequence.accumulateAndGet(currentTimestamp << SEQUENCE_BITS, (p, c) -> {
                if (p < c) { // To make sure that the current timestamp is still a more recent value
                    return c;
                }
                return p + 1;
            });
        }
        return lastTsBasedSequence.incrementAndGet();
    }

    private long getCurrentTimestamp() {
        return Instant.now(clock).toEpochMilli() - epoch;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static int getMachineId() {
        try {
            // Get the MAC address
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            byte[] macBytes = networkInterface.getHardwareAddress();

            // Get the process ID
            long processId = ProcessHandle.current().pid();

            // Concatenate MAC address and process ID
            String combinedString = bytesToHex(macBytes) + processId;

            // Extract a portion of the hash value to use as the machine ID
            return combinedString.hashCode() & MAX_MACHINE_ID;
        } catch (Exception e) {
            return new Random().nextInt() & MAX_MACHINE_ID;
        }
    }
}