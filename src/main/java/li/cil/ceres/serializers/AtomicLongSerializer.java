package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicLong;

public final class AtomicLongSerializer implements Serializer<AtomicLong> {
    public static final AtomicLongSerializer INSTANCE = new AtomicLongSerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class<AtomicLong> type, final Object value) throws SerializationException {
        visitor.putLong("value", ((AtomicLong) value).get());
    }

    @Override
    public AtomicLong deserialize(final DeserializationVisitor visitor, final Class<AtomicLong> type, @Nullable final Object value) throws SerializationException {
        AtomicLong typedValue = (AtomicLong) value;
        if (visitor.exists("value")) {
            if (typedValue == null) {
                typedValue = new AtomicLong();
            }
            typedValue.set(visitor.getLong("value"));
        }
        return typedValue;
    }
}
