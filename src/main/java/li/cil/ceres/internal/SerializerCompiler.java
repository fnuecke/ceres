package li.cil.ceres.internal;

import li.cil.ceres.api.*;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

final class SerializerCompiler {
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

    @SuppressWarnings("unchecked")
    public static <T> Serializer<T> generateSerializer(final Class<T> type) throws SerializationException {
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
            generateSerializeMethod(serialize, type);
        }
        serialize.visitMaxs(-1, -1);
        serialize.visitEnd();

        // Deserialize
        final MethodVisitor deserialize = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "deserialize", Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(DeserializationVisitor.class), Type.getType(Class.class), Type.getType(Object.class)), null, new String[]{
                Type.getInternalName(SerializationException.class)
        });
        deserialize.visitCode();
        {
            generateDeserializeMethod(deserialize, type);
        }
        deserialize.visitMaxs(-1, -1);
        deserialize.visitEnd();

        cw.visitEnd();

        final Class<Serializer<T>> serializerClass = (Class<Serializer<T>>) UNSAFE.defineAnonymousClass(type, cw.toByteArray(), null);
        try {
            return serializerClass.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    private static <T> void generateSerializeMethod(final MethodVisitor mv, final Class<T> type) throws SerializationException {
        final int visitorIndex = 1;
        final int valueIndex = 3;
        final int fieldValueIndex = 4;
        int fieldValueCount = 0;

        mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
        mv.visitVarInsn(Opcodes.ASTORE, valueIndex);

        for (final Field field : collectFields(type)) {
            final Class<?> fieldType = field.getType();
            if (fieldType == boolean.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putBoolean", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == byte.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putByte", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == char.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putChar", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == short.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putShort", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == int.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putInt", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == long.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putLong", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == float.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putFloat", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else if (fieldType == double.class) {
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                        "putDouble", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class), Type.getType(fieldType)), true);
            } else {
                final Label fieldValueValidLabel = new Label();
                final Label elseLabel = new Label(), endifLabel = new Label();

                mv.visitLocalVariable("fieldValue" + fieldValueCount++, Type.getDescriptor(fieldType), null, fieldValueValidLabel, endifLabel, fieldValueIndex);

                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));

                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ASTORE, fieldValueIndex);
                mv.visitLabel(fieldValueValidLabel);

                // if (fieldValue != null)
                mv.visitJumpInsn(Opcodes.IFNULL, elseLabel);
                {
                    mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                    mv.visitLdcInsn(field.getName());
                    mv.visitLdcInsn(0);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                            "putNull", "(Ljava/lang/String;Z)V", true);

                    if (fieldType.isArray()) {
                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitVarInsn(Opcodes.ALOAD, fieldValueIndex);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                                "putArray", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)V", true);
                    } else if (fieldType.isEnum()) {
                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitVarInsn(Opcodes.ALOAD, fieldValueIndex);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Enum.class));
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                                "putEnum", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Enum;)V", true);
                    } else {
                        final Label nothrow = new Label();

                        mv.visitVarInsn(Opcodes.ALOAD, fieldValueIndex);
                        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                                "getClass", "()Ljava/lang/Class;", false);
                        mv.visitLdcInsn(Type.getType(fieldType));
                        // if (fieldValue.getClass() != fieldType)
                        mv.visitJumpInsn(Opcodes.IF_ACMPEQ, nothrow);
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
                                mv.visitVarInsn(Opcodes.ALOAD, fieldValueIndex); // [..., {}, {}, 0, fieldValue]
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
                        mv.visitLabel(nothrow);

                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitVarInsn(Opcodes.ALOAD, fieldValueIndex);
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                                "putObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)V", true);
                    }

                    mv.visitJumpInsn(Opcodes.GOTO, endifLabel);
                }
                // else
                mv.visitLabel(elseLabel);
                {
                    mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                    mv.visitLdcInsn(field.getName());
                    mv.visitLdcInsn(1);
                    mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                            "putNull", "(Ljava/lang/String;Z)V", true);
                }
                mv.visitLabel(endifLabel);
            }
        }

        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
            mv.visitLdcInsn("<super>");
            mv.visitLdcInsn(Type.getType(type));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                    "getSuperclass", "()Ljava/lang/Class;", false);
            mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(SerializationVisitor.class),
                    "putObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)V", true);
        }

        mv.visitInsn(Opcodes.RETURN);
    }

    private static <T> void generateDeserializeMethod(final MethodVisitor mv, final Class<T> type) throws SerializationException {
        final int visitorIndex = 1;
        final int valueIndex = 3;

        final Label nonnullLabel = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, valueIndex);

        // if (value == null)
        mv.visitJumpInsn(Opcodes.IFNONNULL, nonnullLabel);
        {
            // value = new type();
            mv.visitTypeInsn(Opcodes.NEW, Type.getInternalName(type));
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(type), "<init>", "()V", false);
            mv.visitVarInsn(Opcodes.ASTORE, valueIndex);
        }
        mv.visitLabel(nonnullLabel);

        mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
        mv.visitVarInsn(Opcodes.ASTORE, valueIndex);

        for (final Field field : collectFields(type)) {
            final Class<?> fieldType = field.getType();
            if (fieldType == boolean.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getBoolean", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == byte.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getByte", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == char.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getChar", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == short.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getShort", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == int.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getInt", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == long.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getLong", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == float.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getFloat", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else if (fieldType == double.class) {
                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "getDouble", Type.getMethodDescriptor(Type.getType(fieldType), Type.getType(String.class)), true);
                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            } else {
                final Label elseLabel = new Label(), endifLabel = new Label();

                mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                mv.visitLdcInsn(field.getName());
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                        "isNull", "(Ljava/lang/String;)Z", true);

                mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                mv.visitInsn(Opcodes.SWAP);

                // if (visitor.isNull(field.getName()))
                mv.visitJumpInsn(Opcodes.IFEQ, elseLabel);
                {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitJumpInsn(Opcodes.GOTO, endifLabel);
                }
                // else
                mv.visitLabel(elseLabel);
                {
                    if (fieldType.isArray()) {
                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                                "getArray", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;", true);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
                    } else if (fieldType.isEnum()) {
                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                                "getEnum", "(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Enum;", true);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
                    } else {
                        mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
                        mv.visitLdcInsn(field.getName());
                        mv.visitLdcInsn(Type.getType(fieldType));
                        mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
                        mv.visitFieldInsn(Opcodes.GETFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
                        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                                "getObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", true);
                        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(fieldType));
                    }
                }
                mv.visitLabel(endifLabel);

                mv.visitFieldInsn(Opcodes.PUTFIELD, Type.getInternalName(type), field.getName(), Type.getDescriptor(fieldType));
            }
        }

        final Class<?> parentType = type.getSuperclass();
        if (parentType != null && parentType != Object.class) {
            mv.visitVarInsn(Opcodes.ALOAD, visitorIndex);
            mv.visitLdcInsn("<super>");
            mv.visitLdcInsn(Type.getType(type));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                    "getSuperclass", "()Ljava/lang/Class;", false);
            mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
            mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(DeserializationVisitor.class),
                    "getObject", "(Ljava/lang/String;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", true);
        }

        mv.visitVarInsn(Opcodes.ALOAD, valueIndex);
        mv.visitInsn(Opcodes.ARETURN);
    }

    private static ArrayList<Field> collectFields(final Class<?> type) throws SerializationException {
        final boolean serializeFields = type.isAnnotationPresent(Serialized.class);
        final ArrayList<Field> fields = new ArrayList<>();
        for (final Field field : type.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())) {
                if (field.isAnnotationPresent(Serialized.class)) {
                    throw new SerializationException(String.format("Trying to use serialization on final field [%s.%s].", type.getName(), field.getName()));
                }
                continue;
            }

            if (serializeFields || field.isAnnotationPresent(Serialized.class)) {
                fields.add(field);
            }
        }
        return fields;
    }
}
