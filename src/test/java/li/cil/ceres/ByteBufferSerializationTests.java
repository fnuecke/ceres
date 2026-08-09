package li.cil.ceres;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ByteBufferSerializationTests {
    @Test
    public void capacityIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertTrue(buffer.capacity() <= deserialized.capacity());
    }

    @Test
    public void positionIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.position(42);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(42, deserialized.position());
    }

    @Test
    public void limitIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);

        buffer.limit(42);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(0, deserialized.position());
        assertEquals(42, deserialized.limit());
    }

    @Test
    public void markIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(42);
        buffer.mark();
        buffer.position(69);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(69, deserialized.position());
        deserialized.reset();
        assertEquals(42, deserialized.position());
    }

    @Test
    public void contentIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);
        buffer.flip();

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(buffer.remaining(), deserialized.remaining());
        assertArrayEquals(remaining(buffer), remaining(deserialized));
    }

    @Test
    public void contentIsSerializedCorrectlyForPartialRegion() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);

        buffer.position(11);
        buffer.limit(1000);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(11, deserialized.position());
        assertEquals(1000, deserialized.limit());
        assertArrayEquals(remaining(buffer), remaining(deserialized));
    }

    @Test
    public void serializationDoesNotConsumeSourceBuffer() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);
        buffer.flip();

        BinarySerialization.serialize(buffer, ByteBuffer.class);

        assertEquals(0, buffer.position());
        assertEquals(1024, buffer.limit());
    }

    private static void fill(final ByteBuffer buffer) {
        for (int i = 0; i < buffer.capacity(); i++) {
            buffer.put((byte) (i + 1));
        }
    }

    private static byte[] remaining(final ByteBuffer buffer) {
        final byte[] data = new byte[buffer.remaining()];
        buffer.duplicate().get(data);
        return data;
    }
}
