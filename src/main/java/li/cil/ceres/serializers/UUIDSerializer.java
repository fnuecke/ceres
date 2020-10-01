package li.cil.ceres.serializers;

import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;
import li.cil.ceres.api.Serializer;

import javax.annotation.Nullable;
import java.util.UUID;

public final class UUIDSerializer implements Serializer<UUID> {
    @Override
    public void serialize(final SerializationVisitor visitor, final Class<?> type, final Object value) throws SerializationException {
        final UUID uuid = (UUID) value;
        visitor.putLong("msb", uuid.getMostSignificantBits());
        visitor.putLong("lsb", uuid.getLeastSignificantBits());
    }

    @Override
    public UUID deserialize(final DeserializationVisitor visitor, final Class<UUID> type, @Nullable final Object value) throws SerializationException {
        final long msb = visitor.getLong("msb");
        final long lsb = visitor.getLong("lsb");
        return new UUID(msb, lsb);
    }
}
