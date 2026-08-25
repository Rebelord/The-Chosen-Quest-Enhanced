package thechosenquest.desktop;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

/**
 * Central, non-blocking sound-effects service.
 *
 * The first release uses cached procedural samples so the game has useful
 * feedback without relying on operating-system sounds. Bundled WAV files can
 * replace individual cues later without changing any gameplay call sites.
 */
final class SoundManager {
    /** Authored music states keep scene changes expressive without gameplay call-site noise. */
    enum Music {
        NONE(null),
        TITLE("/assets/audio/title-theme.wav"),
        EXPLORATION("/assets/audio/exploration-theme.wav"),
        TAVERN("/assets/audio/tavern-theme.wav"),
        COMBAT("/assets/audio/combat-theme.wav"),
        BOSS("/assets/audio/boss-theme.wav");

        final String resource;

        Music(String resource) { this.resource = resource; }
    }

    enum Ambience {
        NONE, FOREST, LAKESHORE, CRYPT, MARKET, FORGE, ALCHEMIST, CAMPFIRE, TAVERN,
        COMBAT, DRAGON
    }

    enum Cue {
        UI_CONFIRM(new double[] {660, 880}, new int[] {55, 65}, false,
            "/assets/audio/sfx/ui-confirm.wav"),
        UI_CANCEL(new double[] {330, 220}, new int[] {65, 85}, false,
            "/assets/audio/sfx/ui-cancel.wav"),
        DICE_ROLL(new double[] {190, 145, 225, 165, 205},
            new int[] {34, 30, 38, 32, 58}, true),
        RACE_HUMAN(new double[] {392, 523}, new int[] {75, 115}, false),
        RACE_DWARF(new double[] {147, 196}, new int[] {105, 130}, true),
        RACE_ELF(new double[] {659, 988, 1319}, new int[] {55, 65, 120}, false),
        RACE_HALFLING(new double[] {587, 784, 698}, new int[] {45, 50, 85}, false),
        CLASS_FIGHTER(new double[] {220, 330}, new int[] {65, 125}, true),
        CLASS_MAGE(new double[] {440, 659, 988}, new int[] {60, 70, 130}, false),
        CLASS_ROGUE(new double[] {523, 392, 294}, new int[] {40, 45, 80}, true),
        CLASS_HUNTER(new double[] {392, 587}, new int[] {75, 135}, false),
        PATH_SELECT(new double[] {330, 494, 659}, new int[] {45, 55, 95}, false),
        ADVENTURE_BEGIN(new double[] {392, 523, 659, 784, 1047},
            new int[] {70, 70, 85, 105, 220}, false),
        ERROR(new double[] {180, 145}, new int[] {90, 130}, true,
            "/assets/audio/sfx/error.wav"),
        MOVE(new double[] {150}, new int[] {65}, true),
        EQUIP(new double[] {480, 720}, new int[] {70, 100}, false,
            "/assets/audio/sfx/equip.wav"),
        PURCHASE(new double[] {880, 1175, 1450}, new int[] {45, 45, 70}, false,
            "/assets/audio/sfx/purchase.wav"),
        ATTACK(new double[] {280, 120}, new int[] {70, 85}, true,
            "/assets/audio/sfx/attack.wav"),
        SHIELD_BASH(new double[] {135, 92, 182}, new int[] {55, 85, 95}, true),
        HEAVY_STRIKE(new double[] {205, 118, 72}, new int[] {65, 95, 130}, true),
        ROGUE_STRIKE(new double[] {620, 410, 255}, new int[] {34, 40, 72}, true),
        PINNING_SHOT(new double[] {780, 440, 196}, new int[] {35, 48, 90}, true),
        WEAPON_MASTERY(new double[] {330, 495, 742}, new int[] {50, 55, 105}, false),
        EXECUTE(new double[] {185, 128, 82, 55}, new int[] {45, 55, 75, 125}, true),
        HUNTERS_MARK(new double[] {392, 587, 880}, new int[] {45, 55, 110}, false),
        ENEMY_HIT(new double[] {165, 105}, new int[] {45, 95}, true,
            "/assets/audio/sfx/enemy-hit.wav"),
        PLAYER_HIT(new double[] {110}, new int[] {120}, true,
            "/assets/audio/sfx/player-hit.wav"),
        DEFEND(new double[] {240, 360}, new int[] {75, 85}, false,
            "/assets/audio/sfx/defend.wav"),
        SPELL(new double[] {420, 630, 945}, new int[] {70, 75, 120}, false,
            "/assets/audio/sfx/spell.wav"),
        MAGIC_MISSILE(new double[] {520, 780, 1040}, new int[] {42, 46, 95}, false),
        FIREBALL(new double[] {165, 220, 110}, new int[] {75, 105, 150}, true),
        ICE_SPIKE(new double[] {880, 1175, 660}, new int[] {35, 55, 125}, false),
        HEAL(new double[] {520, 660, 790}, new int[] {75, 75, 130}, false,
            "/assets/audio/sfx/heal.wav"),
        REST(new double[] {440, 554, 659}, new int[] {140, 140, 220}, false,
            "/assets/audio/sfx/rest.wav"),
        FLEE(new double[] {420, 300, 190}, new int[] {60, 65, 100}, true),
        ENEMY_DEFEATED(new double[] {520, 660, 880}, new int[] {75, 75, 160}, false),
        LOOT_DISCOVERED(new double[] {659, 784, 988}, new int[] {45, 55, 115}, false),
        UNCOMMON_LOOT(new double[] {659, 880, 1175, 1397},
            new int[] {45, 55, 75, 150}, false),
        RELIC_DISCOVERED(new double[] {523, 659, 784, 1047, 1319},
            new int[] {75, 75, 85, 120, 260}, false),
        RELIC_IDENTIFIED(new double[] {392, 523, 659, 784, 1047},
            new int[] {80, 80, 90, 110, 240}, false),
        RELIC_EQUIPPED(new double[] {523, 784, 1047, 1319, 1568},
            new int[] {55, 65, 85, 115, 250}, false),
        VICTORY(new double[] {523, 659, 784, 1047}, new int[] {120, 120, 140, 300}, false),
        DEFEAT(new double[] {392, 294, 196}, new int[] {180, 190, 330}, false),
        DRAGON_ROAR(new double[] {92, 73, 58}, new int[] {220, 250, 380}, true,
            "/assets/audio/sfx/dragon-roar.wav");

