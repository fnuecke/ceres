package li.cil.ceres;

import li.cil.ceres.api.RegisterSerializer;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serializer;
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

public final class Ceres {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<Class<?>, Serializer<?>> SERIALIZERS = new HashMap<>();

    static {
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

    public static <T> void putSerializer(final Class<T> type, final Serializer<T> serializer) {
        synchronized (SERIALIZERS) {
            SERIALIZERS.put(type, serializer);
        }
    }
}
