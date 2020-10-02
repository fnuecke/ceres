package li.cil.ceres.internal;

import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serialized;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class SerializerUtils {
    static ArrayList<Field> collectFields(final Class<?> type) throws SerializationException {
        return collectFields(type, new HashSet<>());
    }

    private static ArrayList<Field> collectFields(final Class<?> type, final Set<Class<?>> seen) {
        if (!seen.add(type)) {
            throw new SerializationException("Cycle detected in serialization graph. Aliasing is not supported.");
        }

        final boolean serializeFields = type.isAnnotationPresent(Serialized.class);
        final ArrayList<Field> fields = new ArrayList<>();
        for (final Field field : type.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on transient field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }
            if (Modifier.isFinal(field.getModifiers()) && !isSerializable(field.getType(), seen)) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on final field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            if (serializeFields || field.isAnnotationPresent(Serialized.class)) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static boolean isSerializable(final Class<?> type, final Set<Class<?>> seen) {
        if (type.isPrimitive()) return false;
        if (type.isEnum()) return false;
        if (type.isArray()) return true;
        return hasSerializableFields(type, seen);
    }

    private static boolean hasSerializableFields(final Class<?> type, final Set<Class<?>> seen) {
        if (!collectFields(type, seen).isEmpty()) {
            return true;
        }

        final Class<?> parentType = type.getSuperclass();
        if (parentType == null || parentType == Object.class) {
            return false;
        }

        return hasSerializableFields(parentType, seen);
    }
}
