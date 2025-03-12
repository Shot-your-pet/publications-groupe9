package fr.miage.syp.publication.services

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong


/**
 * Custom implementation of the Snowflake ID generator.
 * Generates a unique 64-bit ID based on timestamp, machine ID, datacenter ID, and sequence number.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
internal class SnowflakeIdGenerator {
    private val lastTimestamp = AtomicLong(-1L)
    private val sequence = AtomicLong(0L)

    private val machineId = SecureRandom().nextLong(0, 32) and 0b11111

    /**
     * Generates the next unique Snowflake ID.
     *
     * @param datacenterId Unique identifier for the datacenter (0-31)
     * @return A globally unique 64-bit ID
     */
    @Synchronized
    fun nextId(datacenterId: Long): Long {
        var timestamp = System.currentTimeMillis()
        // If the timestamp is the same as the previous one, increment sequence
        if (timestamp == lastTimestamp.get()) {
            val seq = (sequence.incrementAndGet()) and MAX_SEQUENCE
            if (seq == 0L) {
                timestamp = waitNextMicros(timestamp)
            }
        } else {
            sequence.set(0L)
        }
        // Update last timestamp
        lastTimestamp.set(timestamp)
        // Construct the 64-bit ID using bit shifts
        return (((timestamp - START_TIMESTAMP) shl TIMESTAMP_SHIFT.toInt()) or (datacenterId shl DATACENTER_ID_SHIFT.toInt()) or (machineId shl MACHINE_ID_SHIFT.toInt()) or sequence.get())
    }

    /**
     * Waits until the next microsecond if the sequence number exceeds the limit.
     *
     * @param initTimestamp The current timestamp in microseconds.
     * @return The next valid timestamp.
     */
    private fun waitNextMicros(initTimestamp: Long): Long {
        var timestamp = initTimestamp
        while (timestamp <= lastTimestamp.get()) {
            timestamp = System.nanoTime() / 1000
        }
        return timestamp
    }

    companion object {
        // Epoch timestamp in milliseconds
        private const val START_TIMESTAMP = 1741783382057

        // Number of bits allocated for Machine ID (max value: 31)
        private const val MACHINE_ID_BITS = 5L

        // Number of bits allocated for Datacenter ID (max value: 31)
        private const val DATACENTER_ID_BITS = 5L

        // Number of bits allocated for Sequence (max value: 4095 per millisecond per node)
        private const val SEQUENCE_BITS = 12L

        // Maximum value for the sequence number
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS.toInt()) - 1

        // Shift left for Machine ID
        private const val MACHINE_ID_SHIFT = SEQUENCE_BITS

        // Shift left for Datacenter ID
        private const val DATACENTER_ID_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS

        // Shift left for Timestamp
        private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS + DATACENTER_ID_BITS
    }
}
