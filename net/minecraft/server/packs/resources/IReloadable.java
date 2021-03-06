package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import net.minecraft.util.Unit;

public interface IReloadable {
    CompletableFuture<Unit> done();

    float getActualProgress();

    boolean isDone();

    void checkExceptions();
}
