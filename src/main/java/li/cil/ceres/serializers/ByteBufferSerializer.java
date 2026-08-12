package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.InvalidMarkException;

public final class ByteBufferSerializer implements Serializer<ByteBuffer> {
    private static final int NO_MARK = -1;

    @Override
    public void serialize(final SerializationVisitor visitor, final Class<ByteBuffer> type, final Object value) throws SerializationException {
        final ByteBuffer buffer = (ByteBuffer) value;

        visitor.putInt("capacity", buffer.capacity());
        visitor.putInt("position", buffer.position());
        visitor.putInt("limit", buffer.limit());

        final ByteBuffer view = buffer.duplicate();

        int mark = NO_MARK;
        try {
            view.reset();
            mark = view.position();
        } catch (final InvalidMarkException ignored) {
        }
        visitor.putInt("mark", mark);
        visitor.putBoolean("bigEndian", buffer.order() == ByteOrder.BIG_ENDIAN);

        final byte[] data = new byte[buffer.capacity()];
        view.clear();
        view.get(data);

        visitor.putObject("value", byte[].class, data);
    }

    @Override
    public ByteBuffer deserialize(final DeserializationVisitor visitor, final Class<ByteBuffer> type, @Nullable final Object value) throws SerializationException {
        ByteBuffer buffer = (ByteBuffer) value;
        if (!visitor.exists("capacity") ||
                !visitor.exists("position") ||
                !visitor.exists("limit") ||
                !visitor.exists("mark") ||
                !visitor.exists("bigEndian") ||
                !visitor.exists("value")) {
            return buffer;
        }

        final int capacity = visitor.getInt("capacity");
        final int position = visitor.getInt("position");
        final int limit = visitor.getInt("limit");
        final int mark = visitor.getInt("mark");
        final ByteOrder order = visitor.getBoolean("bigEndian") ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        final byte[] data = (byte[]) visitor.getObject("value", byte[].class, null);
        if (data == null) {
            return null;
        }

        if (buffer == null || buffer.capacity() < capacity) {
            buffer = ByteBuffer.allocate(capacity);
        }

        buffer.order(order);
        buffer.clear();
        buffer.put(data);

        buffer.clear();
        if (mark != NO_MARK) {
            buffer.position(mark);
            buffer.mark();
        }
        buffer.position(position);
        buffer.limit(limit);

        return buffer;
    }
}
