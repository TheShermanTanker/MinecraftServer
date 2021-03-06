package net.minecraft.util.profiling.metrics;

public enum EnumMetricCategory {
    PATH_FINDING("pathfinding"),
    EVENT_LOOPS("event-loops"),
    MAIL_BOXES("mailboxes"),
    TICK_LOOP("ticking"),
    JVM("jvm"),
    CHUNK_RENDERING("chunk rendering"),
    CHUNK_RENDERING_DISPATCHING("chunk rendering dispatching"),
    CPU("cpu");

    private final String description;

    private EnumMetricCategory(String name) {
        this.description = name;
    }

    public String getDescription() {
        return this.description;
    }
}
