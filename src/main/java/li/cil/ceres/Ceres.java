package li.cil.ceres;

import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serializer;
import li.cil.ceres.internal.SerializerFactory;
import li.cil.ceres.serializers.ArraySerializer;
import li.cil.ceres.serializers.UUIDSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Ceres {
    private static final Map<Class<?>, Serializer<?>> SERIALIZERS = new HashMap<>();

    static {
        putSerializer(UUID.class, UUIDSerializer.INSTANCE);
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
