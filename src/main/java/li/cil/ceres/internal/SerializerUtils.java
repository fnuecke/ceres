package li.cil.ceres.internal;

import li.cil.ceres.Ceres;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serialized;
import li.cil.ceres.api.Serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

final class SerializerUtils {
    static ArrayList<Field> collectSerializableFields(final Class<?> type) throws SerializationException {
        return collectSerializableFields(type, new ArrayList<>());
    }

    private static ArrayList<Field> collectSerializableFields(final Class<?> type, final ArrayList<Class<?>> seenTypes) {
        final boolean serializeFields = type.isAnnotationPresent(Serialized.class);
        final ArrayList<Field> fields = new ArrayList<>();
        for (final Field field : type.getDeclaredFields()) {
            // We do not serialize synthetic fields (e.g. reference to parent class for non-static inner classes).
            if (field.isSynthetic()) {
                continue;
            }

            // We do not serialize static fields.
            if (Modifier.isStatic(field.getModifiers())) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on static field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            // We do not serialize transient fields.
            if (Modifier.isTransient(field.getModifiers())) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on transient field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            // We do not serialize final fields holding immutable values.
            // We *do* serialize final fields holding values that we can deserialize into.
            if (Modifier.isFinal(field.getModifiers()) && isImmutable(field.getType(), concat(seenTypes, type))) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on immutable field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            // We only serialize fields if either the whole class was marked for serialization
            // or the field itself was marked for serialization.
            if (serializeFields || field.isAnnotationPresent(Serialized.class)) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static boolean isImmutable(final Class<?> type, final ArrayList<Class<?>> seenTypes) {
        if (type.isPrimitive()) return true; // Primitives are immutable by definition.
        if (type.isEnum()) return true; // Enum values are immutable by definition.
        if (type.isArray()) return false; // Arrays are mutable by definition.

        final Serializer<?> serializer = Ceres.getSerializer(type, false);
        if (serializer != null && (!(serializer instanceof GeneratedSerializer) || ((GeneratedSerializer) serializer).hasSerializedFields()))
            return false; // If we have a serializer we can serialize into this type.

        return !hasSerializableFields(type, seenTypes);
    }

    private static boolean hasSerializableFields(final Class<?> type, final ArrayList<Class<?>> seenTypes) {
        // If we have a cycle that means this path will only ever lead to final fields
        // holding immutable values or null. Terminate this path.
        if (seenTypes.contains(type)) {
            return false;
        }

        // Check for serializable fields on this exact type.
        if (!collectSerializableFields(type, seenTypes).isEmpty()) {
            return true;
        }

        // If we cannot ascend the type hierarchy any further that terminate this path.
        final Class<?> parentType = type.getSuperclass();
        if (parentType == null || parentType == Object.class) {
            return false;
        }

        // Keep climbing the hierarchy.
        return hasSerializableFields(parentType, seenTypes);
    }

    private static <T> ArrayList<T> concat(final ArrayList<T> list, final T value) {
        final ArrayList<T> newList = new ArrayList<>(list);
        newList.add(value);
        return newList;
    }
}
