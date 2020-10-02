package li.cil.ceres.internal;

import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serializer;

public final class SerializerFactory {
    private static final boolean USE_GENERATED_CLASSES = !Boolean.getBoolean("li.cil.ceres.disableCodeGen");

    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> generateSerializer(final Class<T> type) throws SerializationException {
        if (USE_GENERATED_CLASSES) {
            return SerializerCompiler.generateSerializer(type);
        } else {
            return new ReflectionSerializer(type);
        }
    }
}
