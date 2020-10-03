package li.cil.ceres.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark fields as serialized for generated serializers.
 * <p>
 * This annotation may also be used on classes, in which case all eligible fields in the class
 * are marked as serialized. Fields ignored by this are fields marked {@code transient} and
 * {@code final} fields holding immutable values.
 * <p>
 * This annotation is used when a serializer is requested via {@link li.cil.ceres.Ceres#getSerializer(Class)}
 * and no such serializer has been registered before. Ceres will attempt to generate a serializer
 * by scanning the specified type for serializable fields.
 * <p>
 * Adding this annotation to a {@code transient} field or a {@code final} field holding an immutable
 * value will lead to a {@link SerializationException} during serializer generation.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Serialized {
}
