package net.minecraft.sounds;

public class Musics {
    private static final int ONE_SECOND = 20;
    private static final int THIRTY_SECONDS = 600;
    private static final int TEN_MINUTES = 12000;
    private static final int TWENTY_MINUTES = 24000;
    private static final int FIVE_MINUTES = 6000;
    public static final Music MENU = new Music(SoundEffects.MUSIC_MENU, 20, 600, true);
    public static final Music CREATIVE = new Music(SoundEffects.MUSIC_CREATIVE, 12000, 24000, false);
    public static final Music CREDITS = new Music(SoundEffects.MUSIC_CREDITS, 0, 0, true);
    public static final Music END_BOSS = new Music(SoundEffects.MUSIC_DRAGON, 0, 0, true);
    public static final Music END = new Music(SoundEffects.MUSIC_END, 6000, 24000, true);
    public static final Music UNDER_WATER = createGameMusic(SoundEffects.MUSIC_UNDER_WATER);
    public static final Music GAME = createGameMusic(SoundEffects.MUSIC_GAME);

    public static Music createGameMusic(SoundEffect event) {
        return new Music(event, 12000, 24000, false);
    }
}