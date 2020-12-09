package li.cil.ceres.serializers;

import li.cil.ceres.api.*;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

@RegisterSerializer
public final class ByteBufferSerializer implements Serializer<ByteBuffer> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<ByteBuffer> type, final Object value) throws SerializationException {
        final ByteBuffer buffer = (ByteBuffer) value;
        visitor.putInt("capacity", buffer.capacity());
        visitor.putInt("position", buffer.position());
        visitor.putInt("limit", buffer.limit());
        final int pos = buffer.position();
        int mark = -1;
        try {
            buffer.reset();
            mark = buffer.position();
            buffer.position(pos);
        } catch (final InvalidMarkException ignored) {
        }
        visitor.putInt("mark", mark);

        final byte[] data = new byte[buffer.remaining()];
        for (int i = 0; i < buffer.remaining(); i++) {
            data[i] = buffer.get();
        }

        buffer.position(pos);
        visitor.putObject("value", byte[].class, data);
    }

    @Override
    public ByteBuffer deserialize(final DeserializationVisitor visitor, final Class<ByteBuffer> type, @Nullable final Object value) throws SerializationException {
        ByteBuffer buffer = (ByteBuffer) value;
        if (!visitor.exists("capacity") ||
            !visitor.exists("position") ||
            !visitor.exists("limit") ||
            !visitor.exists("mark") ||
            !visitor.exists("value")) {
            return buffer;
        }

        final int capacity = visitor.getInt("capacity");
        final int position = visitor.getInt("position");
        final int limit = visitor.getInt("limit");
        final int mark = visitor.getInt("mark");
        final byte[] data = (byte[]) visitor.getObject("value", byte[].class, null);
        if (data == null) {
            return null;
        }

        if (buffer == null || buffer.capacity() < capacity) {
            buffer = ByteBuffer.allocate(capacity);
        }

        buffer.clear();

        if (mark >= 0) {
            buffer.position(mark);
            buffer.mark();
        }

        buffer.position(position);
        buffer.put(data);
        buffer.position(position);
        buffer.limit(limit);

        return buffer;
    }
}
