package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicIntegerArray;

public final class AtomicIntegerArraySerializer implements Serializer<AtomicIntegerArray> {
    public static final AtomicIntegerArraySerializer INSTANCE = new AtomicIntegerArraySerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class<AtomicIntegerArray> type, final Object value) throws SerializationException {
        final AtomicIntegerArray typedValue = (AtomicIntegerArray) value;
        final int[] data = new int[typedValue.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = typedValue.get(i);
        }
        visitor.putObject("value", int[].class, data);
    }

    @Override
    public AtomicIntegerArray deserialize(final DeserializationVisitor visitor, final Class<AtomicIntegerArray> type, @Nullable final Object value) throws SerializationException {
        AtomicIntegerArray typedValue = (AtomicIntegerArray) value;
        if (visitor.exists("value")) {
            final int[] data = (int[]) visitor.getObject("value", int[].class, null);
            if (data != null) {
                if (typedValue == null || typedValue.length() != data.length) {
                    typedValue = new AtomicIntegerArray(data);
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
