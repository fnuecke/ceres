package li.cil.ceres.api;

import javax.annotation.Nullable;

/**
 * A serializer for type {@link T}.
 * <p>
 * Serializers are responsible for writing a representation of an instance of the object type
 * they support to a {@link SerializationVisitor} in a way such that they can later reconstruct
 * a passed instance by reading from a {@link DeserializationVisitor}.
 * <p>
 * Explicit implementations of types are necessary when the definition of the type cannot be
 * changed (external code). This allows serializing such instances without using the annotations
 * used to define a serializer implicitly (e.g. {@link Serialized}).
 *
 * @param <T> the type the serializer provides serialization for.
 */
public interface Serializer<T> {
    default void serialize(final SerializationVisitor visitor, final Object value) throws SerializationException {
        serialize(visitor, value.getClass(), value);
    }

    void serialize(final SerializationVisitor visitor, final Class<?> type, final Object value) throws SerializationException;

    T deserialize(final DeserializationVisitor visitor, final Class<T> type, @Nullable final Object value) throws SerializationException;
}
