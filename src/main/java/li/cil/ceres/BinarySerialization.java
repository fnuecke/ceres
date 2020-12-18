package li.cil.ceres;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides binary serialization to and from {@link DataOutputStream}s/{@link DataInputStream}s and {@link ByteBuffer}s.
 * <p>
 * The {@link SerializationVisitor} and {@link DeserializationVisitor} implemented by this serialization format write
 * a minimal amount of data. In particular, no names are stored, and as such there is no way of knowing if a value to
 * be read exists in the serialized data. As such, this format is not suitable for use-cases where the data structures
 * that are serialized may change over time, as this will make the serialized data unreadable: the data structures
 * define the structure of the serialized data.
 */
public final class BinarySerialization {
    public static <T> void serialize(final DataOutputStream stream, final T value, final Class<T> type) throws SerializationException {
        Ceres.getSerializer(type).serialize(new Serializer(stream), type, value);
    }

    public static <T> ByteBuffer serialize(final T value, final Class<T> type) throws SerializationException {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        serialize(new DataOutputStream(data), value, type);
        return ByteBuffer.wrap(data.toByteArray());
    }

    public static <T> void serialize(final DataOutputStream stream, final T value) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        serialize(stream, value, type);
    }

    public static <T> ByteBuffer serialize(final T value) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) value.getClass();
        return serialize(value, type);
    }

    public static <T> T deserialize(final DataInputStream stream, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(stream), type, into);
    }

    public static <T> T deserialize(final DataInputStream stream, final Class<T> type) throws SerializationException {
        return deserialize(stream, type, null);
    }

    public static <T> T deserialize(final DataInputStream stream, final T into) throws SerializationException {
        @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) into.getClass();
        return deserialize(stream, type, into);
    }

    public static <T> T deserialize(final ByteBuffer data, final Class<T> type, @Nullable final T into) throws SerializationException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(data.array())), type, into);
    }

    public static <T> T deserialize(final ByteBuffer data, final Class<T> type) throws SerializationException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(data.array())), type, null);
    }

    public static <T> T deserialize(final ByteBuffer data, final T into) throws SerializationException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(data.array())), into);
    }

    private static final int OBJECT_ARRAY_NULL_VALUE = -1;
    private static final Map<Class<?>, ArraySerializer> ARRAY_SERIALIZERS;

    static {
        ARRAY_SERIALIZERS = new HashMap<>();
        ARRAY_SERIALIZERS.put(boolean.class, new BooleanArraySerializer());
        ARRAY_SERIALIZERS.put(byte.class, new ByteArraySerializer());
        ARRAY_SERIALIZERS.put(char.class, new CharArraySerializer());
        ARRAY_SERIALIZERS.put(short.class, new ShortArraySerializer());
        ARRAY_SERIALIZERS.put(int.class, new IntArraySerializer());
        ARRAY_SERIALIZERS.put(long.class, new LongArraySerializer());
        ARRAY_SERIALIZERS.put(float.class, new FloatArraySerializer());
        ARRAY_SERIALIZERS.put(double.class, new DoubleArraySerializer());
        ARRAY_SERIALIZERS.put(Enum.class, new EnumArraySerializer());
        ARRAY_SERIALIZERS.put(String.class, new StringArraySerializer());
    }

    private static final class Serializer implements SerializationVisitor {
        private final DataOutputStream stream;

        private Serializer(final DataOutputStream stream) {
            this.stream = stream;
        }

        @Override
        public void putBoolean(final String name, final boolean value) throws SerializationException {
            try {
                stream.writeBoolean(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putByte(final String name, final byte value) throws SerializationException {
            try {
                stream.writeByte(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putChar(final String name, final char value) throws SerializationException {
            try {
                stream.writeChar(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putShort(final String name, final short value) throws SerializationException {
            try {
                stream.writeShort(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putInt(final String name, final int value) throws SerializationException {
            try {
                stream.writeInt(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putLong(final String name, final long value) throws SerializationException {
            try {
                stream.writeLong(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putFloat(final String name, final float value) throws SerializationException {
            try {
                stream.writeFloat(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public void putDouble(final String name, final double value) throws SerializationException {
            try {
                stream.writeDouble(value);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
            if (putIsNull(value)) {
                return;
            }

            if (type.isArray()) {
                putArray(stream, name, type, value);
            } else if (type.isEnum()) {
                putInt(name, ((Enum) value).ordinal());
            } else if (type == String.class) {
                final String data = (String) value;
                try {
                    stream.writeUTF(data);
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else {
                Ceres.getSerializer(type).serialize(this, (Class) type, value);
            }
        }

        @Contract("null -> true")
        private boolean putIsNull(@Nullable final Object value) {
            try {
                final boolean isNull = value == null;
                stream.writeBoolean(isNull);
                return isNull;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @FunctionalInterface
        private interface ArrayComponentSerializer {
            void serialize(DataOutputStream stream, Class<?> type, Object value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void putArray(final DataOutputStream stream, final String name, final Class<?> type, final Object value) {
            final Class<?> componentType = type.getComponentType();

            final ArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
            if (arraySerializer != null) {
                arraySerializer.serialize(stream, value);
            } else {
                final ArrayComponentSerializer componentSerializer;
                if (componentType.isArray()) {
                    componentSerializer = (s, t, v) -> putArray(s, name, t, v);
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    componentSerializer = (s, t, v) -> serializer.serialize(new Serializer(s), (Class) t, v);
                }

                final ByteArrayOutputStream componentData = new ByteArrayOutputStream();
                final DataOutputStream componentStream = new DataOutputStream(componentData);
                final Object[] data = (Object[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final Object datum : data) {
                        if (datum == null) {
                            stream.writeInt(OBJECT_ARRAY_NULL_VALUE);
                            continue;
                        }
                        if (datum.getClass() != componentType) {
                            throw new SerializationException(String.format("Polymorphism detected in array [%s]. This is not supported.", name));
                        }
                        componentSerializer.serialize(componentStream, componentType, datum);
                        stream.writeInt(componentData.size());
                        stream.write(componentData.toByteArray());
                        componentData.reset();
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            }
        }
    }

    private static final class Deserializer implements DeserializationVisitor {
        private final DataInputStream stream;

        private Deserializer(final DataInputStream stream) {
            this.stream = stream;
        }

        @Override
        public boolean getBoolean(final String name) throws SerializationException {
            try {
                return stream.readBoolean();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public byte getByte(final String name) throws SerializationException {
            try {
                return stream.readByte();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public char getChar(final String name) throws SerializationException {
            try {
                return stream.readChar();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public short getShort(final String name) throws SerializationException {
            try {
                return stream.readShort();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public int getInt(final String name) throws SerializationException {
            try {
                return stream.readInt();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public long getLong(final String name) throws SerializationException {
            try {
                return stream.readLong();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public float getFloat(final String name) throws SerializationException {
            try {
                return stream.readFloat();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public double getDouble(final String name) throws SerializationException {
            try {
                return stream.readDouble();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        @Override
        public Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException {
            if (isNull()) {
                return null;
            }

            if (type.isArray()) {
                return getArray(stream, type, into);
            } else if (type.isEnum()) {
                return type.getEnumConstants()[getInt(name)];
            } else if (type == String.class) {
                try {
                    return stream.readUTF();
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else {
                return Ceres.getSerializer(type).deserialize(this, (Class) type, into);
            }
        }

        @FunctionalInterface
        private interface ArrayComponentDeserializer {
            Object deserialize(DataInputStream stream, Class<?> type, @Nullable Object into);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object getArray(final DataInputStream stream, final Class<?> type, @Nullable final Object into) {
            final Class<?> componentType = type.getComponentType();

            final ArraySerializer arraySerializer = ARRAY_SERIALIZERS.get(componentType);
            if (arraySerializer != null) {
                return arraySerializer.deserialize(stream, type, into);
            } else {
                final ArrayComponentDeserializer componentDeserializer;
                if (componentType.isArray()) {
                    componentDeserializer = Deserializer::getArray;
                } else {
                    final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                    componentDeserializer = (s, t, i) -> serializer.deserialize(new Deserializer(s), (Class) t, i);
                }

                try {
                    final int length = stream.readInt();
                    Object[] data = (Object[]) into;
                    if (data == null || data.length != length) {
                        data = (Object[]) Array.newInstance(componentType, length);
                    }

                    for (int i = 0; i < length; i++) {
                        final int componentLength = stream.readInt();
                        if (componentLength <= 0) {
                            continue;
                        }
                        final byte[] bytes = new byte[componentLength];
                        stream.readFully(bytes);
                        data[i] = componentDeserializer.deserialize(new DataInputStream(new ByteArrayInputStream(bytes)), componentType, data[i]);
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            }
        }

        private boolean isNull() throws SerializationException {
            try {
                return stream.readBoolean();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private interface ArraySerializer {
        void serialize(DataOutputStream stream, Object value);

        Object deserialize(DataInputStream stream, final Class<?> type, @Nullable final Object into);
    }

    private static final class BooleanArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final boolean[] data = (boolean[]) value;
            try {
                stream.writeInt(data.length);
                for (final boolean datum : data) {
                    stream.writeBoolean(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                boolean[] data = (boolean[]) into;
                if (data == null || data.length != length) {
                    data = new boolean[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readBoolean();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class ByteArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final byte[] data = (byte[]) value;
            try {
                stream.writeInt(data.length);
                stream.write(data);
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                byte[] data = (byte[]) into;
                if (data == null || data.length != length) {
                    data = new byte[length];
                }

                stream.readFully(data);
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class CharArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final char[] data = (char[]) value;
            try {
                stream.writeInt(data.length);
                for (final char datum : data) {
                    stream.writeChar(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                char[] data = (char[]) into;
                if (data == null || data.length != length) {
                    data = new char[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readChar();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class ShortArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final short[] data = (short[]) value;
            try {
                stream.writeInt(data.length);
                for (final short datum : data) {
                    stream.writeShort(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                short[] data = (short[]) into;
                if (data == null || data.length != length) {
                    data = new short[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readShort();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class IntArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final int[] data = (int[]) value;
            try {
                stream.writeInt(data.length);
                for (final int datum : data) {
                    stream.writeInt(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                int[] data = (int[]) into;
                if (data == null || data.length != length) {
                    data = new int[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readInt();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class LongArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final long[] data = (long[]) value;
            try {
                stream.writeInt(data.length);
                for (final long datum : data) {
                    stream.writeLong(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                long[] data = (long[]) into;
                if (data == null || data.length != length) {
                    data = new long[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readLong();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class FloatArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final float[] data = (float[]) value;
            try {
                stream.writeInt(data.length);
                for (final float datum : data) {
                    stream.writeFloat(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                float[] data = (float[]) into;
                if (data == null || data.length != length) {
                    data = new float[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readFloat();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class DoubleArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final double[] data = (double[]) value;
            try {
                stream.writeInt(data.length);
                for (final double datum : data) {
                    stream.writeDouble(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                double[] data = (double[]) into;
                if (data == null || data.length != length) {
                    data = new double[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readDouble();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static final class EnumArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final Enum[] data = (Enum[]) value;
            try {
                stream.writeInt(data.length);
                for (final Enum datum : data) {
                    stream.writeInt(datum.ordinal());
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            final Class<?> componentType = type.getComponentType();
            final Object[] enumConstants = componentType.getEnumConstants();

            try {
                final int length = stream.readInt();
                Enum[] data = (Enum[]) into;
                if (data == null || data.length != length) {
                    data = (Enum[]) Array.newInstance(componentType, length);
                }

                for (int i = 0; i < length; i++) {
                    data[i] = (Enum) enumConstants[stream.readInt()];
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }

    private static final class StringArraySerializer implements ArraySerializer {
        @Override
        public void serialize(final DataOutputStream stream, final Object value) {
            final String[] data = (String[]) value;
            try {
                stream.writeInt(data.length);
                for (final String datum : data) {
                    stream.writeUTF(datum);
                }
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }

        @Override
        public Object deserialize(final DataInputStream stream, final Class<?> type, final Object into) {
            try {
                final int length = stream.readInt();
                String[] data = (String[]) into;
                if (data == null || data.length != length) {
                    data = new String[length];
                }

                for (int i = 0; i < length; i++) {
                    data[i] = stream.readUTF();
                }
                return data;
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }
}
