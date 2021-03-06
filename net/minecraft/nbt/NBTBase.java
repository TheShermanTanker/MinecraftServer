package net.minecraft.nbt;

import java.io.DataOutput;
import java.io.IOException;

public interface NBTBase {
    int OBJECT_HEADER = 64;
    int ARRAY_HEADER = 96;
    int OBJECT_REFERENCE = 32;
    int STRING_SIZE = 224;
    byte TAG_END = 0;
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;
    byte TAG_ANY_NUMERIC = 99;
    int MAX_DEPTH = 512;

    void write(DataOutput output) throws IOException;

    @Override
    String toString();

    byte getTypeId();

    NBTTagType<?> getType();

    NBTBase clone();

    default String asString() {
        return (new TagVisitorString()).visit(this);
    }

    void accept(TagVisitor visitor);

    StreamTagVisitor.ValueResult accept(StreamTagVisitor visitor);

    default void acceptAsRoot(StreamTagVisitor visitor) {
        StreamTagVisitor.ValueResult valueResult = visitor.visitRootEntry(this.getType());
        if (valueResult == StreamTagVisitor.ValueResult.CONTINUE) {
            this.accept(visitor);
        }

    }
}
