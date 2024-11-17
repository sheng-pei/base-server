package ppl.server.base.session;

import java.util.UUID;

public final class UuidSessionIdGenerator implements SessionIdGenerator {

    private static final UuidSessionIdGenerator INSTANCE = new UuidSessionIdGenerator();

    private UuidSessionIdGenerator() {
    }

    @Override
    public String generate() {
        return UUID.randomUUID().toString();
    }

    public static UuidSessionIdGenerator getInstance() {
        return INSTANCE;
    }

}
