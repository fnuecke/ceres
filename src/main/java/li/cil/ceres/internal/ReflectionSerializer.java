package li.cil.ceres.internal;

import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
final class ReflectionSerializer implements Serializer, GeneratedSerializer {
    private final ArrayList<Field> fields;

    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> generateSerializer(final Class<T> type) throws SerializationException {
        if (type.isInterface()) {
            throw new SerializationException(String.format("Cannot generate serializer for interface [%s].", type));
        }

        final ArrayList<Field> fields = SerializerUtils.collectSerializableFields(type);
        for (final Field field : fields) {
            field.setAccessible(true);
        }

        return new ReflectionSerializer(fields);
    }

    private ReflectionSerializer(final ArrayList<Field> fields) {
        this.fields = fields;
    }

    @Override
    public boolean hasSerializedFields() {
        return !fields.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void serialize(final SerializationVisitor visitor, final Class type, final Object value) throws SerializationException {
        for (final Field field : fields) {
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
                    if (fieldValue != null && fieldValue.getClass() != fieldType) {
                        final Serializer serializer = Ceres.getSerializer(fieldType, false);
                        if (serializer == null || serializer instanceof GeneratedSerializer) {
                            throw new SerializationException(String.format("Value type [%s] does not match field type in field [%s.%s] and no explicit serializer has been registered for field type [%s]. Polymorphism is not supported when using generated serializers.", value.getClass().getName(), type.getName(), field.getName(), fieldType.getName()));
                        }
                    }
                    visitor.putObject(field.getName(), fieldType, fieldValue);
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
            if (Modifier.isAbstract(type.getModifiers())) {
                throw new SerializationException(String.format("Cannot create new instance of abstract type [%s].", type));
            } else {
                try {
                    final Constructor constructor = type.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    value = constructor.newInstance();
                } catch (final NoSuchMethodException e) {
                    throw new SerializationException(String.format("Cannot create new instance of type without a default constructor [%s].", type));
                } catch (final Throwable e) {
                    throw new SerializationException(String.format("Failed instantiating type [%s]", type.getName()), e);
                }
            }
        }

        for (final Field field : fields) {
            try {
                if (visitor.exists(field.getName())) {
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
                    } else if (Modifier.isFinal(field.getModifiers())) {
                        // Must deserialize into existing final field references, we never overwrite final field values.
                        // This means there is the weird edge-case where the length of a serialized array may
                        // differ from the currently assigned array. In that case the serialized value silently
                        // get ignores. I'll probably kick myself for this in the future.
                        visitor.getObject(field.getName(), fieldType, field.get(value));
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
}
