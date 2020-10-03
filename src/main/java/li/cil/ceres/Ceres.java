package li.cil.ceres;

import li.cil.ceres.api.*;
import li.cil.ceres.internal.SerializerFactory;
import li.cil.ceres.serializers.ArraySerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

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
 *     <li>Easy to reason about: no aliasing support, no polymorphism support.</li>
 * </ul>
 * <p>
 * To add support for types a {@link Serializer} for that type must be implemented and registered by
 * either calling {@link #putSerializer(Class, Serializer)} or annotating it with {@link RegisterSerializer}.
 * <p>
 * Adding support for own types can be eased by marking fields to be serialized with the {@link Serialized}
 * annotation and providing a default constructor. This will allow automatic generation of a serializer for
 * those types.
 * <p>
 * Using Ceres requires an implementation of a {@link SerializationVisitor} and a {@link DeserializationVisitor}.
 * Ceres comes with an implementation serializing to a binary format accessible via {@link BinarySerialization}.
 */
public final class Ceres {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SERIALIZER_PACKAGE_LOOKUP_PREFIX = System.getProperty("li.cil.ceres.searchedPackagesPrefix", "li.cil");

    private static final Map<Class<?>, Serializer<?>> SERIALIZERS = new HashMap<>();

    static {
        for (final Class<?> type : new Reflections(SERIALIZER_PACKAGE_LOOKUP_PREFIX).getSubTypesOf(Serializer.class)) {
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
    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> getSerializer(final Class<T> type) throws SerializationException {
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

            final Serializer<T> serializer = SerializerFactory.generateSerializer(type);
            SERIALIZERS.put(type, serializer);
            return serializer;
        }
    }

    /**
     * Checks if a serializer for the specified type exists without creating one if it does not.
     *
     * @param type the type to check for.
     * @return {@code true} if a serializer for this type is known; {@code false} otherwise.
     */
    public static <T> boolean hasSerializer(final Class<T> type) {
        synchronized (SERIALIZERS) {
            return SERIALIZERS.containsKey(type) && !SerializerFactory.isSuperclassSerializer(SERIALIZERS.get(type));
        }
    }

    /**
     * Assigns a serializer to the specified type.
     * <p>
     * If a serializer had been previously assigned to the type it will be replaced.
     *
     * @param type       the type the serializer operates on.
     * @param serializer the serializer to assign to {@code type}.
     */
    public static <T> void putSerializer(final Class<T> type, final Serializer<T> serializer) {
        synchronized (SERIALIZERS) {
            SERIALIZERS.put(type, serializer);
        }
    }
}
