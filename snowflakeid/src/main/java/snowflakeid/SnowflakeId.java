package snowflakeid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeId {
    private static volatile SnowflakeId instance;

    private static final long defaultEpoch = 1759536000000L;
    private static final long machineIdBits = 10L;
    private static final long sequenceBits = 12L;

    private static final long maxMachineId = ~(-1L << machineIdBits);

    private long machineId = getMachineId();
    private static final long machineIdShift = sequenceBits;
    private static final long timestampLeftShift = sequenceBits + machineIdBits;
    private static final long sequenceMask = ~(-1L << sequenceBits);

    private Clock clock = Clock.systemUTC();

    private volatile long lastTimestamp = -1L;
    private long epoch;
    private AtomicLong sequence = new AtomicLong(0);

    private SnowflakeId(long epoch) {
        this.epoch = epoch;
    }

    public static SnowflakeId getInstance(Long epoch) {
        if (instance == null) {
            synchronized (SnowflakeId.class) {
                if (instance == null) {
                    long finalEpoch = epoch == null ? defaultEpoch : epoch;
                    instance = new SnowflakeId(finalEpoch);
                }
            }
        }
        return instance;
    }

    public long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate ID for %d milliseconds", lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence.set((sequence.get() + 1) & sequenceMask);
            if (sequence.get() == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence.set(0);
        }

        lastTimestamp = timestamp;

        return ((timestamp - epoch) << timestampLeftShift) |
                (machineId << machineIdShift) |
                sequence.get();
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return Instant.now(clock).toEpochMilli();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static long getMachineId() {
        try {
            // Get the MAC address
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            byte[] macBytes = networkInterface.getHardwareAddress();

            // Get the process ID
            long processId = ProcessHandle.current().pid();

            // Concatenate MAC address and process ID
            String combinedString = bytesToHex(macBytes) + processId;

            // Compute hash value (SHA-256) of the concatenated string
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(combinedString.getBytes());

            // Extract a portion of the hash value to use as the machine ID
            return Math.abs(hashBytes[0]);
        } catch (Exception e) {
            return new Random().nextLong() & maxMachineId;
        }
    }
}