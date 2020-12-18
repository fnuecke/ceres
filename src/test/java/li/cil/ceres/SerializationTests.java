package li.cil.ceres;

import li.cil.ceres.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class SerializationTests {
    @AfterEach
    public void unregisterTestSerializers() {
        Ceres.putSerializer(PolymorphicFieldType.class, null);
    }

    @Test
    public void testFlat() {
        final Flat value = new Flat();

        final UUID uuid = UUID.randomUUID();
        value.byteValue = 123;
        value.shortValue = 234;
        value.intValue = 456;
        value.longValue = 567;
        value.floatValue = 678.9f;
        value.doubleValue = 789.0;
        value.byteArrayValue = new byte[]{1, 2, 3};
        value.intArrayValue = new int[]{4, 5, 6};
        value.longArrayValue = new long[]{7, 8, 9};
        value.stringValue = "test string";
        value.uuidValue = uuid;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        Flat deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, Flat.class, new Flat()));

        assertEquals(value.byteValue, deserialized.byteValue);
        assertEquals(value.shortValue, deserialized.shortValue);
        assertEquals(value.intValue, deserialized.intValue);
        assertEquals(value.longValue, deserialized.longValue);
        assertEquals(value.floatValue, deserialized.floatValue);
        assertEquals(value.doubleValue, deserialized.doubleValue);
        assertArrayEquals(value.byteArrayValue, deserialized.byteArrayValue);
        assertArrayEquals(value.intArrayValue, deserialized.intArrayValue);
        assertArrayEquals(value.longArrayValue, deserialized.longArrayValue);
        assertEquals(value.stringValue, deserialized.stringValue);
        assertEquals(value.uuidValue, deserialized.uuidValue);

        deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, Flat.class));

        assertEquals(value.byteValue, deserialized.byteValue);
        assertEquals(value.shortValue, deserialized.shortValue);
        assertEquals(value.intValue, deserialized.intValue);
        assertEquals(value.longValue, deserialized.longValue);
        assertEquals(value.floatValue, deserialized.floatValue);
        assertEquals(value.doubleValue, deserialized.doubleValue);
        assertArrayEquals(value.byteArrayValue, deserialized.byteArrayValue);
        assertArrayEquals(value.intArrayValue, deserialized.intArrayValue);
        assertArrayEquals(value.longArrayValue, deserialized.longArrayValue);
        assertEquals(value.stringValue, deserialized.stringValue);
        assertEquals(value.uuidValue, deserialized.uuidValue);
    }

    @Test
    public void testFlatFields() {
        final FlatFields value = new FlatFields();
        value.value1 = 123;
        value.value2 = 234;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final FlatFields deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, FlatFields.class));

        assertEquals(value.value1, deserialized.value1);
        assertEquals(0, deserialized.value2);
    }

    @Test
    public void testModifiers() {
        final WithModifiers value = new WithModifiers();
        value.nonTransientInt = 123;
        value.transientInt = 234;
        value.finalIntArray[0] = 23;
        value.finalIntArray[1] = 64;
        value.finalIntArray[2] = 420;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final WithModifiers deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, WithModifiers.class));

        assertEquals(value.nonTransientInt, deserialized.nonTransientInt);
        assertEquals(0, deserialized.transientInt);
        assertArrayEquals(value.finalIntArray, deserialized.finalIntArray);
    }

    @Test
    public void testFinal() {
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(new SerializeFinalPrimitive()));
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(new SerializeFinalEnum()));
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(new SerializeFinalImmutableObject()));
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(new SerializedStaticFields()));
    }

    @Test
    public void testRecursive() {
        final Recursive root = new Recursive();
        root.value = 123;
        root.child = new Recursive();
        root.child.value = 234;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(root));

        final Recursive deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, Recursive.class));

        assertEquals(root.value, deserialized.value);
        assertNotNull(deserialized.child);
        assertEquals(root.child.value, deserialized.child.value);
    }

    @Test
    public void testHierarchy() {
        final Subclass value = new Subclass();
        value.val = 123;
        value.sup1 = 234;
        value.sup2 = 345;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final Subclass deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, Subclass.class));

        assertEquals(value.val, deserialized.val);
        assertEquals(0, deserialized.sup1);
        assertEquals(value.sup2, deserialized.sup2);
    }

    @Test
    public void testPrimitiveArray() {
        final byte[] value = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final byte[] deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, byte[].class));

        assertArrayEquals(value, deserialized);
    }

    @Test
    public void testObjectArray() {
        final Flat[] value = new Flat[2];
        value[1] = new Flat();
        value[1].byteValue = 23;
        value[1].intValue = 23;
        value[1].stringValue = "a test";

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final Flat[] deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, Flat[].class));

        assertArrayEquals(value, deserialized);
    }

    @Test
    public void testEnums() {
        final WithEnum value = new WithEnum();
        value.value = WithEnum.TestEnum.TWO;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final WithEnum deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, WithEnum.class));

        assertEquals(value.value, deserialized.value);
    }

    @Test
    public void testIgnoreStaticFields() {
        final IgnoreStaticFields value = new IgnoreStaticFields();

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        IgnoreStaticFields.s = 654;

        assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, IgnoreStaticFields.class));

        assertEquals(654, IgnoreStaticFields.s);
    }

    @Test
    public void testCustomSerializer() {
        final MultipleCustomSerializer value = new MultipleCustomSerializer();
        value.a = new Custom();
        value.a.x = 22;
        value.c = new Custom();
        value.c.x = 43;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        final MultipleCustomSerializer deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, MultipleCustomSerializer.class));

        assertNotNull(deserialized.a);
        assertEquals(value.a.x * 2 + 1, deserialized.a.x);
        assertNull(deserialized.b);
        assertNotNull(deserialized.c);
        assertEquals(value.c.x * 2 + 1, deserialized.c.x);
    }

    @Test
    public void testMutableIndirect() {
        final MutableIndirectRoot value = new MutableIndirectRoot();
        assertDoesNotThrow(() -> BinarySerialization.serialize(value));
    }

    @Test
    public void testImmutableIndirect() {
        final ImmutableIndirectRoot value = new ImmutableIndirectRoot();
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(value));
    }

    @Test
    public void testPolymorphicFieldNoSerializer() {
        final PolymorphicFieldHolder value = new PolymorphicFieldHolder();

        // Serializing null does not throw because no serializer is looked up.
        assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        value.value = new PolymorphicFieldClassMul2();
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(value));

        value.value = new PolymorphicFieldClassAdd1();
        assertThrows(SerializationException.class, () -> BinarySerialization.serialize(value));
    }

    @Test
    public void testPolymorphicFieldYesSerializer() {
        Ceres.putSerializer(PolymorphicFieldType.class, PolymorphicFieldTypeSerializer.INSTANCE);

        final PolymorphicFieldHolder value = new PolymorphicFieldHolder();

        value.value = new PolymorphicFieldClassAdd1();
        value.value.x = 2;

        final ByteBuffer serialized = assertDoesNotThrow(() -> BinarySerialization.serialize(value));

        // New instance of polymorphic field value.
        PolymorphicFieldHolder deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, PolymorphicFieldHolder.class));
        assertEquals(PolymorphicFieldClassAdd1.class, deserialized.value.getClass());

        // New instance of polymorphic field value in existing owner instance.
        value.value = new PolymorphicFieldClassMul2();
        deserialized = assertDoesNotThrow(() -> BinarySerialization.deserialize(serialized, PolymorphicFieldHolder.class, value));
        assertEquals(PolymorphicFieldClassAdd1.class, deserialized.value.getClass());
    }

    @Test
    public void testStringArray() {
        final ByteBuffer serialized = BinarySerialization.serialize(new StringArrayTest());

        final StringArrayTest deserialized = BinarySerialization.deserialize(serialized, StringArrayTest.class);

        assertEquals(deserialized.data[0], "a");
        assertEquals(deserialized.data[1], "b");
        assertEquals(deserialized.data[2], "c");
    }

    @Test
    public void testMultidimensionalArrays() {
        final MultiDimArray value = new MultiDimArray();
        value.array[0][1] = 42;
        value.array[1][1] = 23;

        final ByteBuffer serialized = BinarySerialization.serialize(value);
        final MultiDimArray deserialized = BinarySerialization.deserialize(serialized, MultiDimArray.class);

        assertArrayEquals(value.array[0], deserialized.array[0]);
        assertArrayEquals(value.array[1], deserialized.array[1]);
    }

    public static final class StringArrayTest {
        public String[] data = {"a", "b", "c"};
    }

    @Serialized
    public static final class Flat {
        private byte byteValue;
        private short shortValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
        private byte[] byteArrayValue;
        private int[] intArrayValue;
        private long[] longArrayValue;
        private String stringValue;
        private UUID uuidValue;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Flat flat = (Flat) o;
            return byteValue == flat.byteValue &&
                   shortValue == flat.shortValue &&
                   intValue == flat.intValue &&
                   longValue == flat.longValue &&
                   Float.compare(flat.floatValue, floatValue) == 0 &&
                   Double.compare(flat.doubleValue, doubleValue) == 0 &&
                   Arrays.equals(byteArrayValue, flat.byteArrayValue) &&
                   Arrays.equals(intArrayValue, flat.intArrayValue) &&
                   Arrays.equals(longArrayValue, flat.longArrayValue) &&
                   Objects.equals(stringValue, flat.stringValue) &&
                   Objects.equals(uuidValue, flat.uuidValue);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(byteValue, shortValue, intValue, longValue, floatValue, doubleValue, stringValue, uuidValue);
            result = 31 * result + Arrays.hashCode(byteArrayValue);
            result = 31 * result + Arrays.hashCode(intArrayValue);
            result = 31 * result + Arrays.hashCode(longArrayValue);
            return result;
        }
    }

    public static final class FlatFields {
        @Serialized private int value1;
        private int value2;
    }

    @Serialized
    public static final class WithModifiers {
        private int nonTransientInt;
        private transient int transientInt;
        private final int finalInt = 678;
        private final int[] finalIntArray = new int[3];
    }

    public static final class SerializeFinalPrimitive {
        @Serialized private final int finalInt = 23;
    }

    public static final class SerializeFinalEnum {
        public enum TestEnum {
            A, B
        }

        @Serialized private final TestEnum finalEnum = TestEnum.B;
    }

    public static final class SerializeFinalImmutableObject {
        @Serialized private final ImmutableClass finalValue = new ImmutableClass();

        @Serialized
        public static final class ImmutableClass {
            private final int value = 123;
        }
    }

    public static final class SerializedStaticFields {
        @Serialized public static int s = 123;
    }

    @Serialized
    public static final class Recursive {
        private int value;
        private Recursive child;
    }

    @Serialized
    public static class Subclass extends NonSerializedSuperclass {
        int val;
    }

    public static class NonSerializedSuperclass extends SerializedSuperclass {
        int sup1;
    }

    @Serialized
    public static class SerializedSuperclass {
        int sup2;
    }

    @Serialized
    public static final class WithEnum {
        public enum TestEnum {
            ONE,
            TWO
        }

        public TestEnum value;
    }

    @Serialized
    public static final class IgnoreStaticFields {
        public static int s = 123;
        public int i = 234;
    }

    public static final class Custom {
        public int x;
    }

    @RegisterSerializer
    public static final class CustomSerializer implements Serializer<Custom> {
        @Override
        public void serialize(final SerializationVisitor visitor, final Class<Custom> type, final Object value) throws SerializationException {
            visitor.putInt("value", ((Custom) value).x * 2);
        }

        @Override
        public Custom deserialize(final DeserializationVisitor visitor, final Class<Custom> type, final Object value) throws SerializationException {
            Custom custom = (Custom) value;
            if (custom == null) custom = new Custom();
            custom.x = visitor.getInt("value") + 1;
            return custom;
        }
    }

    @Serialized
    public static final class MultipleCustomSerializer {
        public Custom a;
        public Custom b;
        public Custom c;
    }

    public static final class MutableIndirect {
        @Serialized public int v;
    }

    public static final class MutableIndirectMid {
        @Serialized public final MutableIndirect v = null;
    }

    public static final class MutableIndirectRoot {
        @Serialized public final MutableIndirectMid v = null;
    }

    public static final class ImmutableIndirect {
        @Serialized public final int v = 0;
    }

    public static final class ImmutableIndirectMid {
        @Serialized public final ImmutableIndirect v = null;
    }

    public static final class ImmutableIndirectRoot {
        @Serialized public final ImmutableIndirectMid v = null;
    }

    public static abstract class PolymorphicFieldType {
        @Serialized public int x;

        public abstract int f();
    }

    public static final class PolymorphicFieldClassMul2 extends PolymorphicFieldType {
        @Override
        public int f() {
            return x * 2;
        }
    }

    public static final class PolymorphicFieldClassAdd1 extends PolymorphicFieldType {
        @Override
        public int f() {
            return x + 1;
        }
    }

    public static final class PolymorphicFieldHolder {
        @Serialized public PolymorphicFieldType value;
    }

    static final class PolymorphicFieldTypeSerializer implements Serializer<PolymorphicFieldType> {
        static final PolymorphicFieldTypeSerializer INSTANCE = new PolymorphicFieldTypeSerializer();

        @Override
        public void serialize(final SerializationVisitor visitor, final Class<PolymorphicFieldType> type, final Object value) throws SerializationException {
            visitor.putBoolean("type", value instanceof PolymorphicFieldClassMul2);
            visitor.putInt("x", ((PolymorphicFieldType) value).x);
        }

        @Override
        public PolymorphicFieldType deserialize(final DeserializationVisitor visitor, final Class<PolymorphicFieldType> type, final Object value) throws SerializationException {
            final Class<?> polyType = visitor.getBoolean("type") ? PolymorphicFieldClassMul2.class : PolymorphicFieldClassAdd1.class;
            final int x = visitor.getInt("x");
            if (polyType == type) {
                ((PolymorphicFieldType) value).x = x;
                return (PolymorphicFieldType) value;
            } else {
                if (polyType == PolymorphicFieldClassMul2.class) {
                    final PolymorphicFieldClassMul2 inst = new PolymorphicFieldClassMul2();
                    inst.x = x;
                    return inst;
                } else {
                    final PolymorphicFieldClassAdd1 inst = new PolymorphicFieldClassAdd1();
                    inst.x = x;
                    return inst;
                }
            }
        }
    }

    @Serialized
    public static final class MultiDimArray {
        public int[][] array = {{1, 2, 3}, {4, 5, 6}};
    }
}
