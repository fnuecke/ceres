package li.cil.ceres.internal;

import li.cil.ceres.api.*;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
final class ReflectionSerializer implements Serializer {
    static final ReflectionSerializer INSTANCE = new ReflectionSerializer();

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(final SerializationVisitor visitor, final Class type, final Object value) throws SerializationException {
        for (final Field field : collectFields(type)) {
            try {
                final Class fieldType = field.getType();
                if (fieldType == boolean.class) {
                    visitor.putBoolean(field.getName(), field.getBoolean(value));
                } else if (fieldType == byte.class) {
                    visitor.putByte(field.getName(), field.getByte(value));
                } else if (fieldType == char.class) {
                    visitor.putChar(field.getName(), field.getChar(value));
                } else if (fieldType == short.class) {
                    visitor.putShort(field.getName(), field.getShort(value));
                } else if (fieldType == int.class) {
                    visitor.putInt(field.getName(), field.getInt(value));
                } else if (fieldType == long.class) {
                    visitor.putLong(field.getName(), field.getLong(value));
                } else if (fieldType == float.class) {
                    visitor.putFloat(field.getName(), field.getFloat(value));
                } else if (fieldType == double.class) {
                    visitor.putDouble(field.getName(), field.getDouble(value));
                } else {
                    final Object fieldValue = field.get(value);
                    final boolean isNull = fieldValue == null;
                    visitor.putNull(field.getName(), isNull);
                    if (isNull) {
                        continue;
                    }

                    if (fieldType.isArray()) {
                        visitor.putArray(field.getName(), fieldType, fieldValue);
                    } else if (fieldType.isEnum()) {
                        visitor.putEnum(field.getName(), fieldType, (Enum) fieldValue);
                    } else {
                        if (fieldValue.getClass() != fieldType) {
                            throw new SerializationException(String.format("Value type [%s] does not match field type [%s] in field [%s.%s]. Polymorphism is not supported.", value.getClass().getName(), fieldType.getName(), type.getName(), field.getName()));
                        }
                        visitor.putObject(field.getName(), fieldType, fieldValue);
                    }
                }
            } catch (final Throwable e) {
                throw new SerializationException(String.format("Failed serializing field [%s.%s]", type.getName(), field.getName()), e);
            }
        }

        final Class parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            visitor.putObject("<super>", parentType, value);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(final DeserializationVisitor visitor, final Class type, @Nullable Object value) throws SerializationException {
        if (value == null) {
            try {
                final Constructor constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                value = constructor.newInstance();
            } catch (final Throwable e) {
                throw new SerializationException(String.format("Failed instantiating type [%s]", type.getName()), e);
            }
        }

        for (final Field field : collectFields(type)) {
            try {
                final Class fieldType = field.getType();
                if (fieldType == boolean.class) {
                    field.setBoolean(value, visitor.getBoolean(field.getName()));
                } else if (fieldType == byte.class) {
                    field.setByte(value, visitor.getByte(field.getName()));
                } else if (fieldType == char.class) {
                    field.setChar(value, visitor.getChar(field.getName()));
                } else if (fieldType == short.class) {
                    field.setShort(value, visitor.getShort(field.getName()));
                } else if (fieldType == int.class) {
                    field.setInt(value, visitor.getInt(field.getName()));
                } else if (fieldType == long.class) {
                    field.setLong(value, visitor.getLong(field.getName()));
                } else if (fieldType == float.class) {
                    field.setFloat(value, visitor.getFloat(field.getName()));
                } else if (fieldType == double.class) {
                    field.setDouble(value, visitor.getDouble(field.getName()));
                } else {
                    if (visitor.isNull(field.getName())) {
                        field.set(value, null);
                    } else if (fieldType.isArray()) {
                        field.set(value, visitor.getArray(field.getName(), fieldType));
                    } else if (fieldType.isEnum()) {
                        field.set(value, visitor.getEnum(field.getName(), fieldType));
                    } else {
                        field.set(value, visitor.getObject(field.getName(), fieldType, field.get(value)));
                    }
                }
            } catch (final Throwable e) {
                throw new SerializationException(String.format("Failed deserializing field [%s.%s]", type.getName(), field.getName()), e);
            }
        }

        final Class parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            visitor.getObject("<super>", parentType, value);
        }

        return value;
    }

    private static ArrayList<Field> collectFields(final Class type) throws SerializationException {
        final boolean serializeFields = type.isAnnotationPresent(Serialized.class);
        final ArrayList<Field> fields = new ArrayList<>();
        for (final Field field : type.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on final field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            field.setAccessible(true);
            if (serializeFields || field.isAnnotationPresent(Serialized.class)) {
                fields.add(field);
            }
        }
        return fields;
    }
}
