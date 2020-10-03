package li.cil.ceres.serializers;

import li.cil.ceres.api.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

@RegisterSerializer
public final class ByteBufferSerializer implements Serializer<ByteBuffer> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<ByteBuffer> type, final Object value) throws SerializationException {
        final ByteBuffer buffer = (ByteBuffer) value;
        visitor.putObject("value", byte[].class, buffer.slice().array());
    }

    @Override
    public ByteBuffer deserialize(final DeserializationVisitor visitor, final Class<ByteBuffer> type, @Nullable final Object value) throws SerializationException {
        ByteBuffer buffer = (ByteBuffer) value;
        if (!visitor.exists("value")) {
            return buffer;
        }

        final byte[] data = (byte[]) visitor.getObject("value", byte[].class, null);
        if (data == null) {
            return null;
        }

        if (buffer == null || buffer.capacity() < data.length) {
            buffer = ByteBuffer.allocate(data.length);
        }

        buffer.clear();
        buffer.put(data);

        return buffer;
    }
}
