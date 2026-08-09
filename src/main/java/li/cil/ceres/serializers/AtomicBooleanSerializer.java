package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AtomicBooleanSerializer implements Serializer<AtomicBoolean> {
    public static final AtomicBooleanSerializer INSTANCE = new AtomicBooleanSerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class<AtomicBoolean> type, final Object value) throws SerializationException {
        visitor.putBoolean("value", ((AtomicBoolean) value).get());
    }

    @Override
    public AtomicBoolean deserialize(final DeserializationVisitor visitor, final Class<AtomicBoolean> type, @Nullable final Object value) throws SerializationException {
        AtomicBoolean typedValue = (AtomicBoolean) value;
        if (visitor.exists("value")) {
            if (typedValue == null) {
                typedValue = new AtomicBoolean();
            }
            typedValue.set(visitor.getBoolean("value"));
        }
        return typedValue;
    }
}
