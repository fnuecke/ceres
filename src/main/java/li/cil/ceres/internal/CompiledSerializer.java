package li.cil.ceres.internal;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

final class CompiledSerializer {
    private static final Unsafe UNSAFE;

    static {
        final Field field;
        try {
            field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (final NoSuchFieldException | IllegalAccessException e) {
            throw new AssertionError();
        }
    }

    private static final int SERIALIZER_VISITOR_INDEX = 1;
    private static final int SERIALIZER_VALUE_INDEX = 3;
    private static final int SERIALIZER_FIELD_VALUE_INDEX = 4;
    private static final int DESERIALIZER_VISITOR_INDEX = 1;
    private static final int DESERIALIZER_VALUE_INDEX = 3;

    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> generateSerializer(final Class<T> type) throws SerializationException {
        if (type.isInterface()) {
            throw new SerializationException(String.format("Cannot generate serializer for interface [%s].", type));
        }

        final ArrayList<Field> fields = SerializerUtils.collectSerializableFields(type);
        if (fields.isEmpty()) {
            return SuperclassSerializer.INSTANCE;
        }

        final String className = Type.getInternalName(type) + "$" + Type.getInternalName(Serializer.class);

        // Generate signature for `implements Serializer<type>`
        final SignatureWriter classSignature = new SignatureWriter();
        {
            final SignatureVisitor serializer = classSignature.visitInterface();
            serializer.visitClassType(Type.getInternalName(Serializer.class));
            final SignatureVisitor serializerType = serializer.visitTypeArgument('=');
            serializerType.visitClassType(Type.getInternalName(type));
            serializerType.visitEnd();
            serializer.visitEnd();
        }
        classSignature.visitEnd();

        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                className,
                classSignature.toString(),
                Type.getInternalName(Object.class),
                new String[]{
                        Type.getInternalName(Serializer.class)
                });

        // Constructor
        final MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        {
            init.visitVarInsn(Opcodes.ALOAD, 0);
            init.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
            init.visitInsn(Opcodes.RETURN);
        }
        init.visitMaxs(-1, -1);
        init.visitEnd();

        // Serialize
        final MethodVisitor serialize = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "serialize", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(SerializationVisitor.class), Type.getType(Class.class), Type.getType(Object.class)), null, new String[]{
                Type.getInternalName(SerializationException.class)
        });
        serialize.visitCode();
        {
            generateSerializeMethod(serialize, type, fields);
        }
        serialize.visitMaxs(-1, -1);
        serialize.visitEnd();

        // Deserialize
        final MethodVisitor deserialize = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "deserialize", Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(DeserializationVisitor.class), Type.getType(Class.class), Type.getType(Object.class)), null, new String[]{
                Type.getInternalName(SerializationException.class)
        });
        deserialize.visitCode();
        {
            generateDeserializeMethod(deserialize, type, fields);
        }
        deserialize.visitMaxs(-1, -1);
        deserialize.visitEnd();

        cw.visitEnd();

        final Class<Serializer<T>> serializerClass = (Class<Serializer<T>>) UNSAFE.defineAnonymousClass(type, cw.toByteArray(), null);
        try {
            return serializerClass.newInstance();
        } catch (final Throwable e) {
            throw new SerializationException(String.format("Failed generating serializer for type [%s]", type), e);
        }
    }

    private static <T> void generateSerializeMethod(final MethodVisitor mv, final Class<T> type, final ArrayList<Field> fields) {
        int fieldValueCount = 0;

        // value = (type) value; to satisfy class verification.
        mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VALUE_INDEX);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
        mv.visitVarInsn(Opcodes.ASTORE, SERIALIZER_VALUE_INDEX);

        for (final Field field : fields) {
            final Class<?> fieldType = field.getType();
            if (fieldType == boolean.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putBoolean");
            } else if (fieldType == byte.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putByte");
            } else if (fieldType == char.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putChar");
            } else if (fieldType == short.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putShort");
            } else if (fieldType == int.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putInt");
            } else if (fieldType == long.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putLong");
            } else if (fieldType == float.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putFloat");
            } else if (fieldType == double.class) {
                generateSerializePrimitiveCall(mv, type, field, fieldType, "putDouble");
            } else {
                final Label fieldValueValidLabel = new Label();
                final Label nothrowLabel = new Label(), endifLabel = new Label();

                mv.visitLocalVariable("fieldValue" + fieldValueCount++, Type.getDescriptor(fieldType), null, fieldValueValidLabel, endifLabel, SERIALIZER_FIELD_VALUE_INDEX);

                mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VALUE_INDEX);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));

                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ASTORE, SERIALIZER_FIELD_VALUE_INDEX);
                mv.visitLabel(fieldValueValidLabel);

                // if (fieldValue != null &&
                mv.visitJumpInsn(Opcodes.IFNULL, nothrowLabel);
                //     fieldValue.getClass() != fieldType)
                mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_FIELD_VALUE_INDEX);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                        "getClass", "()Ljava/lang/Class;", false);
                mv.visitLdcInsn(Type.getType(fieldType));
                mv.visitJumpInsn(Opcodes.IF_ACMPEQ, nothrowLabel);
                {
                    // throw new SerializationException(message, value.getClass().getName(), fieldType.getName(), type.getName(), field.getName()));
                    mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(SerializationException.class)); // [e]
                    mv.visitInsn(Opcodes.DUP); // [e, e]

                    // message = String.format(fmt, args)
                    {
                        mv.visitLdcInsn("Value type [%s] does not match field type [%s] in field [%s.%s]. Polymorphism is not supported."); // [e, e, fmt]

                        // args = new Object[4];
                        mv.visitLdcInsn(4); // [e, e, fmt, len]
                        mv.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(Object.class)); // [e, e, fmt, {}]

                        // args[0] = fieldValue.getClass().getName()
                        mv.visitInsn(Opcodes.DUP); // [..., {}, {}]
                        mv.visitLdcInsn(0); // [..., {}, {}, 0]
                        mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_FIELD_VALUE_INDEX); // [..., {}, {}, 0, fieldValue]
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                                "getClass", "()Ljava/lang/Class;", false); // [..., {}, {}, 0, fieldValueType]
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                                "getName", "()Ljava/lang/String;", false); // [..., {}, {}, 0, fieldValueTypeName]
                        mv.visitInsn(Opcodes.AASTORE); // [..., {fieldValueTypeName}]

                        // args[1] = fieldType.getName();
                        mv.visitInsn(Opcodes.DUP); // [..., {}, {}]
                        mv.visitLdcInsn(1); // [..., {}, {}, 1]
                        mv.visitLdcInsn(fieldType.getName()); // [..., {}, {}, 1, fieldTypeName]
                        mv.visitInsn(Opcodes.AASTORE); // [..., {fieldValueTypeName, fieldTypeName}]

                        // args[2] = type.getName();
                        mv.visitInsn(Opcodes.DUP); // [..., {}, {}]
                        mv.visitLdcInsn(1); // [..., {}, {}, 2]
                        mv.visitLdcInsn(type.getName()); // [..., {}, {}, 1, typeName]
                        mv.visitInsn(Opcodes.AASTORE); // [..., {fieldValueTypeName, fieldTypeName, typeName}]

                        // args[3] = field.getName();
                        mv.visitInsn(Opcodes.DUP); // [..., {}, {}]
                        mv.visitLdcInsn(1); // [..., {}, {}, 3]
                        mv.visitLdcInsn(field.getName()); // [..., {}, {}, 1, fieldName]
                        mv.visitInsn(Opcodes.AASTORE); // [..., {fieldValueTypeName, fieldTypeName, typeName, fieldName}]

                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(String.class),
                                "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false); // [e, e, message]
                    }

                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(SerializationException.class),
                            "<init>", "(Ljava/lang/String;)V", false); // [e]
                    mv.visitInsn(Opcodes.ATHROW); // []
                }
                // else
                mv.visitLabel(nothrowLabel);
                {
                    mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VISITOR_INDEX);
                    mv.visitLdcInsn(field.getName());
                    mv.visitLdcInsn(Type.getType(fieldType));
                    mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_FIELD_VALUE_INDEX);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                            "putObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)V", true);
                }
                mv.visitLabel(endifLabel);
            }
        }

        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VISITOR_INDEX);
            mv.visitLdcInsn("<super>");
            mv.visitLdcInsn(Type.getType(type));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                    "getSuperclass", "()Ljava/lang/Class;", false);
            mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VALUE_INDEX);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                    "putObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)V", true);
        }

        mv.visitInsn(Opcodes.RETURN);
    }

    private static <T> void generateDeserializeMethod(final MethodVisitor mv, final Class<T> type, final ArrayList<Field> fields) {
        final Label nonnullLabel = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);

        // if (value == null)
        mv.visitJumpInsn(Opcodes.IFNONNULL, nonnullLabel);
        {
            if (Modifier.isAbstract(type.getModifiers())) {
                // throw new SerializationException(String.format("Cannot create new instance of abstract type [%s].", type));
                mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(SerializationException.class));
                mv.visitInsn(Opcodes.DUP);
                mv.visitLdcInsn(String.format("Cannot create new instance of abstract type [%s].", type));
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(SerializationException.class), "<init>", "(Ljava/lang/String;)V", false);
                mv.visitInsn(Opcodes.ATHROW);
            } else {
                boolean hasDefaultConstructor = false;
                try {
                    type.getDeclaredConstructor();
                    hasDefaultConstructor = true;
                } catch (final NoSuchMethodException ignored) {
                }

                if (!hasDefaultConstructor) {
                    // throw new SerializationException(String.format("Cannot create new instance of type without a default constructor [%s].", type));
                    mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(SerializationException.class));
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(String.format("Cannot create new instance of type without a default constructor [%s].", type));
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(SerializationException.class), "<init>", "(Ljava/lang/String;)V", false);
                    mv.visitInsn(Opcodes.ATHROW);
                } else {
                    // value = new type();
                    mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>", "()V", false);
                    mv.visitVarInsn(Opcodes.ASTORE, DESERIALIZER_VALUE_INDEX);
                }
            }
        }
        mv.visitLabel(nonnullLabel);

        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
        mv.visitVarInsn(Opcodes.ASTORE, DESERIALIZER_VALUE_INDEX);

        for (final Field field : fields) {
            final Label endifLabel = new Label();

            // if (visitor.exists(field.getName()))
            mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VISITOR_INDEX);
            mv.visitLdcInsn(field.getName());
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                    "exists", "(Ljava/lang/String;)Z", true);
            mv.visitJumpInsn(Opcodes.IFEQ, endifLabel);
            {
                mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);

                final Class<?> fieldType = field.getType();
                if (fieldType == boolean.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getBoolean");
                } else if (fieldType == byte.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getByte");
                } else if (fieldType == char.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getChar");
                } else if (fieldType == short.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getShort");
                } else if (fieldType == int.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getInt");
                } else if (fieldType == long.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getLong");
                } else if (fieldType == float.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getFloat");
                } else if (fieldType == double.class) {
                    generateDeserializePrimitiveCall(mv, type, field, fieldType, "getDouble");
                } else if (Modifier.isFinal(field.getModifiers())) {
                    // Must deserialize into existing final field references, we never overwrite final field values.
                    // This means there is the weird edge-case where the length of a serialized array may
                    // differ from the currently assigned array. In that case the serialized value silently
                    // get ignores. I'll probably kick myself for this in the future.
                    generateDeserializeObjectCall(mv, type, field, fieldType);
                    mv.visitInsn(Opcodes.POP2);
                } else {
                    generateDeserializeObjectCall(mv, type, field, fieldType);
                    mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
                    mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                }
            }
            mv.visitLabel(endifLabel);
        }

        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VISITOR_INDEX);
            mv.visitLdcInsn("<super>");
            mv.visitLdcInsn(Type.getType(type));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                    "getSuperclass", "()Ljava/lang/Class;", false);
            mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                    "getObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", true);
        }

        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);
        mv.visitInsn(Opcodes.ARETURN);
    }

    private static <T> void generateSerializePrimitiveCall(final MethodVisitor mv, final Class<T> type, final Field field, final Class<?> fieldType, final String name) {
        mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VISITOR_INDEX);
        mv.visitLdcInsn(field.getName());
        mv.visitVarInsn(Opcodes.ALOAD, SERIALIZER_VALUE_INDEX);
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                name, Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
    }

    private static <T> void generateDeserializePrimitiveCall(final MethodVisitor mv, final Class<T> type, final Field field, final Class<?> fieldType, final String name) {
        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VISITOR_INDEX);
        mv.visitLdcInsn(field.getName());
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                name, Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
        mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
    }

    private static <T> void generateDeserializeObjectCall(final MethodVisitor mv, final Class<T> type, final Field field, final Class<?> fieldType) {
        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VISITOR_INDEX);
        mv.visitLdcInsn(field.getName());
        mv.visitLdcInsn(Type.getType(fieldType));
        mv.visitVarInsn(Opcodes.ALOAD, DESERIALIZER_VALUE_INDEX);
        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                "getObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", true);
    }
}
