package net.minecraft.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.minecraft.SharedConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EULA {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Path file;
    private final boolean agreed;

    public EULA(Path eulaFile) {
        this.file = eulaFile;
        this.agreed = SharedConstants.IS_RUNNING_IN_IDE || this.readFile();
    }

    private boolean readFile() {
        try {
            InputStream inputStream = Files.newInputStream(this.file);

            boolean var3;
            try {
                Properties properties = new Properties();
                properties.load(inputStream);
                var3 = Boolean.parseBoolean(properties.getProperty("eula", "false"));
            } catch (Throwable var5) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var4) {
                        var5.addSuppressed(var4);
                    }
                }

                throw var5;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var3;
        } catch (Exception var6) {
            LOGGER.warn("Failed to load {}", (Object)this.file);
            this.saveDefaults();
            return false;
        }
    }

    public boolean hasAgreedToEULA() {
        return this.agreed;
    }

    private void saveDefaults() {
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            try {
                OutputStream outputStream = Files.newOutputStream(this.file);

                try {
                    Properties properties = new Properties();
                    properties.setProperty("eula", "false");
                    properties.store(outputStream, "By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).");
                } catch (Throwable var5) {
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (Throwable var4) {
                            var5.addSuppressed(var4);
                        }
                    }

                    throw var5;
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Exception var6) {
                LOGGER.warn("Failed to save {}", this.file, var6);
            }

        }
    }
}
