# Ceres Serialization Library

Ceres is a relatively simple serialization library. The reason it exists is that there are some requirements to
behaviour that I didn't really find a way of handling with other serialization logic such as Java's built-in
serialization or Gson.

## Features

- **Separation of object deconstruction and serialized data output**. This means the same serializer logic can be used
  in combination with various serialization formats. The only built-in format is plain binary data using
  `DataOutputStream`s and `DataInputStream`s producing and consuming `ByteBuffer`s, respectively. This is implemented
  and made available in the [BinarySerialization](src/main/java/li/cil/ceres/BinarySerialization.java) class.
- **Deserialization into existing objects**. This means it is possible to "load" some persisted state into an object
  that was re-created outside the deserialization logic. This is useful in cases where the deserializer would otherwise
  need mechanisms to access context for constructing certain objects.
- **Automatic serializer generation**. Types can be annotated to have a serializer generated for them automatically. By
  default, these serializers will be realized as generated classes for maximum performance.

## Limitations

- Only supports creating objects in deserialization if the type has a default constructor.
- Aliasing is not supported. Each occurrence of a reference to some object will be serialized separately. Consequently,
  cycles are not supported.
- Polymorphism support requires an explicit serializer for the supertype to be serialized and deserialized.
- Can only deserialize into existing values of final fields. I.e. deserialization will not replace the object assigned
  to a final field.

## Design

The core principle of Ceres is to separate serialization into a *deconstruction* step, and a *data storage* step.

Serializers take care of the deconstruction step, i.e. they simply deconstruct complex data types into primitive data
types, which can then be written and read through the `SerializationVisitor` and `DeserializationVisitor`
interfaces. These visitors' implementations take care of writing and reading primitive data types to and from a backing
storage.

Finally, I like to use the term *serialization backend* for the collection of utility methods that can be used for
correctly invoking a registered serializer for serializing or deserializing a value using some a pair of visitors. An
example for this is the `BinarySerialization` class.

One concession is that implementations may choose to ignore the names passed for serialized and deserialized values. As
such, serializers should, where possible ensure writing and reading happens in the same order.

## Usage

To serialize an object the recommended way is to go through a serialization backend. See the built-in
`BinarySerialization` for reference. Alternatively the serializer for a type can be obtained and invoked directly.

```java

public class Example {
    @Serialized
    public static class MySerializableType {
        public int a;
        public int b;
    }

    // The above is equivalent to marking all fields directly like so:
    public static class MySerializableType {
        @Serialized public int a;
        @Serialized public int b;
    }

    public static void main(String[] args) {
        // Through the BinarySerialization backend:

        ByteBuffer serialized = BinarySerialization.serialize(new MySerializableType());

        // Deserialize into a new object. This requires Example to have a default constructor.
        MySerializableType deserialized = BinarySerialization.deserialize(serialized, MySerializableType.class);

        // Deserialize into an existing object, if possible.
        // This will use the serializer for deserialized.getClass().
        // As such, deserialized must not be null.
        BinarySerialization.deserialize(serialized, deserialized);

        // Alternatively, specify the type explicitly. Here deserialized may be null.
        BinarySerialization.deserialize(serialized, MySerializableType.class, deserialized);

        // If there's a chance we cannot deserialize into the specified object,
        // e.g. for arrays if the length may differ, use the returned value.
        deserialized = BinarySerialization.deserialize(serialized, MySerializableType.class, deserialized);


        // Using serializers directly:

        // Create the part that will actually write the data to some output format (e.g. MySerializedDataType).
        MySerializer serializer = new MySerializer();
        Ceres.getSerializer(MySerializableType.class)
                .serialize(serializer, MySerializableType.class, new MySerializableType());

        MyDeserializer deserializer = new MyDeserializer(serializer.getSerializedData());
        deserialized = Ceres.getSerializer(MySerializableType.class)
                .deserialize(deserializer, MySerializableType.class, deserialized);
    }

    private static class MySerializedDataType {
        // Something that stores the serialized data, e.g. binary, json, ...
    }

    private static class MySerializer implements SerializationVisitor {
        public MySerializedDataType getSerializedData();

        // Implementation ...
    }

    private static class MyDeserializer implements DeserializationVisitor {
        public DeserializationVisitor(MySerializedDataType serializedData) {
            // Store access to serialized data for reading via the implemented interface.
        }

        // Implementation ...
    }
}
```

## Maven

Ceres can be included into a project via the Github Package Repository. See [the documentation][GithubPackagesGradle]
for more information on how to set that up. In short, you'll want to add your username and a public access token into
your `~/.gradle/gradle.properties` and use those variables in your repository declaration. Note that the public access
token will need `read:packages` permissions.

For example, using Gradle:

```groovy
repositories {
  maven {
    url = uri("https://maven.pkg.github.com/fnuecke/ceres")
    credentials {
      username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
      password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
    }
  }
}

dependencies {
  implementation 'li.cil.ceres:ceres:0.0.4'
}
```

[GithubPackagesGradle]: https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry
