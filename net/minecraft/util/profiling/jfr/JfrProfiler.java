package net.minecraft.util.profiling.jfr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.FlightRecorderListener;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import net.minecraft.SharedConstants;
import net.minecraft.SystemUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.util.profiling.jfr.event.ChunkGenerationEvent;
import net.minecraft.util.profiling.jfr.event.NetworkSummaryEvent;
import net.minecraft.util.profiling.jfr.event.PacketReceivedEvent;
import net.minecraft.util.profiling.jfr.event.PacketSentEvent;
import net.minecraft.util.profiling.jfr.event.ServerTickTimeEvent;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.level.ChunkCoordIntPair;
import net.minecraft.world.level.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JfrProfiler implements JvmProfiler {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String ROOT_CATEGORY = "Minecraft";
    public static final String WORLD_GEN_CATEGORY = "World Generation";
    public static final String TICK_CATEGORY = "Ticking";
    public static final String NETWORK_CATEGORY = "Network";
    private static final List<Class<? extends Event>> CUSTOM_EVENTS = List.of(ChunkGenerationEvent.class, PacketReceivedEvent.class, PacketSentEvent.class, NetworkSummaryEvent.class, ServerTickTimeEvent.class, WorldLoadFinishedEvent.class);
    private static final String FLIGHT_RECORDER_CONFIG = "/flightrecorder-config.jfc";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = (new DateTimeFormatterBuilder()).appendPattern("yyyy-MM-dd-HHmmss").toFormatter().withZone(ZoneId.systemDefault());
    private static final JfrProfiler INSTANCE = new JfrProfiler();
    @Nullable
    Recording recording;
    private float currentAverageTickTime;
    private final Map<String, NetworkSummaryEvent.SumAggregation> networkTrafficByAddress = new ConcurrentHashMap<>();

    private JfrProfiler() {
        CUSTOM_EVENTS.forEach(FlightRecorder::register);
        FlightRecorder.addPeriodicEvent(ServerTickTimeEvent.class, () -> {
            (new ServerTickTimeEvent(this.currentAverageTickTime)).commit();
        });
        FlightRecorder.addPeriodicEvent(NetworkSummaryEvent.class, () -> {
            Iterator<NetworkSummaryEvent.SumAggregation> iterator = this.networkTrafficByAddress.values().iterator();

            while(iterator.hasNext()) {
                iterator.next().commitEvent();
                iterator.remove();
            }

        });
    }

    public static JfrProfiler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean start(Environment instanceType) {
        URL uRL = JfrProfiler.class.getResource("/flightrecorder-config.jfc");
        if (uRL == null) {
            LOGGER.warn("Could not find default flight recorder config at {}", (Object)"/flightrecorder-config.jfc");
            return false;
        } else {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uRL.openStream()));

                boolean var4;
                try {
                    var4 = this.start(bufferedReader, instanceType);
                } catch (Throwable var7) {
                    try {
                        bufferedReader.close();
                    } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                    }

                    throw var7;
                }

                bufferedReader.close();
                return var4;
            } catch (IOException var8) {
                LOGGER.warn("Failed to start flight recorder using configuration at {}", uRL, var8);
                return false;
            }
        }
    }

    @Override
    public Path stop() {
        if (this.recording == null) {
            throw new IllegalStateException("Not currently profiling");
        } else {
            this.networkTrafficByAddress.clear();
            Path path = this.recording.getDestination();
            this.recording.stop();
            return path;
        }
    }

    @Override
    public boolean isRunning() {
        return this.recording != null;
    }

    @Override
    public boolean isAvailable() {
        return FlightRecorder.isAvailable();
    }

    private boolean start(Reader reader, Environment instanceType) {
        if (this.isRunning()) {
            LOGGER.warn("Profiling already in progress");
            return false;
        } else {
            try {
                Configuration configuration = Configuration.create(reader);
                String string = DATE_TIME_FORMATTER.format(Instant.now());
                this.recording = SystemUtils.make(new Recording(configuration), (recording) -> {
                    CUSTOM_EVENTS.forEach(recording::enable);
                    recording.setDumpOnExit(true);
                    recording.setToDisk(true);
                    recording.setName("%s-%s-%s".formatted(instanceType.getDescription(), SharedConstants.getCurrentVersion().getName(), string));
                });
                Path path = Paths.get("debug/%s-%s.jfr".formatted(instanceType.getDescription(), string));
                if (!Files.exists(path.getParent())) {
                    Files.createDirectories(path.getParent());
                }

                this.recording.setDestination(path);
                this.recording.start();
                this.setupSummaryListener();
            } catch (ParseException | IOException var6) {
                LOGGER.warn("Failed to start jfr profiling", (Throwable)var6);
                return false;
            }

            LOGGER.info("Started flight recorder profiling id({}):name({}) - will dump to {} on exit or stop command", this.recording.getId(), this.recording.getName(), this.recording.getDestination());
            return true;
        }
    }

    private void setupSummaryListener() {
        FlightRecorder.addListener(new FlightRecorderListener() {
            final SummaryReporter summaryReporter = new SummaryReporter(() -> {
                JfrProfiler.this.recording = null;
            });

            @Override
            public void recordingStateChanged(Recording recording) {
                if (recording == JfrProfiler.this.recording && recording.getState() == RecordingState.STOPPED) {
                    this.summaryReporter.recordingStopped(recording.getDestination());
                    FlightRecorder.removeListener(this);
                }
            }
        });
    }

    @Override
    public void onServerTick(float tickTime) {
        if (ServerTickTimeEvent.TYPE.isEnabled()) {
            this.currentAverageTickTime = tickTime;
        }

    }

    @Override
    public void onPacketReceived(int protocolId, int packetId, SocketAddress remoteAddress, int bytes) {
        if (PacketReceivedEvent.TYPE.isEnabled()) {
            (new PacketReceivedEvent(protocolId, packetId, remoteAddress, bytes)).commit();
        }

        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(remoteAddress).trackReceivedPacket(bytes);
        }

    }

    @Override
    public void onPacketSent(int protocolId, int packetId, SocketAddress remoteAddress, int bytes) {
        if (PacketSentEvent.TYPE.isEnabled()) {
            (new PacketSentEvent(protocolId, packetId, remoteAddress, bytes)).commit();
        }

        if (NetworkSummaryEvent.TYPE.isEnabled()) {
            this.networkStatFor(remoteAddress).trackSentPacket(bytes);
        }

    }

    private NetworkSummaryEvent.SumAggregation networkStatFor(SocketAddress address) {
        return this.networkTrafficByAddress.computeIfAbsent(address.toString(), NetworkSummaryEvent.SumAggregation::new);
    }

    @Nullable
    @Override
    public ProfiledDuration onWorldLoadedStarted() {
        if (!WorldLoadFinishedEvent.TYPE.isEnabled()) {
            return null;
        } else {
            WorldLoadFinishedEvent worldLoadFinishedEvent = new WorldLoadFinishedEvent();
            worldLoadFinishedEvent.begin();
            return worldLoadFinishedEvent::commit;
        }
    }

    @Nullable
    @Override
    public ProfiledDuration onChunkGenerate(ChunkCoordIntPair chunkPos, ResourceKey<World> world, String targetStatus) {
        if (!ChunkGenerationEvent.TYPE.isEnabled()) {
            return null;
        } else {
            ChunkGenerationEvent chunkGenerationEvent = new ChunkGenerationEvent(chunkPos, world, targetStatus);
            chunkGenerationEvent.begin();
            return chunkGenerationEvent::commit;
        }
    }
}
