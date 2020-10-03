package li.cil.ceres.api;

import javax.annotation.Nullable;

/**
 * Represents an interface for performing deserialization of basic value types.
 * <p>
 * {@link Serializer}s break up more complex types into basic value types and use this interface
 * to read those basic value types from a serialization format defined by the visitor implementation.
 * <p>
 * For each {@link DeserializationVisitor} it is expected there exists a compatible {@link SerializationVisitor}.
 * The {@link DeserializationVisitor} can be used to read back data serialized by the {@link SerializationVisitor}.
 * <p>
 * <em>Important</em>: {@link DeserializationVisitor}s are expected to provide custom implementations
 * for deserializing arrays. Treating these at a low level allows format-specific handling of arrays.
 * This allows for better efficiency in both performance and storage. In other words,
 * {@link #getObject(String, Class, Object)} <em>must not</em> try to obtain a serializer for array
 * types. It must handle arrays directly.
 * <p>
 * {@link SerializationVisitor}-{@link DeserializationVisitor} pairs may decide to handle additional
 * types in {@link #getObject(String, Class, Object)} and {@link SerializationVisitor#putObject(String, Class, Object)}.
 * This is usually done if this is expected to be more efficient than falling back to a more general
 * {@link Serializer} implementation.
 * <p>
 * The presence of the {@code name} parameter in the methods provided by this interface does not
 * imply order-independence. Some implementations may completely ignore the provided names. It is
 * required that serialization and deserialization happen in the same order.
 * <p>
 * The values of the {@code name} parameter may however be used to uniquely identify values in the
 * underlying format by implementations. As such, while callers should not rely on the names being
 * used, as mentioned above, they must provide distinct names for each distinct value to be read.
 * <p>
 * <b>TL;DR</b>
 * <ul>
 *     <li>Implementations <em>must</em> support deserialization of arrays
 *         in {@link #getObject(String, Class, Object)}.</li>
 *     <li>Implementations <em>may</em> support built-in deserialization for additional types
 *         in {@link #getObject(String, Class, Object)}.</li>
 *     <li>Callers <em>must</em> call methods in the same order they were called during serialization.</li>
 *     <li>Callers <em>must not</em> call a method on this interface with a name that was passed to an
 *         earlier call to a method on this interface.</li>
 * </ul>
 */
public interface DeserializationVisitor {
    /**
     * Reads a {@code boolean} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    boolean getBoolean(final String name) throws SerializationException;

    /**
     * Reads a {@code byte} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    byte getByte(final String name) throws SerializationException;

    /**
     * Reads a {@code char} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    char getChar(final String name) throws SerializationException;

    /**
     * Reads a {@code short} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    short getShort(final String name) throws SerializationException;

    /**
     * Reads an {@code int} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    int getInt(final String name) throws SerializationException;

    /**
     * Reads a {@code long} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    long getLong(final String name) throws SerializationException;

    /**
     * Reads a {@code float} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    float getFloat(final String name) throws SerializationException;

    /**
     * Reads a {@code double} value from the underlying serialization format.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    double getDouble(final String name) throws SerializationException;

    /**
     * Reads an {@code Object} value from the underlying serialization format.
     * <p>
     * Implementations <em>must</em> support deserializing arrays.
     * <p>
     * For all other types, implementations <em>may</em> try to obtain a serializer for
     * the specified type and delegate deserialization of the value to it in a suitable
     * fashion (typically by passing a new instance of the implementation type).
     * <p>
     * When delegating to a {@link Serializer}, the exact type passed in {@code type}
     * must be used when calling {@link li.cil.ceres.Ceres#getSerializer(Class)}. The
     * passed {@code type} may be a supertype of {@code value} when currently deserializing
     * a superclass of {@code value}.
     *
     * @param name the name passed with the value during serialization.
     * @return the read value.
     */
    @Nullable
    Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException;

    /**
     * Checks if there exists a value for the specified name in the underlying format.
     * <p>
     * Implementations which support deserializing into data structures that may have structurally changed
     * since serialization may provide this to avoid overwriting default values (in particular for
     * primitive types where no default/{@code into} value is provided).
     *
     * @param name the name of the value to check for.
     * @return {@code true} if a value with the specified name exists in the serialized data; {@code false} otherwise.
     */
    default boolean exists(final String name) throws SerializationException {
        return true;
    }
}
