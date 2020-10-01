package li.cil.ceres;

import li.cil.ceres.api.Serialized;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class SerializationTests {
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

        final ByteBuffer serialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.serialize(value));

        Flat deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, Flat.class, new Flat()));

        Assertions.assertEquals(value.byteValue, deserialized.byteValue);
        Assertions.assertEquals(value.shortValue, deserialized.shortValue);
        Assertions.assertEquals(value.intValue, deserialized.intValue);
        Assertions.assertEquals(value.longValue, deserialized.longValue);
        Assertions.assertEquals(value.floatValue, deserialized.floatValue);
        Assertions.assertEquals(value.doubleValue, deserialized.doubleValue);
        Assertions.assertArrayEquals(value.byteArrayValue, deserialized.byteArrayValue);
        Assertions.assertArrayEquals(value.intArrayValue, deserialized.intArrayValue);
        Assertions.assertArrayEquals(value.longArrayValue, deserialized.longArrayValue);
        Assertions.assertEquals(value.stringValue, deserialized.stringValue);
        Assertions.assertEquals(value.uuidValue, deserialized.uuidValue);

        deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, Flat.class, null));

        Assertions.assertEquals(value.byteValue, deserialized.byteValue);
        Assertions.assertEquals(value.shortValue, deserialized.shortValue);
        Assertions.assertEquals(value.intValue, deserialized.intValue);
        Assertions.assertEquals(value.longValue, deserialized.longValue);
        Assertions.assertEquals(value.floatValue, deserialized.floatValue);
        Assertions.assertEquals(value.doubleValue, deserialized.doubleValue);
        Assertions.assertArrayEquals(value.byteArrayValue, deserialized.byteArrayValue);
        Assertions.assertArrayEquals(value.intArrayValue, deserialized.intArrayValue);
        Assertions.assertArrayEquals(value.longArrayValue, deserialized.longArrayValue);
        Assertions.assertEquals(value.stringValue, deserialized.stringValue);
        Assertions.assertEquals(value.uuidValue, deserialized.uuidValue);
    }

    @Test
    public void testFlatFields() {
        final FlatFields value = new FlatFields();
        value.value1 = 123;
        value.value2 = 234;

        final ByteBuffer serialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.serialize(value));

        final FlatFields deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, FlatFields.class, null));

        Assertions.assertEquals(value.value1, deserialized.value1);
        Assertions.assertEquals(0, deserialized.value2);
    }

    @Test
    public void testModifiers() {
        final WithModifiers value = new WithModifiers();
        value.nonTransientInt = 123;
        value.transientInt = 234;
        value.finalIntArray[0] = 23;
        value.finalIntArray[1] = 64;
        value.finalIntArray[2] = 420;

        final ByteBuffer serialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.serialize(value));

        final WithModifiers deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, WithModifiers.class, null));

        Assertions.assertEquals(value.nonTransientInt, deserialized.nonTransientInt);
        Assertions.assertEquals(0, deserialized.transientInt);
        Assertions.assertArrayEquals(value.finalIntArray, deserialized.finalIntArray);
    }

    @Test
    public void testRecursive() {
        final Recursive root = new Recursive();
        root.value = 123;
        root.child = new Recursive();
        root.child.value = 234;

        final ByteBuffer serialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.serialize(root));

        final Recursive deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, Recursive.class, null));

        Assertions.assertEquals(root.value, deserialized.value);
        Assertions.assertNotNull(deserialized.child);
        Assertions.assertEquals(root.child.value, deserialized.child.value);
    }

    @Test
    public void testHierarchy() {
        final Subclass value = new Subclass();
        value.val = 123;
        value.sup1 = 234;
        value.sup2 = 345;

        final ByteBuffer serialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.serialize(value));

        final Subclass deserialized = Assertions.assertDoesNotThrow(() -> DataStreamSerialization.deserialize(serialized, Subclass.class, null));

        Assertions.assertEquals(value.val, deserialized.val);
        Assertions.assertEquals(0, deserialized.sup1);
        Assertions.assertEquals(value.sup2, deserialized.sup2);
    }

    @Serialized
    private static final class Flat {
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
    }

    private static final class FlatFields {
        @Serialized private int value1;
        private int value2;
    }

    @Serialized
    private static final class WithModifiers {
        private int nonTransientInt;
        private transient int transientInt;
        private final int finalInt = 678;
        private final int[] finalIntArray = new int[3];
    }

    @Serialized
    private static final class Recursive {
        private int value;
        private Recursive child;
    }

    @Serialized
    private static class Subclass extends NonSerializedSuperclass {
        int val;
    }

    private static class NonSerializedSuperclass extends SerializedSuperclass {
        int sup1;
    }

    @Serialized
    private static class SerializedSuperclass {
        int sup2;
    }
}
