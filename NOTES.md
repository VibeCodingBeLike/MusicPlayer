# Never Music Mod - Technical Notes & Documentation

## Target Environment
- **Minecraft:** 26.2 (Fabric, official Mojang mappings, non-remapping Loom)
- **Java:** 25
- **Fabric Loader:** 0.20.1
- **Fabric API:** 0.161.0+26.2
- **Environment:** Client-only (`"environment": "client"`)
- **Mixin Configuration:** Compatibility level `JAVA_25`, no refmap, `injectors.defaultRequire = 1`

---

## 1. Source Analysis Findings (Stage 2)

### What decides when a new song starts, and which field holds the countdown?
In `net.minecraft.client.sounds.MusicManager`:
- The countdown field is `private int nextSongDelay` (initialized to `100` ticks).
- In `MusicManager.tick()`:
  - Each tick, if situational music is present (`this.minecraft.getSituationalMusic() != null`), `nextSongDelay` is clamped to `music.maxDelay()`:
    ```java
    this.nextSongDelay = Math.min(this.nextSongDelay, music.maxDelay());
    ```
  - When no music is currently playing (`this.currentMusic == null`), the client screen is not `LevelLoadingScreen`, and `--this.nextSongDelay <= 0`, a new song is started via `this.startPlaying(music)`.

### Every call site of `getNextSongDelay`:
1. `MusicManager.tick()` (line 57): Called when a song finishes playing (`!soundManager.isActive(currentMusic)`):
   ```java
   this.nextSongDelay = Math.min(this.nextSongDelay, this.gameMusicFrequency.getNextSongDelay(music, this.random));
   ```
2. `MusicManager.stopPlaying()` (line 107): Called when stopping a song:
   ```java
   this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random) + 100;
   ```
3. `MusicManager.setMinutesBetweenSongs(MusicFrequency)` (line 158): Called when changing the frequency setting in Options:
   ```java
   this.gameMusicFrequency = musicFrequency;
   this.nextSongDelay = this.gameMusicFrequency.getNextSongDelay(this.minecraft.getSituationalMusic(), this.random);
   ```

### What happens to a currently playing song when the setting changes?
- In `setMinutesBetweenSongs(musicFrequency)`, vanilla only updates `this.gameMusicFrequency` and `this.nextSongDelay`.
- Vanilla does **not** stop `this.currentMusic`. A song that is currently playing continues to play until it finishes on its own unless explicitly stopped.

---

## 2. Stage 4 Design Choice: Why Design A Was Chosen

### Design B (Injecting into `getNextSongDelay`):
Attempting to return a very large value (or `Integer.MAX_VALUE`) from `getNextSongDelay` fails for three distinct reasons:
1. **Delay clamping in `tick()`**: At line 61 of `MusicManager.tick()`, `this.nextSongDelay = Math.min(this.nextSongDelay, music.maxDelay())` runs every tick. Even if `nextSongDelay` were set to `Integer.MAX_VALUE`, it is immediately clamped down to `music.maxDelay()` (typically 12,000 to 24,000 ticks, i.e., 10-20 minutes). The countdown continues to decrement each tick until a song starts playing.
2. **Initial countdown bypass**: On client startup, `nextSongDelay` initializes to `100` ticks (5 seconds). `getNextSongDelay` is never called on startup, so the initial song plays after 5 seconds regardless of the setting.
3. **Mid-song setting change**: Changing the setting to `NEVER` while a song is playing does not stop the currently playing song in vanilla code.

### Design A (Injecting at `HEAD` of `MusicManager.tick`):
Design A was chosen because it reliably and cleanly prevents music playback:
- In `MusicManagerMixin`, we inject at `HEAD` of `tick()` with `cancellable = true`.
- If `this.gameMusicFrequency == NeverMusicMod.NEVER`:
  - If music is actively playing (`this.currentMusic != null`), it immediately calls `this.stopPlaying()`.
  - It cancels `ci.cancel()`, preventing `nextSongDelay` from decrementing, suppressing situational music checks, and ensuring `startPlaying` is never called.
- When switching back to another frequency, `setMinutesBetweenSongs` updates `gameMusicFrequency`, and subsequent ticks resume normal execution and delay scheduling.

---

## 3. Enum Extension & Exact `<clinit>` Target String (Stage 3)

### Exact Bytecode Inspection
In `net/minecraft/client/sounds/MusicManager$MusicFrequency.class`, the static initializer decompilation shows:
```bytecode
56: invokestatic  #129  // Method $values:()[Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;
59: putstatic     #42   // Field $VALUES:[Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;
62: invokedynamic #145  // StringRepresentable.fromEnum (values supplier)
70: putstatic     #151  // Field CODEC:Lcom/mojang/serialization/Codec;
73: return
```

### Exact Target Descriptor
```
Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;$VALUES:[Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;
```
Using:
- **`at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;$VALUES:[Lnet/minecraft/client/sounds/MusicManager$MusicFrequency;", shift = At.Shift.AFTER)`**

### Why this target is required:
Injecting with `shift = At.Shift.AFTER` on `PUTSTATIC` of `$VALUES` executes right after the vanilla array is assigned, but **before** `StringRepresentable.fromEnum(MusicFrequency::values)` is called at byte 67 to construct `CODEC`. Because `CODEC` queries `values()`, our appended `NEVER` constant is included in `CODEC`, allowing saving and loading `musicFrequency:"NEVER"` in `options.txt` without schema errors.

### Helper Class
Mixins cannot define new static fields on the target enum class. The instantiated `NEVER` constant is stored in `kat.blasted.music_player.NeverMusicMod.NEVER` for use by `MusicManagerMixin`.

---

## 4. Verification Checkpoints (Stage 6)

1. **Option Cycle Button**: `MusicFrequency.values().length == 4` confirmed at runtime (`DEFAULT` [0], `FREQUENT` [1], `CONSTANT` [2], `NEVER` [3]). Button cycles correctly in Options > Music & Sounds > Music Frequency.
2. **Lang File**: `options.music_frequency.never` is localized as `"Never"` in `assets/music_player/lang/en_us.json`.
3. **Codec / Persistence**: Verified by launching the client with `musicFrequency:"NEVER"` in `run/options.txt`. The option loads cleanly as `NEVER` without warnings or resets.
4. **Behavior**: While `NEVER` is active, `tick()` cancels at `HEAD`, preventing countdowns and music from starting across all biomes and screens. If changed mid-song, `stopPlaying()` stops the active music immediately.

---

## 5. Explicit Assumptions
- No other client mod introduces an incompatible overwrite of `MusicManager.tick()` that discards mixin cancellations at `HEAD`.
- Vanilla continues to load client language assets from `<modid>/lang/<lang>.json` via Fabric's resource manager (verified working).
