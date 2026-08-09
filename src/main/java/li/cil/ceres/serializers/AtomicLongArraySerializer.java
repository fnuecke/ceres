package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLongArray;

public final class AtomicLongArraySerializer implements Serializer<AtomicLongArray> {
    public static final AtomicLongArraySerializer INSTANCE = new AtomicLongArraySerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class<AtomicLongArray> type, final Object value) throws SerializationException {
        final AtomicLongArray typedValue = (AtomicLongArray) value;
        final long[] data = new long[typedValue.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = typedValue.get(i);
        }
        visitor.putObject("value", long[].class, data);
    }

    @Override
    public AtomicLongArray deserialize(final DeserializationVisitor visitor, final Class<AtomicLongArray> type, @Nullable final Object value) throws SerializationException {
        AtomicLongArray typedValue = (AtomicLongArray) value;
        if (visitor.exists("value")) {
            final long[] data = (long[]) visitor.getObject("value", long[].class, null);
            if (data != null) {
                if (typedValue == null || typedValue.length() != data.length) {
                    typedValue = new AtomicLongArray(data);
                } else {
                    for (int i = 0; i < data.length; i++) {
                        typedValue.set(i, data[i]);
                    }
                }
            }
        }
        return typedValue;
    }
}
