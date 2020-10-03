package li.cil.ceres.api;

import javax.annotation.Nullable;

/**
 * Represents an interface for performing serialization of basic value types.
 * <p>
 * {@link Serializer}s break up more complex types into basic value types and use this interface
 * to write those basic value types to a serialization format defined by the visitor implementation.
 * <p>
 * For each {@link SerializationVisitor} it is expected there exists a compatible {@link DeserializationVisitor}.
 * The {@link DeserializationVisitor} can be used to read back data serialized by the {@link SerializationVisitor}.
 * <p>
 * <em>Important</em>: {@link SerializationVisitor}s are expected to provide custom implementations
 * for serializing arrays. Treating these at a low level allows format-specific handling of arrays.
 * This allows for better efficiency in both performance and storage. In other words,
 * {@link #putObject(String, Class, Object)} <em>must not</em> try to obtain a serializer for array
 * types. It must handle arrays directly.
 * <p>
 * {@link SerializationVisitor}-{@link DeserializationVisitor} pairs may decide to handle additional
 * types in {@link #putObject(String, Class, Object)} and {@link DeserializationVisitor#getObject(String, Class, Object)}.
 * This is usually done if this is expected to be more efficient than falling back to a more general
 * {@link Serializer} implementation.
 * <p>
 * The presence of the {@code name} parameter in the methods provided by this interface does not
 * imply order-independence. Some implementations may completely ignore the provided names. It is
 * required that serialization and deserialization happen in the same order.
 * <p>
 * The values of the {@code name} parameter may however be used to uniquely identify values in the
 * underlying format by implementations. As such, while callers should not rely on the names being
 * used, as mentioned above, they must provide distinct names for each distinct value to be written.
 * <p>
 * <b>TL;DR</b>
 * <ul>
 *     <li>Implementations <em>must</em> support serialization of arrays
 *         in {@link #putObject(String, Class, Object)}.</li>
 *     <li>Implementations <em>may</em> support built-in serialization for additional types
 *         in {@link #putObject(String, Class, Object)}.</li>
 *     <li>Callers <em>must</em> call methods in the same order they will be called during deserialization.</li>
 *     <li>Callers <em>must not</em> call a method on this interface with a name that was passed to an
 *         earlier call to a method on this interface.</li>
 * </ul>
 */
public interface SerializationVisitor {
    /**
     * Writes a {@code boolean} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putBoolean(final String name, final boolean value) throws SerializationException;

    /**
     * Writes a {@code byte} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putByte(final String name, final byte value) throws SerializationException;

    /**
     * Writes a {@code char} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putChar(final String name, final char value) throws SerializationException;

    /**
     * Writes a {@code short} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putShort(final String name, final short value) throws SerializationException;

    /**
     * Writes an {@code int} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putInt(final String name, final int value) throws SerializationException;

    /**
     * Writes a {@code long} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putLong(final String name, final long value) throws SerializationException;

    /**
     * Writes a {@code float} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putFloat(final String name, final float value) throws SerializationException;

    /**
     * Writes a {@code double} value to the underlying serialization format.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putDouble(final String name, final double value) throws SerializationException;

    /**
     * Writes an {@code Object} value to the underlying serialization format.
     * <p>
     * Implementations <em>must</em> support serializing arrays.
     * <p>
     * For all other types, implementations <em>may</em> try to obtain a serializer for
     * the specified type and delegate serialization of the value to it in a suitable
     * fashion (typically by passing a new instance of the implementation type).
     * <p>
     * When delegating to a {@link Serializer}, the exact type passed in {@code type}
     * must be used when calling {@link li.cil.ceres.Ceres#getSerializer(Class)}. The
     * passed {@code type} may be a supertype of {@code value} when currently serializing
     * a superclass of {@code value}.
     *
     * @param name  the name identifying the value during deserialization.
     * @param value the value to write.
     */
    void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException;
}
