package li.cil.ceres;

import li.cil.ceres.api.*;
import li.cil.ceres.internal.SerializerFactory;
import li.cil.ceres.serializers.ArraySerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Ceres is a simplistic serialization framework.
 * <p>
 * Its main goals are:
 * <ul>
 *     <li>Allow serialization of types from external code: {@link Serializer}.</li>
 *     <li>Make serialization of own code quick and easy: {@link Serialized}.</li>
 *     <li>Exchangeable serialization backends: {@link SerializationVisitor}, {@link DeserializationVisitor}.</li>
 * </ul>
 * Its main limitations are:
 * <ul>
 *     <li>No aliasing support.</li>
 *     <li>No explicit polymorphism support<sup>1)</sup>.</li>
 *     <li>Generated serializers cannot access private inner types.</li>
 * </ul>
 * <p>
 * To add support for types a {@link Serializer} for that type must be implemented and registered by
 * either calling {@link #putSerializer(Class, Serializer)} or annotating it with {@link RegisterSerializer}.
 * <p>
 * Adding support for own types can be eased by marking fields to be serialized with the {@link Serialized}.
 * This will allow automatic generation of a serializer for those types. <em>Important</em>: if a type does
 * not have a default constructor, deserializing this type into fields not already holding a value the
 * type can be deserialized into, an exception will be thrown during deserialization.
 * <p>
 * Using Ceres requires an implementation of a {@link SerializationVisitor} and a {@link DeserializationVisitor}.
 * Ceres comes with an implementation serializing to a binary format accessible via {@link BinarySerialization}.
 * <p>
 * <sup>1)</sup> Ceres does not inherently support polymorphic fields. The serializer chosen for such fields
 * will depend on the field's type, not the value currently assigned to that field. As such, it is required
 * that an explicit serializer for the field type is registered for cases where value type does not match the
 * field type. Such serializers will need to function for all subtypes of their type or write the full type
 * of the serialized value to know the type the serialized data applies to during deserialization.
 */
public final class Ceres {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Class<?>, Serializer<?>> SERIALIZERS = new HashMap<>();
    private static boolean isInitialized = false;

    static {
        initialize();
    }

    public static void initialize() {
        if (isInitialized) {
            return;
        }

        isInitialized = true;

        for (final Class<?> type : new Reflections().getSubTypesOf(Serializer.class)) {
            if (!type.isAnnotationPresent(RegisterSerializer.class)) {
                continue;
            }

            try {
                final ArrayList<Class<?>> supportedTypes = new ArrayList<>();
                Arrays.stream(type.getGenericInterfaces()).forEach(interfaceType -> {
                    final ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                    if (parameterizedType.getRawType() == Serializer.class) {
                        final Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        assert typeArguments.length == 1;
                        supportedTypes.add((Class<?>) typeArguments[0]);
                    }
                });
                if (!supportedTypes.isEmpty()) {
                    final Serializer<?> serializer = (Serializer<?>) type.newInstance();
                    for (final Class<?> supportedType : supportedTypes) {
                        SERIALIZERS.put(supportedType, serializer);
                    }
                }
            } catch (final InstantiationException | IllegalAccessException e) {
                LOGGER.error("Failed instantiating serializer [{}]: {}", type, e);
            }
        }
    }

    /**
     * Returns a serializer for the specified type.
     * <p>
     * If no serializer for this type has been assigned using {@link #putSerializer(Class, Serializer)}
     * a serializer will be generated. A generated serializer will operate on fields marked with the
     * {@link Serialized} annotation, either directly or by annotating the class itself.
     *
     * @param type the type to get a serializer for.
     * @return a {@link Serializer} for the specified type.
     * @throws SerializationException if an exception is raised while generating a serializer.
     */
    public static <T> Serializer<T> getSerializer(final Class<T> type) throws SerializationException {
        return getSerializer(type, true);
    }

    /**
     * Returns a serializer for the specified type.
     * <p>
     * If no serializer for this type has been assigned using {@link #putSerializer(Class, Serializer)}
     * and {@code generateMissing} is {@code true} a serializer will be generated. A generated serializer
     * will operate on fields marked with the {@link Serialized} annotation, either directly or by
     * annotating the class itself. If {@code generateMissing} is {@code false}, {@code null} will be
     * returned.
     *
     * @param type the type to get a serializer for.
     * @return a {@link Serializer} for the specified type.
     * @throws SerializationException if an exception is raised while generating a serializer.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Serializer<T> getSerializer(final Class<T> type, final boolean generateMissing) throws SerializationException {
        synchronized (SERIALIZERS) {
            if (SERIALIZERS.containsKey(type)) {
                return (Serializer<T>) SERIALIZERS.get(type);
            }

            // NB: this is only relevant for root object array serialization. Whenever arrays
            // are part of a to-be-serialized object serializers will directly call the put-
            // and getArray methods on visitors.
            if (type.isArray()) {
                return ArraySerializer.INSTANCE;
            }

            if (generateMissing) {
                final Serializer<T> serializer = SerializerFactory.generateSerializer(type);
                SERIALIZERS.put(type, serializer);
                return serializer;
            }
        }

        return null;
    }

    /**
     * Assigns a serializer to the specified type.
     * <p>
     * If a serializer had been previously assigned to the type it will be replaced.
     * <p>
     * Passing {@code null} as the {@code serializer} value will remove the current serializer for
     * the specified {@code type}. This is mainly intended to be used when writing unit tests.
     *
     * @param type       the type the serializer operates on.
     * @param serializer the serializer to assign to {@code type}.
     */
    public static <T> void putSerializer(final Class<T> type, @Nullable final Serializer<T> serializer) {
        synchronized (SERIALIZERS) {
            if (serializer != null) {
                SERIALIZERS.put(type, serializer);
            } else {
                SERIALIZERS.remove(type);
            }
        }
    }
}
