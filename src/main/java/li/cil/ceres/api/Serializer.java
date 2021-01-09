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
 * <p>
 * Serializers must be registered by calling {@link li.cil.ceres.Ceres#putSerializer(Class, Serializer)}
 * before they will be used by the serialization system.
 *
 * @param <T> the type the serializer provides serialization for.
 */
public interface Serializer<T> {
    /**
     * Serialize the specified value using the specified visitor.
     *
     * @param visitor the implementation to write basic values to.
     * @param type    the type to serialize.
     *                This matches {@link T} unless implementing a raw serializer.
     *                This may be a superclass of {@code value}.
     * @param value   the value to serialize.
     * @throws SerializationException if an exception is raised by the {@link SerializationVisitor}.
     */
    void serialize(final SerializationVisitor visitor, final Class<T> type, final Object value) throws SerializationException;

    /**
     * Deserializes a value using the specified visitor.
     * <p>
     * When {@code value} is not {@code null}, the serializer <em>must</em> deserialize into
     * the specified instance unless this is impossible (e.g. differing array lengths). In
     * that case the implementation <em>may</em> return a new instance but <em>should</em>
     * expect the returned value to be ignored.
     *
     * @param visitor the implementation to read basic values from.
     * @param type    the type to deserialize.
     *                This matches {@link T} unless implementing a raw serializer.
     *                This may be a superclass of {@code value}.
     * @param value   the value to deserialize into if not {@code null}.
     * @return the deserialized value; if {@code value} was not {@code null} generally {@code value}.
     * @throws SerializationException if an exception is raised by the {@link DeserializationVisitor}.
     */
    T deserialize(final DeserializationVisitor visitor, final Class<T> type, @Nullable final Object value) throws SerializationException;
}
