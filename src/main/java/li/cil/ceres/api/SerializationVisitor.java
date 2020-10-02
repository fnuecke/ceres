package li.cil.ceres.api;

import javax.annotation.Nullable;

/**
 * Represents an interface for performing serialization of values.
 * <p>
 * This is used by a {@link Serializer} to write values extracted from a to-be-serialized
 * object to the underlying serialization format encapsulated by this interface.
 * <p>
 * For each {@link SerializationVisitor} there should exist a compatible {@link DeserializationVisitor}.
 * The {@link DeserializationVisitor} can be used to read back data serialized by the {@link SerializationVisitor}.
 * <p>
 * The overloads for Java's primitive types solely exist to avoid boxing.
 * <p>
 * <em>Important</em>: {@link SerializationVisitor}s are expected to provide custom implementations
 * for serializing arrays of primitive types. Treating these here allows format-specific handling
 * of such arrays which allows for better efficiency in both performance and storage.
 */
public interface SerializationVisitor {
    void putBoolean(final String name, final boolean value) throws SerializationException;

    void putByte(final String name, final byte value) throws SerializationException;

    void putChar(final String name, final char value) throws SerializationException;

    void putShort(final String name, final short value) throws SerializationException;

    void putInt(final String name, final int value) throws SerializationException;

    void putLong(final String name, final long value) throws SerializationException;

    void putFloat(final String name, final float value) throws SerializationException;

    void putDouble(final String name, final double value) throws SerializationException;

    void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException;
}
