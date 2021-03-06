package net.minecraft.server.players;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.UtilColor;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.storage.SavedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NameReferencingFileConverter {
    static final Logger LOGGER = LogManager.getLogger();
    public static final File OLD_IPBANLIST = new File("banned-ips.txt");
    public static final File OLD_USERBANLIST = new File("banned-players.txt");
    public static final File OLD_OPLIST = new File("ops.txt");
    public static final File OLD_WHITELIST = new File("white-list.txt");

    static List<String> readOldListFormat(File file, Map<String, String[]> valueMap) throws IOException {
        List<String> list = Files.readLines(file, StandardCharsets.UTF_8);

        for(String string : list) {
            string = string.trim();
            if (!string.startsWith("#") && string.length() >= 1) {
                String[] strings = string.split("\\|");
                valueMap.put(strings[0].toLowerCase(Locale.ROOT), strings);
            }
        }

        return list;
    }

    private static void lookupPlayers(MinecraftServer server, Collection<String> bannedPlayers, ProfileLookupCallback callback) {
        String[] strings = bannedPlayers.stream().filter((stringx) -> {
            return !UtilColor.isNullOrEmpty(stringx);
        }).toArray((i) -> {
            return new String[i];
        });
        if (server.getOnlineMode()) {
            server.getGameProfileRepository().findProfilesByNames(strings, Agent.MINECRAFT, callback);
        } else {
            for(String string : strings) {
                UUID uUID = EntityHuman.createPlayerUUID(new GameProfile((UUID)null, string));
                GameProfile gameProfile = new GameProfile(uUID, string);
                callback.onProfileLookupSucceeded(gameProfile);
            }
        }

    }

    public static boolean convertUserBanlist(MinecraftServer server) {
        final GameProfileBanList userBanList = new GameProfileBanList(PlayerList.USERBANLIST_FILE);
        if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
            if (userBanList.getFile().exists()) {
                try {
                    userBanList.load();
                } catch (IOException var6) {
                    LOGGER.warn("Could not load existing file {}", userBanList.getFile().getName(), var6);
                }
            }

            try {
                final Map<String, String[]> map = Maps.newHashMap();
                readOldListFormat(OLD_USERBANLIST, map);
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
                    public void onProfileLookupSucceeded(GameProfile gameProfile) {
                        server.getUserCache().add(gameProfile);
                        String[] strings = map.get(gameProfile.getName().toLowerCase(Locale.ROOT));
                        if (strings == null) {
                            NameReferencingFileConverter.LOGGER.warn("Could not convert user banlist entry for {}", (Object)gameProfile.getName());
                            throw new NameReferencingFileConverter.FileConversionException("Profile not in the conversionlist");
                        } else {
                            Date date = strings.length > 1 ? NameReferencingFileConverter.parseDate(strings[1], (Date)null) : null;
                            String string = strings.length > 2 ? strings[2] : null;
                            Date date2 = strings.length > 3 ? NameReferencingFileConverter.parseDate(strings[3], (Date)null) : null;
                            String string2 = strings.length > 4 ? strings[4] : null;
                            userBanList.add(new GameProfileBanEntry(gameProfile, date, string, date2, string2));
                        }
                    }

                    public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                        NameReferencingFileConverter.LOGGER.warn("Could not lookup user banlist entry for {}", gameProfile.getName(), exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new NameReferencingFileConverter.FileConversionException("Could not request user " + gameProfile.getName() + " from backend systems", exception);
                        }
                    }
                };
                lookupPlayers(server, map.keySet(), profileLookupCallback);
                userBanList.save();
                renameOldFile(OLD_USERBANLIST);
                return true;
            } catch (IOException var4) {
                LOGGER.warn("Could not read old user banlist to convert it!", (Throwable)var4);
                return false;
            } catch (NameReferencingFileConverter.FileConversionException var5) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)var5);
                return false;
            }
        } else {
            return true;
        }
    }

    public static boolean convertIpBanlist(MinecraftServer server) {
        IpBanList ipBanList = new IpBanList(PlayerList.IPBANLIST_FILE);
        if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
            if (ipBanList.getFile().exists()) {
                try {
                    ipBanList.load();
                } catch (IOException var11) {
                    LOGGER.warn("Could not load existing file {}", ipBanList.getFile().getName(), var11);
                }
            }

            try {
                Map<String, String[]> map = Maps.newHashMap();
                readOldListFormat(OLD_IPBANLIST, map);

                for(String string : map.keySet()) {
                    String[] strings = map.get(string);
                    Date date = strings.length > 1 ? parseDate(strings[1], (Date)null) : null;
                    String string2 = strings.length > 2 ? strings[2] : null;
                    Date date2 = strings.length > 3 ? parseDate(strings[3], (Date)null) : null;
                    String string3 = strings.length > 4 ? strings[4] : null;
                    ipBanList.add(new IpBanEntry(string, date, string2, date2, string3));
                }

                ipBanList.save();
                renameOldFile(OLD_IPBANLIST);
                return true;
            } catch (IOException var10) {
                LOGGER.warn("Could not parse old ip banlist to convert it!", (Throwable)var10);
                return false;
            }
        } else {
            return true;
        }
    }

    public static boolean convertOpsList(MinecraftServer server) {
        final OpList serverOpList = new OpList(PlayerList.OPLIST_FILE);
        if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
            if (serverOpList.getFile().exists()) {
                try {
                    serverOpList.load();
                } catch (IOException var6) {
                    LOGGER.warn("Could not load existing file {}", serverOpList.getFile().getName(), var6);
                }
            }

            try {
                List<String> list = Files.readLines(OLD_OPLIST, StandardCharsets.UTF_8);
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
                    public void onProfileLookupSucceeded(GameProfile gameProfile) {
                        server.getUserCache().add(gameProfile);
                        serverOpList.add(new OpListEntry(gameProfile, server.getOperatorUserPermissionLevel(), false));
                    }

                    public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                        NameReferencingFileConverter.LOGGER.warn("Could not lookup oplist entry for {}", gameProfile.getName(), exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new NameReferencingFileConverter.FileConversionException("Could not request user " + gameProfile.getName() + " from backend systems", exception);
                        }
                    }
                };
                lookupPlayers(server, list, profileLookupCallback);
                serverOpList.save();
                renameOldFile(OLD_OPLIST);
                return true;
            } catch (IOException var4) {
                LOGGER.warn("Could not read old oplist to convert it!", (Throwable)var4);
                return false;
            } catch (NameReferencingFileConverter.FileConversionException var5) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)var5);
                return false;
            }
        } else {
            return true;
        }
    }

    public static boolean convertWhiteList(MinecraftServer server) {
        final WhiteList userWhiteList = new WhiteList(PlayerList.WHITELIST_FILE);
        if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
            if (userWhiteList.getFile().exists()) {
                try {
                    userWhiteList.load();
                } catch (IOException var6) {
                    LOGGER.warn("Could not load existing file {}", userWhiteList.getFile().getName(), var6);
                }
            }

            try {
                List<String> list = Files.readLines(OLD_WHITELIST, StandardCharsets.UTF_8);
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
                    public void onProfileLookupSucceeded(GameProfile gameProfile) {
                        server.getUserCache().add(gameProfile);
                        userWhiteList.add(new WhiteListEntry(gameProfile));
                    }

                    public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                        NameReferencingFileConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", gameProfile.getName(), exception);
                        if (!(exception instanceof ProfileNotFoundException)) {
                            throw new NameReferencingFileConverter.FileConversionException("Could not request user " + gameProfile.getName() + " from backend systems", exception);
                        }
                    }
                };
                lookupPlayers(server, list, profileLookupCallback);
                userWhiteList.save();
                renameOldFile(OLD_WHITELIST);
                return true;
            } catch (IOException var4) {
                LOGGER.warn("Could not read old whitelist to convert it!", (Throwable)var4);
                return false;
            } catch (NameReferencingFileConverter.FileConversionException var5) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)var5);
                return false;
            }
        } else {
            return true;
        }
    }

    @Nullable
    public static UUID convertMobOwnerIfNecessary(MinecraftServer server, String name) {
        if (!UtilColor.isNullOrEmpty(name) && name.length() <= 16) {
            Optional<UUID> optional = server.getUserCache().getProfile(name).map(GameProfile::getId);
            if (optional.isPresent()) {
                return optional.get();
            } else if (!server.isEmbeddedServer() && server.getOnlineMode()) {
                final List<GameProfile> list = Lists.newArrayList();
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
                    public void onProfileLookupSucceeded(GameProfile gameProfile) {
                        server.getUserCache().add(gameProfile);
                        list.add(gameProfile);
                    }

                    public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                        NameReferencingFileConverter.LOGGER.warn("Could not lookup user whitelist entry for {}", gameProfile.getName(), exception);
                    }
                };
                lookupPlayers(server, Lists.newArrayList(name), profileLookupCallback);
                return !list.isEmpty() && list.get(0).getId() != null ? list.get(0).getId() : null;
            } else {
                return EntityHuman.createPlayerUUID(new GameProfile((UUID)null, name));
            }
        } else {
            try {
                return UUID.fromString(name);
            } catch (IllegalArgumentException var5) {
                return null;
            }
        }
    }

    public static boolean convertPlayers(DedicatedServer minecraftServer) {
        final File file = getPlayersFolder(minecraftServer);
        final File file2 = new File(file.getParentFile(), "playerdata");
        final File file3 = new File(file.getParentFile(), "unknownplayers");
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            List<String> list = Lists.newArrayList();

            for(File file4 : files) {
                String string = file4.getName();
                if (string.toLowerCase(Locale.ROOT).endsWith(".dat")) {
                    String string2 = string.substring(0, string.length() - ".dat".length());
                    if (!string2.isEmpty()) {
                        list.add(string2);
                    }
                }
            }

            try {
                final String[] strings = list.toArray(new String[list.size()]);
                ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
                    public void onProfileLookupSucceeded(GameProfile gameProfile) {
                        minecraftServer.getUserCache().add(gameProfile);
                        UUID uUID = gameProfile.getId();
                        if (uUID == null) {
                            throw new NameReferencingFileConverter.FileConversionException("Missing UUID for user profile " + gameProfile.getName());
                        } else {
                            this.movePlayerFile(file2, this.getFileNameForProfile(gameProfile), uUID.toString());
                        }
                    }

                    public void onProfileLookupFailed(GameProfile gameProfile, Exception exception) {
                        NameReferencingFileConverter.LOGGER.warn("Could not lookup user uuid for {}", gameProfile.getName(), exception);
                        if (exception instanceof ProfileNotFoundException) {
                            String string = this.getFileNameForProfile(gameProfile);
                            this.movePlayerFile(file3, string, string);
                        } else {
                            throw new NameReferencingFileConverter.FileConversionException("Could not request user " + gameProfile.getName() + " from backend systems", exception);
                        }
                    }

                    private void movePlayerFile(File playerDataFolder, String fileName, String uuid) {
                        File file = new File(file, fileName + ".dat");
                        File file2 = new File(playerDataFolder, uuid + ".dat");
                        NameReferencingFileConverter.ensureDirectoryExists(playerDataFolder);
                        if (!file.renameTo(file2)) {
                            throw new NameReferencingFileConverter.FileConversionException("Could not convert file for " + fileName);
                        }
                    }

                    private String getFileNameForProfile(GameProfile profile) {
                        String string = null;

                        for(String string2 : strings) {
                            if (string2 != null && string2.equalsIgnoreCase(profile.getName())) {
                                string = string2;
                                break;
                            }
                        }

                        if (string == null) {
                            throw new NameReferencingFileConverter.FileConversionException("Could not find the filename for " + profile.getName() + " anymore");
                        } else {
                            return string;
                        }
                    }
                };
                lookupPlayers(minecraftServer, Lists.newArrayList(strings), profileLookupCallback);
                return true;
            } catch (NameReferencingFileConverter.FileConversionException var12) {
                LOGGER.error("Conversion failed, please try again later", (Throwable)var12);
                return false;
            }
        } else {
            return true;
        }
    }

    static void ensureDirectoryExists(File directory) {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new NameReferencingFileConverter.FileConversionException("Can't create directory " + directory.getName() + " in world save directory.");
            }
        } else if (!directory.mkdirs()) {
            throw new NameReferencingFileConverter.FileConversionException("Can't create directory " + directory.getName() + " in world save directory.");
        }
    }

    public static boolean serverReadyAfterUserconversion(MinecraftServer server) {
        boolean bl = areOldUserlistsRemoved();
        return bl && areOldPlayersConverted(server);
    }

    private static boolean areOldUserlistsRemoved() {
        boolean bl = false;
        if (OLD_USERBANLIST.exists() && OLD_USERBANLIST.isFile()) {
            bl = true;
        }

        boolean bl2 = false;
        if (OLD_IPBANLIST.exists() && OLD_IPBANLIST.isFile()) {
            bl2 = true;
        }

        boolean bl3 = false;
        if (OLD_OPLIST.exists() && OLD_OPLIST.isFile()) {
            bl3 = true;
        }

        boolean bl4 = false;
        if (OLD_WHITELIST.exists() && OLD_WHITELIST.isFile()) {
            bl4 = true;
        }

        if (!bl && !bl2 && !bl3 && !bl4) {
            return true;
        } else {
            LOGGER.warn("**** FAILED TO START THE SERVER AFTER ACCOUNT CONVERSION!");
            LOGGER.warn("** please remove the following files and restart the server:");
            if (bl) {
                LOGGER.warn("* {}", (Object)OLD_USERBANLIST.getName());
            }

            if (bl2) {
                LOGGER.warn("* {}", (Object)OLD_IPBANLIST.getName());
            }

            if (bl3) {
                LOGGER.warn("* {}", (Object)OLD_OPLIST.getName());
            }

            if (bl4) {
                LOGGER.warn("* {}", (Object)OLD_WHITELIST.getName());
            }

            return false;
        }
    }

    private static boolean areOldPlayersConverted(MinecraftServer server) {
        File file = getPlayersFolder(server);
        if (!file.exists() || !file.isDirectory() || file.list().length <= 0 && file.delete()) {
            return true;
        } else {
            LOGGER.warn("**** DETECTED OLD PLAYER DIRECTORY IN THE WORLD SAVE");
            LOGGER.warn("**** THIS USUALLY HAPPENS WHEN THE AUTOMATIC CONVERSION FAILED IN SOME WAY");
            LOGGER.warn("** please restart the server and if the problem persists, remove the directory '{}'", (Object)file.getPath());
            return false;
        }
    }

    private static File getPlayersFolder(MinecraftServer server) {
        return server.getWorldPath(SavedFile.PLAYER_OLD_DATA_DIR).toFile();
    }

    private static void renameOldFile(File file) {
        File file2 = new File(file.getName() + ".converted");
        file.renameTo(file2);
    }

    static Date parseDate(String dateString, Date fallback) {
        Date date;
        try {
            date = ExpirableListEntry.DATE_FORMAT.parse(dateString);
        } catch (ParseException var4) {
            date = fallback;
        }

        return date;
    }

    static class FileConversionException extends RuntimeException {
        FileConversionException(String title, Throwable other) {
            super(title, other);
        }

        FileConversionException(String title) {
            super(title);
        }
    }
}
