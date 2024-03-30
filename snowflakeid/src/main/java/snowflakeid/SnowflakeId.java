package snowflakeid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeId {
    private static volatile SnowflakeId instance;
    private static final long DEFAULT_EPOCH = 1759536000000L;
    private static final int MACHINE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    private static final int MAX_MACHINE_ID = ~(-1 << MACHINE_ID_BITS);
    private static final int MACHINE_ID_SHIFT = SEQUENCE_BITS;
    private static final int TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS;
    private static final int SEQUENCE_MASK = ~(-1 << SEQUENCE_BITS);

    private Clock clock;
    private int machineId;

    private long epoch;
    private AtomicLong lastTsBasedSequence;

    private SnowflakeId(long epoch) {
        this.epoch = epoch;
        this.machineId = getMachineId();
        this.lastTsBasedSequence = new AtomicLong(0);
        this.clock = Clock.systemUTC();
    }

    public long getEpoch() {
        return epoch;
    }

    public static SnowflakeId getInstance(Long epoch) {
        if (instance == null) {
            synchronized (SnowflakeId.class) {
                if (instance == null) {
                    long finalEpoch = epoch == null ? DEFAULT_EPOCH : epoch;
                    instance = new SnowflakeId(finalEpoch);
                }
            }
        }
        return instance;
    }

    public long nextId() {
        long now = getCurrentTimestamp();
        long delta = now - epoch;
        long lastDelta = lastTsBasedSequence.get() >> SEQUENCE_BITS;

        if (delta < lastDelta) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds", lastDelta - now));
        }
        long curr;
        if (lastDelta == delta) {
            curr = lastTsBasedSequence.incrementAndGet();
        } else {
            curr = lastTsBasedSequence.accumulateAndGet(delta << SEQUENCE_BITS, (p, c) -> c);
        }

        return (delta << TIMESTAMP_LEFT_SHIFT) |
                (machineId << MACHINE_ID_SHIFT) |
                curr & SEQUENCE_MASK;
    }

    private long getCurrentTimestamp() {
        return Instant.now(clock).toEpochMilli();
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