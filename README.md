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
    - Helped create SlopeFrictionSystem and Tangent-based movement on slopes (with copilot)
    - Helped create One way platform contact system
- Junie/Github Copilot:
    - Used to help write code and provide suggestions for code completion and refactoring.
    - Used to help explain parts of the code and to explain any questions or issues I am having with the code
    - Used to debug code
    - Helped debug the AttackSystem and PlayerInputSystem
    - I refactored the player entity userData to refer to the entity itself instead of the grounded component. I asked the AI to refactor all uses of the grounded component as userData to use the entity itself from userData
    - Ran out of inline suggestion on 10/07/2026
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
