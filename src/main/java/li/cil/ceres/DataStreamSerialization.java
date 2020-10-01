package li.cil.ceres;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;

public final class DataStreamSerialization {
    private static final int OBJECT_ARRAY_NULL_VALUE = -1;

    public static void serialize(final DataOutputStream stream, final Object value) throws SerializationException {
        Ceres.getSerializer(value.getClass()).serialize(new Serializer(stream), value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(final DataInputStream stream, final T into) throws SerializationException {
        return deserialize(stream, (Class<T>) into.getClass(), into);
    }

    public static <T> T deserialize(final DataInputStream stream, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(stream), type, into);
    }

    public static ByteBuffer serialize(final Object value) throws SerializationException {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        serialize(new DataOutputStream(data), value);
        return ByteBuffer.wrap(data.toByteArray());
    }

    public static <T> T deserialize(final ByteBuffer data, final Class<T> type, @Nullable final T into) throws SerializationException {
        return deserialize(new DataInputStream(new ByteArrayInputStream(data.array())), type, into);
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
        public void putArray(final String name, final Class<?> type, final Object value) throws SerializationException {
            if (type == boolean[].class) {
                final boolean[] data = (boolean[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final boolean datum : data) {
                        stream.writeBoolean(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == byte[].class) {
                final byte[] data = (byte[]) value;
                try {
                    stream.writeInt(data.length);
                    stream.write(data);
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == char[].class) {
                final char[] data = (char[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final char datum : data) {
                        stream.writeChar(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == short[].class) {
                final short[] data = (short[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final short datum : data) {
                        stream.writeShort(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == int[].class) {
                final int[] data = (int[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final int datum : data) {
                        stream.writeInt(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == long[].class) {
                final long[] data = (long[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final long datum : data) {
                        stream.writeLong(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == float[].class) {
                final float[] data = (float[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final float datum : data) {
                        stream.writeFloat(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == double[].class) {
                final double[] data = (double[]) value;
                try {
                    stream.writeInt(data.length);
                    for (final double datum : data) {
                        stream.writeDouble(datum);
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type.isArray()) {
                final Class<?> componentType = type.getComponentType();
                final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
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
                            throw new SerializationException(String.format("Polymorphism detected in generic array [%s]. This is not supported.", name));
                        }
                        serializer.serialize(new Serializer(componentStream), (Class) componentType, datum);
                        stream.writeInt(componentData.size());
                        stream.write(componentData.toByteArray());
                        componentData.reset();
                    }
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void putEnum(final String name, final Class<Enum<?>> type, final Enum<?> value) throws SerializationException {
            putInt(name, value.ordinal());
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void putObject(final String name, final Class<?> type, final Object value) throws SerializationException {
            if (type == String.class) {
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

        @Override
        public void putNull(final String name, final boolean isNull) throws SerializationException {
            try {
                stream.writeBoolean(isNull);
            } catch (final IOException e) {
                throw new SerializationException(e);
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

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public Object getArray(final String name, final Class<?> type) throws SerializationException {
            if (type == boolean[].class) {
                try {
                    final int length = stream.readInt();
                    final boolean[] data = new boolean[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readBoolean();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == byte[].class) {
                try {
                    final int length = stream.readInt();
                    final byte[] data = new byte[length];
                    //noinspection ResultOfMethodCallIgnored
                    stream.read(data);
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == char[].class) {
                try {
                    final int length = stream.readInt();
                    final char[] data = new char[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readChar();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == short[].class) {
                try {
                    final int length = stream.readInt();
                    final short[] data = new short[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readShort();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == int[].class) {
                try {
                    final int length = stream.readInt();
                    final int[] data = new int[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readInt();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == long[].class) {
                try {
                    final int length = stream.readInt();
                    final long[] data = new long[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readLong();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == float[].class) {
                try {
                    final int length = stream.readInt();
                    final float[] data = new float[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readFloat();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type == double[].class) {
                try {
                    final int length = stream.readInt();
                    final double[] data = new double[length];
                    for (int i = 0; i < length; i++) {
                        data[i] = stream.readDouble();
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else if (type.isArray()) {
                final Class<?> componentType = type.getComponentType();
                final li.cil.ceres.api.Serializer<?> serializer = Ceres.getSerializer(componentType);
                try {
                    final int length = stream.readInt();
                    final Object[] data = (Object[]) Array.newInstance(componentType, length);
                    for (int i = 0; i < length; i++) {
                        final int componentLength = stream.readInt();
                        if (componentLength <= 0) {
                            continue;
                        }
                        final byte[] bytes = new byte[componentLength];
                        //noinspection ResultOfMethodCallIgnored
                        stream.read(bytes);
                        data[i] = serializer.deserialize(new Deserializer(new DataInputStream(new ByteArrayInputStream(bytes))), (Class) componentType, null);
                    }
                    return data;
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public Enum<?> getEnum(final String name, final Class<Enum<?>> type) throws SerializationException {
            return type.getEnumConstants()[getInt(name)];
        }

        @SuppressWarnings("unchecked")
        @Nullable
        @Override
        public <T> T getObject(final String name, final Class<T> type, @Nullable final T into) throws SerializationException {
            if (type == String.class) {
                try {
                    return (T) stream.readUTF();
                } catch (final IOException e) {
                    throw new SerializationException(e);
                }
            } else {
                return Ceres.getSerializer(type).deserialize(this, type, into);
            }
        }

        @Override
        public boolean isNull(final String name) throws SerializationException {
            try {
                return stream.readBoolean();
            } catch (final IOException e) {
                throw new SerializationException(e);
            }
        }
    }
}
