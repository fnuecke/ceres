package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
public final class ArraySerializer implements Serializer {
    public static final ArraySerializer INSTANCE = new ArraySerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class type, final Object value) throws SerializationException {
        visitor.putObject("value", type, value);
    }

    @Override
    public Object deserialize(final DeserializationVisitor visitor, final Class type, @Nullable final Object value) throws SerializationException {
        return visitor.getObject("value", type, value);
    }
}
