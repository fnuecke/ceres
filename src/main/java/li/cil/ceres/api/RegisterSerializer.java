package li.cil.ceres.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Implementations of {@link Serializer} annotated with this annotation will get automatically
 * registered with Ceres. This is equivalent to calling {@link li.cil.ceres.Ceres#putSerializer(Class, Serializer)}
 * with an instance of the annotated type before performing a serialization or deserialization for the first time.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSerializer {
}
