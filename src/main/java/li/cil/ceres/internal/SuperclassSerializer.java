package li.cil.ceres.internal;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
final class SuperclassSerializer implements Serializer {
    public static final SuperclassSerializer INSTANCE = new SuperclassSerializer();

    @Override
    public void serialize(final SerializationVisitor visitor, final Class type, final Object value) throws SerializationException {
        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            visitor.putObject("<super>", parentType, value);
        }
    }

    @Override
    public Object deserialize(final DeserializationVisitor visitor, final Class type, @Nullable final Object value) throws SerializationException {
        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            return visitor.getObject("<super>", parentType, value);
        } else {
            return value;
        }
    }
}
