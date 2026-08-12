package li.cil.ceres;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void contentBeforePositionIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        for (int i = 0; i < 500; i++) {
            buffer.put((byte) (i + 1));
        }

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(500, deserialized.position());
        assertEquals(1024, deserialized.limit());
        assertArrayEquals(backingStore(buffer), backingStore(deserialized));
    }

    @Test
    public void contentOutsideLimitIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);
        buffer.clear();
        buffer.limit(0);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(1024, deserialized.capacity());
        assertEquals(0, deserialized.position());
        assertEquals(0, deserialized.limit());
        assertArrayEquals(backingStore(buffer), backingStore(deserialized));
    }

    @Test
    public void deserializingIntoExistingBufferRestoresFullBackingStore() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);
        buffer.position(42);

        final ByteBuffer into = ByteBuffer.allocate(1024);
        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class, into);

        assertSame(into, deserialized);
        assertEquals(42, deserialized.position());
        assertArrayEquals(backingStore(buffer), backingStore(deserialized));
    }

    @Test
    public void byteOrderIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(0x01020304);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(ByteOrder.LITTLE_ENDIAN, deserialized.order());
        assertEquals(0x01020304, deserialized.getInt(0), "value reads back identically");
        assertArrayEquals(backingStore(buffer), backingStore(deserialized));
    }

    @Test
    public void defaultByteOrderIsSerializedCorrectly() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        fill(buffer);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class);

        assertEquals(ByteOrder.BIG_ENDIAN, deserialized.order());
    }

    @Test
    public void byteOrderIsRestoredIntoExistingBuffer() {
        final ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        final ByteBuffer into = ByteBuffer.allocate(1024);
        into.order(ByteOrder.BIG_ENDIAN);

        final ByteBuffer deserialized = BinarySerialization.deserialize(BinarySerialization.serialize(buffer, ByteBuffer.class), ByteBuffer.class, into);

        assertSame(into, deserialized);
        assertEquals(ByteOrder.LITTLE_ENDIAN, deserialized.order());
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

    private static byte[] backingStore(final ByteBuffer buffer) {
        final byte[] data = new byte[buffer.capacity()];
        buffer.duplicate().clear().get(data);
        return data;
    }
}
