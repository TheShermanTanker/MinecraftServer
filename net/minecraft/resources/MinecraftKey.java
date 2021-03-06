package net.minecraft.resources;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.ResourceKeyInvalidException;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.util.ChatDeserializer;
import org.apache.commons.lang3.StringUtils;

public class MinecraftKey implements Comparable<MinecraftKey> {
    public static final Codec<MinecraftKey> CODEC = Codec.STRING.comapFlatMap(MinecraftKey::read, MinecraftKey::toString).stable();
    private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(new ChatMessage("argument.id.invalid"));
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";
    protected final String namespace;
    protected final String path;

    protected MinecraftKey(String[] id) {
        this.namespace = StringUtils.isEmpty(id[0]) ? "minecraft" : id[0];
        this.path = id[1];
        if (!isValidNamespace(this.namespace)) {
            throw new ResourceKeyInvalidException("Non [a-z0-9_.-] character in namespace of location: " + this.namespace + ":" + this.path);
        } else if (!isValidPath(this.path)) {
            throw new ResourceKeyInvalidException("Non [a-z0-9/._-] character in path of location: " + this.namespace + ":" + this.path);
        }
    }

    public MinecraftKey(String id) {
        this(decompose(id, ':'));
    }

    public MinecraftKey(String namespace, String path) {
        this(new String[]{namespace, path});
    }

    public static MinecraftKey of(String id, char delimiter) {
        return new MinecraftKey(decompose(id, delimiter));
    }

    @Nullable
    public static MinecraftKey tryParse(String id) {
        try {
            return new MinecraftKey(id);
        } catch (ResourceKeyInvalidException var2) {
            return null;
        }
    }

    protected static String[] decompose(String id, char delimiter) {
        String[] strings = new String[]{"minecraft", id};
        int i = id.indexOf(delimiter);
        if (i >= 0) {
            strings[1] = id.substring(i + 1, id.length());
            if (i >= 1) {
                strings[0] = id.substring(0, i);
            }
        }

        return strings;
    }

    private static DataResult<MinecraftKey> read(String id) {
        try {
            return DataResult.success(new MinecraftKey(id));
        } catch (ResourceKeyInvalidException var2) {
            return DataResult.error("Not a valid resource location: " + id + " " + var2.getMessage());
        }
    }

    public String getKey() {
        return this.path;
    }

    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof MinecraftKey)) {
            return false;
        } else {
            MinecraftKey resourceLocation = (MinecraftKey)object;
            return this.namespace.equals(resourceLocation.namespace) && this.path.equals(resourceLocation.path);
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.path.hashCode();
    }

    @Override
    public int compareTo(MinecraftKey resourceLocation) {
        int i = this.path.compareTo(resourceLocation.path);
        if (i == 0) {
            i = this.namespace.compareTo(resourceLocation.namespace);
        }

        return i;
    }

    public String toDebugFileName() {
        return this.toString().replace('/', '_').replace(':', '_');
    }

    public static MinecraftKey read(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();

        while(reader.canRead() && isAllowedInResourceLocation(reader.peek())) {
            reader.skip();
        }

        String string = reader.getString().substring(i, reader.getCursor());

        try {
            return new MinecraftKey(string);
        } catch (ResourceKeyInvalidException var4) {
            reader.setCursor(i);
            throw ERROR_INVALID.createWithContext(reader);
        }
    }

    public static boolean isAllowedInResourceLocation(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

    private static boolean isValidPath(String path) {
        for(int i = 0; i < path.length(); ++i) {
            if (!validPathChar(path.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidNamespace(String namespace) {
        for(int i = 0; i < namespace.length(); ++i) {
            if (!validNamespaceChar(namespace.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static boolean validPathChar(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '/' || character == '.';
    }

    private static boolean validNamespaceChar(char character) {
        return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
    }

    public static boolean isValidResourceLocation(String id) {
        String[] strings = decompose(id, ':');
        return isValidNamespace(StringUtils.isEmpty(strings[0]) ? "minecraft" : strings[0]) && isValidPath(strings[1]);
    }

    public static class Serializer implements JsonDeserializer<MinecraftKey>, JsonSerializer<MinecraftKey> {
        @Override
        public MinecraftKey deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return new MinecraftKey(ChatDeserializer.convertToString(jsonElement, "location"));
        }

        @Override
        public JsonElement serialize(MinecraftKey resourceLocation, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(resourceLocation.toString());
        }
    }
}
