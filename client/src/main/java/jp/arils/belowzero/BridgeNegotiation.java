package jp.arils.belowzero;

import java.nio.charset.StandardCharsets;

/** Connection-scoped capability negotiation, independent of Minecraft rendering. */
public final class BridgeNegotiation {
    public static final String CHANNEL = "arils:belowzero";
    private static final long TIMEOUT_NANOS = 5_000_000_000L;
    private Object connection;
    private Object requestedWorld;
    private boolean advertised;
    private boolean waiting;
    private boolean confirmed;
    private long requestedAt;

    public void connect(Object owner) {
        connection = owner;
        advertised = false;
        requestedWorld = null;
        waiting = false;
        confirmed = false;
    }

    public void disconnect() { connect(null); }
    public boolean owns(Object owner) { return owner != null && owner == connection; }
    public boolean advertised() { return advertised; }
    public boolean waiting() { return waiting; }

    public boolean channels(Object owner, byte[] bytes, boolean register) {
        if (!owns(owner) || bytes.length > 32767) return false;
        boolean contains = false;
        for (String channel : new String(bytes, StandardCharsets.UTF_8).split("\u0000", -1)) {
            if (CHANNEL.equals(channel)) contains = true;
        }
        if (!contains) return false;
        if (register) {
            if (!advertised) requestedWorld = null;
            advertised = true;
        } else {
            advertised = false;
            requestedWorld = null;
            waiting = false;
        confirmed = false;
        }
        return true;
    }

    public void worldChanged(Object owner) {
        if (!owns(owner)) return;
        requestedWorld = null;
        waiting = false;
        confirmed = false;
    }

    public boolean request(Object owner, Object world, long now) {
        if (!owns(owner) || !advertised || world == null || world == requestedWorld) return false;
        requestedWorld = world;
        requestedAt = now;
        waiting = true;
        return true;
    }

    public boolean accepts(Object owner, Object world) {
        return owns(owner) && advertised && (waiting || confirmed) && world == requestedWorld;
    }

    public void confirmed(Object owner, Object world) {
        if (accepts(owner, world)) { waiting = false; confirmed = true; }
    }

    public void fallback() { waiting = false; confirmed = false; }

    public boolean expire(long now) {
        if (!waiting || now - requestedAt < TIMEOUT_NANOS) return false;
        waiting = false;
        confirmed = false;
        return true;
    }
}