        final double[] frequencies;
        final int[] durations;
        final boolean noisy;
        final String resource;

        Cue(double[] frequencies, int[] durations, boolean noisy) {
            this(frequencies, durations, noisy, null);
        }

        Cue(double[] frequencies, int[] durations, boolean noisy, String resource) {
            this.frequencies = frequencies;
            this.durations = durations;
            this.noisy = noisy;
            this.resource = resource;
        }
    }

    private static final int SAMPLE_RATE = 22050;
    private static final String MASTER_KEY = "audio.master";
    private static final String MUSIC_KEY = "audio.music";
    private static final String AMBIENCE_KEY = "audio.ambience";
    private static final String EFFECTS_KEY = "audio.effects";
    private static final String MUTED_KEY = "audio.muted";
    private static final String FOCUS_KEY = "audio.muteWhenUnfocused";

    private final Preferences preferences;
    private final Map<Cue, byte[]> sampleCache = new EnumMap<Cue, byte[]>(Cue.class);
    private final Map<Ambience, byte[]> ambienceCache =
        new EnumMap<Ambience, byte[]>(Ambience.class);
    private final ExecutorService player = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "chosen-quest-audio");
            thread.setDaemon(true);
            return thread;
        }
    });
    // Character creation is intentionally exploratory. Keep only the latest
    // queued choice chime so rapid clicks never produce seconds of stale audio.
    private final ThreadPoolExecutor selectionPlayer = new ThreadPoolExecutor(1, 1, 0L,
        TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1), new ThreadFactory() {
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "chosen-quest-selection-audio");
                thread.setDaemon(true);
                return thread;
            }
        }, new ThreadPoolExecutor.DiscardOldestPolicy());
    private final ExecutorService ambiencePlayer =
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "chosen-quest-ambience");
                thread.setDaemon(true);
                return thread;
            }
        });

    private volatile int masterVolume;
    private volatile int musicVolume;
    private volatile int ambienceVolume;
    private volatile int effectsVolume;
    private volatile boolean muted;
    private volatile boolean muteWhenUnfocused;
    private volatile boolean focusSuspended;
    private volatile boolean audioAvailable = true;
    private volatile boolean musicAvailable = true;
    private volatile boolean ambienceAvailable = true;
    private volatile Music requestedMusic = Music.NONE;
    private volatile boolean ambienceThreadStarted;
    private volatile boolean shuttingDown;
    private volatile int ambienceRevision;
    private volatile Ambience requestedAmbience = Ambience.NONE;
    private Clip musicClip;

    SoundManager() {
        Preferences loaded = null;
        int master = 75;
        int music = 60;
        int ambience = 45;
        int effects = 80;
        boolean savedMuted = false;
        // Keep title music audible when the game is launched from a terminal or
        // another application that temporarily retains focus. Players can still
        // opt into focus muting from Game Settings.
        boolean savedFocus = false;
        try {
            loaded = Preferences.userNodeForPackage(SoundManager.class);
            master = loaded.getInt(MASTER_KEY, master);
            music = loaded.getInt(MUSIC_KEY, music);
            ambience = loaded.getInt(AMBIENCE_KEY, ambience);
            effects = loaded.getInt(EFFECTS_KEY, effects);
            savedMuted = loaded.getBoolean(MUTED_KEY, savedMuted);
            savedFocus = loaded.getBoolean(FOCUS_KEY, savedFocus);
        } catch (RuntimeException ignored) {
            loaded = null;
        }
        preferences = loaded;
        masterVolume = clamp(master);
        musicVolume = clamp(music);
        ambienceVolume = clamp(ambience);
        effectsVolume = clamp(effects);
        muted = savedMuted;
        muteWhenUnfocused = savedFocus;
        for (Cue cue : Cue.values()) {
            sampleCache.put(cue, loadCueSample(cue));
        }
        for (Ambience ambienceType : Ambience.values()) {
            if (ambienceType != Ambience.NONE) {
                ambienceCache.put(ambienceType, synthesizeAmbience(ambienceType));
            }
        }
    }

    void play(final Cue cue) {
        if (cue == null || muted || (muteWhenUnfocused && focusSuspended)
                || !audioAvailable || GraphicsEnvironment.isHeadless()) {
            return;
        }
        final byte[] source = sampleCache.get(cue);
        if (source == null) return;
        player.execute(new Runnable() {
            public void run() {
                if (muted || (muteWhenUnfocused && focusSuspended)) return;
                playPcm(scale(source, effectiveVolume()));
            }
        });
    }

    void playSelection(final Cue cue) {
        if (cue == null || muted || (muteWhenUnfocused && focusSuspended)
                || !audioAvailable || GraphicsEnvironment.isHeadless()) return;
        final byte[] source = sampleCache.get(cue);
        if (source == null) return;
        selectionPlayer.execute(new Runnable() {
            public void run() {
                if (muted || (muteWhenUnfocused && focusSuspended)) return;
                playPcm(scale(source, effectiveVolume()));
            }
        });
    }

    int getMasterVolume() { return masterVolume; }
    int getMusicVolume() { return musicVolume; }
    int getAmbienceVolume() { return ambienceVolume; }
    int getEffectsVolume() { return effectsVolume; }
    boolean isMuted() { return muted; }
    boolean isMuteWhenUnfocused() { return muteWhenUnfocused; }
    boolean isAudioAvailable() { return audioAvailable; }
    boolean isMusicAvailable() { return musicAvailable; }
    boolean isAmbienceAvailable() { return ambienceAvailable; }

    synchronized boolean isTitleMusicPlaying() {
        return requestedMusic == Music.TITLE && musicClip != null && musicClip.isRunning();
    }

    synchronized Music requestedMusicForTest() { return requestedMusic; }

    static String musicResource(Music music) {
        return music == null ? null : music.resource;
    }

    void setMasterVolume(int value) {
        masterVolume = clamp(value);
        saveInt(MASTER_KEY, masterVolume);
        refreshMusicState();
    }

    void setMusicVolume(int value) {
        musicVolume = clamp(value);
        saveInt(MUSIC_KEY, musicVolume);
        refreshMusicState();
    }

    void setAmbienceVolume(int value) {
        ambienceVolume = clamp(value);
        saveInt(AMBIENCE_KEY, ambienceVolume);
    }

    void setEffectsVolume(int value) {
        effectsVolume = clamp(value);
        saveInt(EFFECTS_KEY, effectsVolume);
    }

    void setMuted(boolean value) {
        muted = value;
        saveBoolean(MUTED_KEY, value);
        refreshMusicState();
    }

    boolean toggleMuted() {
        setMuted(!muted);
        return muted;
    }

    void setMuteWhenUnfocused(boolean value) {
        muteWhenUnfocused = value;
        saveBoolean(FOCUS_KEY, value);
        refreshMusicState();
    }

    void setFocusSuspended(boolean value) {
        focusSuspended = value;
        refreshMusicState();
    }

    int cachedCueCount() {
        return sampleCache.size();
    }

    int fileBackedCueCount() {
        int count = 0;
        for (Cue cue : Cue.values()) if (cue.resource != null) count++;
        return count;
    }

    static String cueResource(Cue cue) { return cue == null ? null : cue.resource; }

    int cachedSampleLengthForTest(Cue cue) {
        byte[] sample = sampleCache.get(cue);
        return sample == null ? 0 : sample.length;
    }

    int cachedAmbienceCount() {
        return ambienceCache.size();
    }

    static Cue cueForRace(String race) {
        if ("Dwarf".equals(race)) return Cue.RACE_DWARF;
        if ("Elf".equals(race)) return Cue.RACE_ELF;
        if ("Halfling".equals(race)) return Cue.RACE_HALFLING;
        return Cue.RACE_HUMAN;
    }

    static Cue cueForClass(String heroClass) {
        if ("Mage".equals(heroClass)) return Cue.CLASS_MAGE;
        if ("Rogue".equals(heroClass)) return Cue.CLASS_ROGUE;
        if ("Hunter".equals(heroClass)) return Cue.CLASS_HUNTER;
        return Cue.CLASS_FIGHTER;
    }

    synchronized void setAmbience(Ambience ambience) {
        Ambience next = ambience == null ? Ambience.NONE : ambience;
        if (requestedAmbience == next) return;
        requestedAmbience = next;
        ambienceRevision++;
        if (next != Ambience.NONE && !GraphicsEnvironment.isHeadless()) {
            ensureAmbienceThread();
        }
    }

    void stopAmbience() {
        setAmbience(Ambience.NONE);
    }

    void shutdown() {
        shuttingDown = true;
        stopAmbience();
        synchronized (this) {
            requestedMusic = Music.NONE;
            if (musicClip != null) {
                musicClip.stop();
                musicClip.close();
                musicClip = null;
            }
        }
        player.shutdownNow();
        selectionPlayer.shutdownNow();
        ambiencePlayer.shutdownNow();
    }

    synchronized void playTitleMusic() {
        setMusic(Music.TITLE);
    }

    synchronized void setMusic(Music music) {
        Music next = music == null ? Music.NONE : music;
        if (requestedMusic != next) {
            requestedMusic = next;
            if (musicClip != null) {
                musicClip.stop();
                musicClip.close();
                musicClip = null;
            }
            // A line or asset can be unavailable transiently. A later scene
            // change retries instead of disabling music for the whole session.
            musicAvailable = true;
        }
        refreshMusicState();
    }

    void stopMusic() { setMusic(Music.NONE); }

    private float effectiveVolume() {
        return (masterVolume / 100.0f) * (effectsVolume / 100.0f);
    }

    private float effectiveAmbienceVolume() {
        if (muted || (muteWhenUnfocused && focusSuspended)) return 0.0f;
        return (masterVolume / 100.0f) * (ambienceVolume / 100.0f);
    }

    private synchronized void ensureAmbienceThread() {
        if (ambienceThreadStarted || shuttingDown) return;
        ambienceThreadStarted = true;
        ambienceAvailable = true;
        ambiencePlayer.execute(new Runnable() {
            public void run() { runAmbienceLoop(); }
        });
    }

    private void runAmbienceLoop() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = null;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, SAMPLE_RATE / 2);
            line.start();
            final int framesPerChunk = 1024;
            final float fadeStep = 1.0f / (SAMPLE_RATE * 1.2f);
            byte[] output = new byte[framesPerChunk * 2];
            byte[] from = null;
            byte[] to = null;
            int fromPosition = 0;
            int toPosition = 0;
            int revision = -1;
            float fade = 1.0f;

            while (!shuttingDown) {
                if (revision != ambienceRevision) {
                    from = to;
                    fromPosition = toPosition;
                    to = ambienceCache.get(requestedAmbience);
                    toPosition = 0;
                    fade = 0.0f;
                    revision = ambienceRevision;
                }
                float gain = effectiveAmbienceVolume();
                for (int frameIndex = 0; frameIndex < framesPerChunk; frameIndex++) {
                    short first = pcmSample(from, fromPosition);
                    short second = pcmSample(to, toPosition);
                    float mixed = first * (1.0f - fade) + second * fade;
                    int sample = Math.round(mixed * gain);
                    sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
                    int byteIndex = frameIndex * 2;
                    output[byteIndex] = (byte) (sample & 0xff);
                    output[byteIndex + 1] = (byte) ((sample >>> 8) & 0xff);
                    fromPosition = advancePcm(from, fromPosition);
                    toPosition = advancePcm(to, toPosition);
                    fade = Math.min(1.0f, fade + fadeStep);
                }
                line.write(output, 0, output.length);
            }
        } catch (Exception unavailable) {
            ambienceAvailable = false;
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            ambienceThreadStarted = false;
        }
    }

    private static short pcmSample(byte[] samples, int position) {
        if (samples == null || samples.length < 2) return 0;
        int safe = Math.max(0, Math.min(samples.length - 2, position));
        return (short) ((samples[safe] & 0xff) | (samples[safe + 1] << 8));
    }

    private static int advancePcm(byte[] samples, int position) {
        if (samples == null || samples.length < 2) return 0;
        int next = position + 2;
        return next >= samples.length ? 0 : next;
    }

    private synchronized void refreshMusicState() {
        if (GraphicsEnvironment.isHeadless()) return;
        boolean shouldPlay = requestedMusic != Music.NONE && !muted &&
            !(muteWhenUnfocused && focusSuspended) && masterVolume > 0 && musicVolume > 0;
        if (!shouldPlay) {
            if (musicClip != null && musicClip.isRunning()) musicClip.stop();
            return;
        }
        ensureMusicClip();
        if (musicClip == null) return;
        applyMusicGain();
        if (!musicClip.isRunning()) {
            if (musicClip.getFramePosition() >= musicClip.getFrameLength() - 1) {
                musicClip.setFramePosition(0);
            }
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void ensureMusicClip() {
        if (musicClip != null || !musicAvailable || requestedMusic == Music.NONE) return;
        AudioInputStream stream = null;
        try {
            java.net.URL resource = SoundManager.class.getResource(requestedMusic.resource);
            // Preserve audible feedback if an optional scene track is missing
            // from a development build; the title theme is the stable fallback.
            if (resource == null && requestedMusic != Music.TITLE) {
                resource = SoundManager.class.getResource(Music.TITLE.resource);
            }
            if (resource == null) throw new java.io.IOException("Music resource unavailable");
            stream = AudioSystem.getAudioInputStream(resource);
            musicClip = AudioSystem.getClip();
            musicClip.open(stream);
        } catch (Exception unavailable) {
            musicAvailable = false;
            if (musicClip != null) musicClip.close();
            musicClip = null;
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    private void applyMusicGain() {
        if (musicClip == null || !musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
        double linear = Math.max(0.0001,
            (masterVolume / 100.0) * (musicVolume / 100.0));
        float decibels = (float) (20.0 * Math.log10(linear));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private void playPcm(byte[] samples) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = null;
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, Math.min(samples.length, SAMPLE_RATE));
            line.start();
            line.write(samples, 0, samples.length);
            line.drain();
        } catch (Exception unavailable) {
            audioAvailable = false;
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
        }
    }

    /** Decode short authored WAVs once; synthesis remains a zero-dependency fallback. */
    private byte[] loadCueSample(Cue cue) {
        if (cue.resource == null) return synthesize(cue);
        AudioInputStream stream = null;
        try {
            java.net.URL resource = SoundManager.class.getResource(cue.resource);
            if (resource == null) return synthesize(cue);
            stream = AudioSystem.getAudioInputStream(resource);
            AudioFormat format = stream.getFormat();
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding()) ||
                    Math.round(format.getSampleRate()) != SAMPLE_RATE ||
                    format.getSampleSizeInBits() != 16 || format.getChannels() != 1 ||
                    format.isBigEndian()) {
                return synthesize(cue);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                if (read > 0) output.write(buffer, 0, read);
            }
            byte[] decoded = output.toByteArray();
            return decoded.length == 0 ? synthesize(cue) : decoded;
        } catch (Exception unavailable) {
            return synthesize(cue);
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception ignored) { }
            }
        }
    }

    private byte[] synthesize(Cue cue) {
        int sampleCount = 0;
        for (int duration : cue.durations) {
            sampleCount += SAMPLE_RATE * duration / 1000;
        }
        byte[] result = new byte[sampleCount * 2];
        int position = 0;
        long noiseState = 7919L + cue.ordinal() * 104729L;
        double filteredNoise = 0.0;
        for (int segment = 0; segment < cue.frequencies.length; segment++) {
            int count = SAMPLE_RATE * cue.durations[segment] / 1000;
            double frequency = cue.frequencies[segment];
            for (int i = 0; i < count; i++) {
                double time = i / (double) SAMPLE_RATE;
                double envelope = Math.min(1.0, i / (SAMPLE_RATE * 0.004));
                envelope *= Math.min(1.0, (count - i) / (SAMPLE_RATE * 0.035));
                double wave = Math.sin(2.0 * Math.PI * frequency * time);
                double triangle = (2.0 / Math.PI) * Math.asin(wave);
                double pulse = wave > 0.30 ? 1.0 : -0.55;
                double tone = triangle * 0.72 + pulse * 0.18 +
                    Math.sin(2.0 * Math.PI * frequency * 2.0 * time) * 0.10;
                if (cue.noisy) {
                    noiseState = (noiseState * 1103515245L + 12345L) & 0x7fffffffL;
                    double noise = (noiseState / (double) 0x7fffffffL) * 2.0 - 1.0;
                    filteredNoise = filteredNoise * 0.78 + noise * 0.22;
                    tone = tone * 0.80 + filteredNoise * 0.20;
                }
                short sample = (short) (Math.max(-1.0, Math.min(1.0, tone * 0.30 * envelope))
                    * Short.MAX_VALUE);
                result[position++] = (byte) (sample & 0xff);
                result[position++] = (byte) ((sample >>> 8) & 0xff);
            }
        }
        return result;
    }

    private byte[] synthesizeAmbience(Ambience ambience) {
        int count = SAMPLE_RATE * 5;
        byte[] result = new byte[count * 2];
        long noiseState = 104729L + ambience.ordinal() * 13007L;
        double filteredNoise = 0.0;
        for (int i = 0; i < count; i++) {
            noiseState = (noiseState * 1103515245L + 12345L) & 0x7fffffffL;
            double noise = (noiseState / (double) 0x7fffffffL) * 2.0 - 1.0;
            filteredNoise = filteredNoise * 0.992 + noise * 0.008;
            double time = i / (double) SAMPLE_RATE;
            double value;
            switch (ambience) {
                case FOREST:
                    value = filteredNoise * 0.12 +
                        retroArpeggio(time, new double[] {196, 247, 294}, 0.72) * 0.018;
                    value += chirp(time, 1.15, 920) * 0.020;
                    break;
                case LAKESHORE:
                    value = filteredNoise * 0.14 +
                        retroArpeggio(time, new double[] {147, 196, 247}, 0.95) * 0.018;
                    break;
                case CRYPT:
                    value = filteredNoise * 0.09 +
                        retroArpeggio(time, new double[] {110, 131, 155}, 1.10) * 0.024;
                    value += drip(time, 1.37) * 0.040;
                    break;
                case MARKET:
                    value = filteredNoise * 0.10 +
                        retroArpeggio(time, new double[] {196, 247, 330}, 0.58) * 0.020;
                    break;
                case FORGE:
                    value = fire(noise, filteredNoise) * 0.45 + hammer(time, 1.18) * 0.065;
                    break;
                case ALCHEMIST:
                    value = filteredNoise * 0.07 + drip(time, 0.83) * 0.030 +
                        retroArpeggio(time, new double[] {220, 277, 330}, 0.82) * 0.018;
                    break;
                case CAMPFIRE:
                    value = fire(noise, filteredNoise) * 0.42 +
                        retroArpeggio(time, new double[] {147, 196, 220}, 1.15) * 0.012;
                    break;
                case TAVERN:
                    value = fire(noise, filteredNoise) * 0.25 +
                        retroArpeggio(time, new double[] {131, 165, 196, 247}, 0.68) * 0.025;
                    break;
                case COMBAT:
                    value = filteredNoise * 0.05 + triangleWave(time, 55) *
                        (0.026 + 0.012 * Math.sin(time * Math.PI * 2 * 1.4));
                    break;
                case DRAGON:
                    value = filteredNoise * 0.12 + triangleWave(time, 38) * 0.052 +
                        triangleWave(time, 57) * 0.018;
                    break;
                default:
                    value = 0.0;
            }
            short sample = (short) (Math.max(-1.0, Math.min(1.0, value)) * Short.MAX_VALUE);
            int position = i * 2;
            result[position] = (byte) (sample & 0xff);
            result[position + 1] = (byte) ((sample >>> 8) & 0xff);
        }
        return result;
    }

    private static double chirp(double time, double period, double frequency) {
        double phase = time % period;
        if (phase > 0.16) return 0.0;
        double envelope = Math.sin(Math.PI * phase / 0.16);
        return triangleWave(time, frequency + phase * 900) * envelope;
    }

    private static double drip(double time, double period) {
        double phase = time % period;
        if (phase > 0.10) return 0.0;
        return Math.sin(2.0 * Math.PI * 780 * time) * Math.exp(-phase * 42.0);
    }

    private static double fire(double noise, double filteredNoise) {
        double crackle = Math.abs(noise) > 0.982 ? noise * 0.11 : 0.0;
        return filteredNoise * 0.14 + crackle;
    }

    private static double hammer(double time, double period) {
        double phase = time % period;
        if (phase > 0.12) return 0.0;
        return (triangleWave(time, 185) + 0.35 * triangleWave(time, 82)) *
            Math.exp(-phase * 32.0);
    }

    private static double retroArpeggio(double time, double[] notes, double stepLength) {
        int step = ((int) (time / stepLength)) % notes.length;
        double phase = time % stepLength;
        double envelope = 0.28 + 0.72 * Math.exp(-phase * 3.2);
        return triangleWave(time, notes[step]) * envelope;
    }

    private static double triangleWave(double time, double frequency) {
        return (2.0 / Math.PI) * Math.asin(
            Math.sin(2.0 * Math.PI * frequency * time));
    }

    private byte[] scale(byte[] source, float gain) {
        byte[] result = new byte[source.length];
        for (int i = 0; i < source.length; i += 2) {
            int raw = (source[i] & 0xff) | (source[i + 1] << 8);
            short value = (short) raw;
            int scaled = Math.round(value * gain);
            scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, scaled));
            result[i] = (byte) (scaled & 0xff);
            result[i + 1] = (byte) ((scaled >>> 8) & 0xff);
        }
        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private void saveInt(String key, int value) {
        if (preferences == null) return;
        try { preferences.putInt(key, value); } catch (RuntimeException ignored) { }
    }

    private void saveBoolean(String key, boolean value) {
        if (preferences == null) return;
        try { preferences.putBoolean(key, value); } catch (RuntimeException ignored) { }
    }
}
