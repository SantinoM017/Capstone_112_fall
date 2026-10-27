# Capstone_112_fall

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

### AI Usage

This project has used AI tools and agents to assist in the development of this project. 

- Gemini Flash:
    - Used to help structure the project and provide help in learning libGDX, Box2D, and AshleyECS API's
    - Writing prompts for copilot/claude
    - Enhancing code written by Copilot
    - Helped create SlopeFrictionSystem and Tangent-based movement on slopes (with copilot)
    - Helped create One way platform contact system
    - Helped code the dodge system
- Junie/Github Copilot:
    - Used to help write code and provide suggestions for code completion and refactoring.
    - Used to help explain parts of the code and to explain any questions or issues I am having with the code
    - Used to debug code
    - Used to write repetitive parts of my code
    - Helped debug the AttackSystem and PlayerInputSystem
    - I refactored the player entity userData to refer to the entity itself instead of the grounded component. I asked the AI to refactor all uses of the grounded component as userData to use the entity itself from userData
    - Ran out of inline suggestion on 10/07/2026
    - Fixed a bug that made player attack hitboxes change player mass
    - Change kinematic enemies to dynamic enemies if they are knock backed
    - Created most of the UI screens
    - Created the checkpoint/save system from my rough outline
    - Created the audio/sfx system
    - Created the music system
    - Added background images
- Claude (built-in Agent):
    - Used to generate code or provide suggestions.
    - Used to refactor large/complex parts of the codebase.
    - Used to create the tiled to java parser
    - Ran out of credits very quickly, so switched to CoPilot

Important prompts:
- I am in a second year comp sci class, and for my intended capstone project I am using libgdx with ashley, box2d, etc. to build a 2d platformer/boss fighting game with the help of AI and AI agents. I have already worked with libgdx building a simple 2d platformer game without any external extensions or packs other than tiled with a working physics system and block behavior system using composition (similar to ECS in a way) but no entities. The main goal is to obviously complete the project by december but to also actually understand what I'm doing. (to Gemini)
- can you refactor mapbuilder to convert both objects and tiles into values that are passed to enititymanager (Claude)
- what is causing the player to slow down when walking up slopes (CoPilot. It derived a tangent-based solution)
- ok can you refactor all the getUserData so that it uses the entity instead of looking for groundedComponent. (CoPilot)
- how to make fall through platforms (You can step on them but can jump through them or fall through them) (Gemini)
- Refactor my `knockbackSystem` and entity update logic to support knocking back Kinematic bodies in my LibGDX/Ashley ECS platformer.

    Please generate the following updates:

  1. Update `knockbackSystem`:
     Before calling `applyLinearImpulse`, check if `affectedBody.body.getType() == BodyDef.BodyType.KinematicBody`. If it is, store a flag (or track the original type in `Box2DComponent`) and set the body type to `BodyDef.BodyType.DynamicBody`. This allows the body to react to the impulse and Box2D gravity.

  2. Create a reversion method (`checkAndRevertKinematicState`):
     Generate a method to run in my AI or State system when `health.invulnerabilitytimer <= 0`. It should check if the body was temporarily made Dynamic.

  3. Ground Snapping Logic (Crucial):
     To prevent the entity from freezing in mid-air or sinking into the floor when switching back to Kinematic, the reversion method must:
  - Check if the entity's Y velocity is at or near 0 (falling has stopped).
  - Perform a short Box2D raycast directly downwards from the body's center (e.g., `rayLength = halfHeight + 0.1f`).
  - If the raycast hits static ground/terrain, snap the body's Y position exactly to `hitPoint.y + halfHeight`.
  - Zero out the linear velocity.
  - Finally, set the body type back to `BodyDef.BodyType.KinematicBody`.

    Please provide the updated `knockbackSystem` code and the new reversion logic with the Box2D raycast callback.
    (prompt created by Gemini sent to CoPilot)
- Please refactor `LevelSelectScreen`, update level progression persistence, and implement a new `MainMenuScreen` for my 2D LibGDX / Ashley platformer project according to the following requirements:

1. Level Path Routing in LevelSelectScreen:
    - Update `LevelSelectScreen` so clicking an unlocked level button launches the game with that specific stage.
    - Pass the selected map path (e.g., "maps/level_1.tmx", "maps/level_2.tmx") directly into the `Main` game screen constructor (`new Main(game, levelPath)`).

2. Level Progression Persistence via Preferences:
    - Create or integrate a dedicated `SaveManager` utility class using LibGDX `Gdx.app.getPreferences("MyGameSaveData")`.
    - Store the highest unlocked level index (defaulting to level 1).
    - In `LevelSelectScreen`, read this value dynamically so `UNLOCKED_LEVELS` reflects actual saved progress.
    - Add a static method `unlockNextLevel(int completedLevel)` to update and flush preferences when a level is cleared.

3. MainMenuScreen Implementation:
    - Create a new `MainMenuScreen` implementing LibGDX `Screen` using Scene2D (`Stage` and `FitViewport(800, 480)`).
    - Include the following menu options styled with my existing `Skin`:
        * "Play" / "Select Level": Navigates to `LevelSelectScreen`.
        * "Reset Progress": Resets saved level preferences back to level 1 (with a brief confirmation popup/dialog or toggle state) and flushes `Preferences`.
        * "Exit": Calls `Gdx.app.exit()`.
    - Provide a "Back" button on `LevelSelectScreen` that returns to `MainMenuScreen`.

Ensure all Scene2D Label font scales call `.invalidate()` to fix bounds alignment, and keep color styles isolated to prevent TextButtonStyle mutation bugs. (written by gemini sent to copilot)
-create a respawn component and system. add the component in the playerdeathsystem and don't reset the screen on player death. Spawn should create a preloaded checkpoint. Also it is bad practice to call systems inside systems, so the getActiveCheckpoint method is useless. Instead use checkpoint order to determine the active checkpoint. (copilot)
- Please implement a centralized SoundManager and integrate SFX triggers directly into my Ashley ECS systems using Option A (Direct Playback) in my LibGDX 2D platformer project according to the following requirements:

1. Create a `SoundManager` Utility Class:
    - Construct a `SoundManager` class that wraps the game's `AssetManager`.
    - Include methods to queue and load `Sound` assets during application startup.
    - Implement `playSound(String path)` and an overloaded `playSound(String path, float pitch)` method that checks if the asset is loaded before playing.
    - Include master volume controls (`setSfxVolume(float volume)`) and a mute toggle state (`boolean isMuted`).

2. System Integration:
    - Update system constructors (such as `DodgingSystem`, `RespawnSystem`, and `PhysicsContactSystem`) to accept `SoundManager` as a parameter.
    - Trigger appropriate SFX on key state changes:
        * Play dodge SFX immediately upon executing a teleport in `DodgingSystem`.
        * Play checkpoint activation SFX when a new checkpoint sensor is touched.
        * Play player hurt / respawn SFX upon taking damage or respawning.

3. Pitch Randomization:
   - Provide a helper method `playRandomizedSound(String path, float minPitch, float maxPitch)` using `MathUtils.random()` to add slight pitch variation (e.g., 0.9f to 1.1f) for repetitive actions like dodging or landing.
     (written by gemini sent to copilot)
-   
