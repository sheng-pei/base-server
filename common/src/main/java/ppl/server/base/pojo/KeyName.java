package ppl.server.base.pojo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.function.Supplier;

public class KeyName<K> {

    private final K key;
    private final String name;

    private KeyName(K key, String name) {
        this.key = key;
        this.name = name;
    }

    public K getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public static <K> KeyName<K> create(Supplier<K> keySupplier, Supplier<String> nameSupplier) {
        return new KeyName<>(keySupplier.get(), nameSupplier.get());
    }

    @JsonCreator
    public static <K> KeyName<K> create(@JsonProperty("key") K key, @JsonProperty("name") String name) {
        return new KeyName<>(key, name);
    }
}
