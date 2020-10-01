package li.cil.ceres.api;

import javax.annotation.Nullable;

/**
 * Represents an interface for performing deserialization of values.
 * <p>
 * This is used by a {@link Serializer} to read values from the underlying serialization format
 * encapsulated by this interface.
 * <p>
 * For each {@link DeserializationVisitor} there should exist a compatible {@link SerializationVisitor}.
 * The {@link DeserializationVisitor} can be used to read back data serialized by the {@link SerializationVisitor}.
 * <p>
 * The overloads for Java's primitive types solely exist to avoid boxing.
 * <p>
 * <em>Important</em>: {@link DeserializationVisitor}s are expected to provide custom implementations
 * for deserializing arrays of primitive types. Treating these here allows format-specific handling
 * of such arrays which allows for better efficiency in both performance and storage.
 */
public interface DeserializationVisitor {
    boolean getBoolean(final String name) throws SerializationException;

    byte getByte(final String name) throws SerializationException;

    char getChar(final String name) throws SerializationException;

    short getShort(final String name) throws SerializationException;

    int getInt(final String name) throws SerializationException;

    long getLong(final String name) throws SerializationException;

    float getFloat(final String name) throws SerializationException;

    double getDouble(final String name) throws SerializationException;

    Object getArray(final String name, final Class<?> type) throws SerializationException;

    Enum<?> getEnum(final String name, final Class<Enum<?>> type) throws SerializationException;

    <T> T getObject(final String name, final Class<T> type, @Nullable final T into) throws SerializationException;

    boolean isNull(final String name) throws SerializationException;
}
