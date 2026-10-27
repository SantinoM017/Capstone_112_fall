package io.github.Capstone_112_fall;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.Capstone_112_fall.Screens.LevelSelectScreen;
import io.github.Capstone_112_fall.Screens.MainScreen;
import io.github.Capstone_112_fall.Screens.DeathDialog;
import io.github.Capstone_112_fall.Screens.VictoryDialog;
import io.github.Capstone_112_fall.systems.*;
import io.github.Capstone_112_fall.utils.AnimationFactory;
import io.github.Capstone_112_fall.utils.EntityManager;
import io.github.Capstone_112_fall.utils.MapBuilder;
import io.github.Capstone_112_fall.utils.MapEntityData;
import io.github.Capstone_112_fall.utils.SoundManager;

public class Main extends ScreenAdapter {
    private final MainScreen game;
    private final String levelPath;
    private final int levelNumber;
    public static final float PPM = 16.0f;
    private SpriteBatch batch; // renders sprites
    private OrthographicCamera camera; // world camera
    private Viewport viewport; // manages the camera and screen size
    private World world; // sandbox world
    private Box2DDebugRenderer debugRenderer; // For testing. Draws hitboxes and physics
    private Engine engine; // Ashley engine for managing entities
    private RenderSystem renderSystem;
    private TiledMap map; // stores the map
    private OrthogonalTiledMapRenderer mapRenderer; // draws the map
    private TextureAtlas atlas;
    private Stage victoryStage;
    private Stage deathStage;
    private boolean continueAfterVictory;
    private boolean retryAfterDeath;
    private boolean leaveAfterDeath;

    // viewport size
    private static final float V_WIDTH = 24;
    private static final float V_HEIGHT = 13.5f;

    public Main(MainScreen game) {
        this(game, "levels/level1.tmx");
    }

    public Main(MainScreen game, String levelPath) {
        this.game = game;
        this.levelPath = levelPath;
        String levelNumberText = levelPath.replaceAll(".*level[_-]?(\\d+).*", "$1");
        if (levelNumberText.equals(levelPath)) {
            throw new IllegalArgumentException("Level path must include a level number: " + levelPath);
        }
        this.levelNumber = Integer.parseInt(levelNumberText);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera); // adjusts viewport based on screen size
        viewport.apply();
        camera.setToOrtho(false, V_WIDTH, V_HEIGHT); // set the camera size
        // handles camera movement
        CameraSystem cameraSystem = new CameraSystem(camera);
        world = new World(new Vector2(0, -2f), true);
        debugRenderer = new Box2DDebugRenderer();
        engine = new Engine();

        atlas = game.getAssets().getGameAtlas();
        SoundManager soundManager = game.getAssets().getSoundManager();
        ShaderProgram hitFlashShader = game.getAssets().getHitFlashShader();

        AnimationFactory animationFactory = new AnimationFactory(atlas);
        EntityManager entityManager = new EntityManager(engine, animationFactory, world, atlas);

        map = game.getAssets().getMap(levelPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1/PPM); // draws the map converting from pixels to meters

        int mapWidthTiles = map.getProperties().get("width", Integer.class);
        int mapHeightTiles = map.getProperties().get("height", Integer.class);
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        float mapWidth = mapWidthTiles * tileWidth / PPM;
        float mapHeight = mapHeightTiles * tileHeight / PPM;
        entityManager.createMapSideBoundaries(mapWidth, mapHeight);

        // Ashley setup
        // 1. INPUT PHASE
        engine.addSystem(new PlayerInputSystem());

        // 2. COMBAT & BEHAVIOR LOGIC PHASE
        // Modify movement intentions, apply state changes, and manage player extensions
        engine.addSystem(new PlayerStateSystem(soundManager));
        engine.addSystem(new EnemyAISystem(world));
        engine.addSystem(new EnemyStateSystem());
        engine.addSystem(new KnockbackSystem());
        engine.addSystem(new DamageSystem(soundManager));
        engine.addSystem(new StunSystem());
        engine.addSystem(new PlayerExtensionSystem(entityManager, soundManager));
        engine.addSystem(new DodgingSystem(world, camera, atlas));
        engine.addSystem(new HealthSystem());
        engine.addSystem(new PlayerBarrierSystem());
        engine.addSystem(new SlopeFrictionSystem()); // Pre-step friction adjustments

        // 3. PHYSICS & CONTACT PHASE
        // PhysicsContactSystem steps Box2D and processes collisions
        engine.addSystem(new PhysicsContactSystem(world, soundManager));

        // 4. POST-PHYSICS & CLEANUP PHASE
        // Sync visual transforms to Box2D bodies, process transitions, and destroy dead entities
        engine.addSystem(new PhysicsSyncSystem());
        engine.addSystem(new CheckpointSystem());
        engine.addSystem(new PlayerDeathSystem(world));
        engine.addSystem(new RespawnSystem(entityManager, this));
        engine.addSystem(new CompleteLevelSystem(this, levelNumber));
        engine.addSystem(new DestroyEntitySystem(world)); // MUST run after PhysicsContactSystem and HealthSystem

        // 5. CAMERA & RENDERING PHASE
        engine.addSystem(cameraSystem);
        renderSystem = new RenderSystem(batch, camera, atlas, hitFlashShader);
        engine.addSystem(renderSystem);

        // Modified by Claude (Anthropic AI assistant)
        entityManager.createMapEntities(MapBuilder.parse(map, PPM)); // turns tiles and objects into entities

        Array<MapEntityData> data = new Array<>();
        entityManager.createMapEntities(data);
        cameraSystem.setMapBounds(mapWidth, mapHeight);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0f, 0f, 0f, 1f);
        float backgroundWidth = camera.viewportWidth;
        float backgroundHeight = camera.viewportHeight;
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.begin();
        batch.draw(
            game.getAssets().getLevelBackground(),
            camera.position.x - backgroundWidth / 2f,
            camera.position.y - backgroundHeight / 2f,
            backgroundWidth,
            backgroundHeight
        );
        batch.end();

        if (victoryStage == null && deathStage == null) {
            world.step(1/60f, 6, 2); // advance the physics simulation
            engine.update(delta); // runs the engine
        } else {
            renderSystem.update(0f);
        }
        mapRenderer.setView(camera);
        mapRenderer.render();
        debugRenderer.render(world, camera.combined);

        if (victoryStage != null) {
            victoryStage.act(delta);
            victoryStage.draw();
            if (continueAfterVictory) {
                game.setScreen(new LevelSelectScreen(game));
            }
        }

        if (deathStage != null) {
            deathStage.act(delta);
            deathStage.draw();
            if (leaveAfterDeath) {
                game.setScreen(new LevelSelectScreen(game));
            } else if (retryAfterDeath) {
                disposeDeathUi();
            }
        }
    }

    public void showVictoryDialog(int completedLevel) {
        if (victoryStage != null) return;

        victoryStage = new Stage(new FitViewport(400, 240));
        victoryStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(victoryStage);
        VictoryDialog.show(victoryStage, game.getSkin(), completedLevel,
            game.getAssets().getSoundManager(), () -> continueAfterVictory = true);
    }

    public void showDeathDialog() {
        if (deathStage != null || victoryStage != null) return;

        deathStage = new Stage(new FitViewport(400, 240));
        deathStage.getViewport().update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(deathStage);
        DeathDialog.show(
            deathStage,
            game.getSkin(),
            game.getAssets().getSoundManager(),
            () -> retryAfterDeath = true,
            () -> leaveAfterDeath = true
        );
    }

    public boolean isDeathDialogVisible() {
        return deathStage != null;
    }

    public boolean consumeDeathRetry() {
        boolean retryRequested = retryAfterDeath;
        retryAfterDeath = false;
        return retryRequested;
    }

    @Override
    public void hide() {
        disposeVictoryUi();
    }

    // cleans up after the program is closed
    @Override
    public void dispose() {
        disposeVictoryUi();
        batch.dispose();
        world.dispose();
        debugRenderer.dispose();
        mapRenderer.dispose();
    }

    // runs when window is resized
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        if (victoryStage != null) {
            victoryStage.getViewport().update(width, height, true);
        }
        if (deathStage != null) {
            deathStage.getViewport().update(width, height, true);
        }
    }

    private void disposeVictoryUi() {
        if (victoryStage != null) {
            if (Gdx.input.getInputProcessor() == victoryStage) {
                Gdx.input.setInputProcessor(null);
            }
            victoryStage.dispose();
            victoryStage = null;
        }
    }

    private void disposeDeathUi() {
        if (deathStage != null) {
            if (Gdx.input.getInputProcessor() == deathStage) {
                Gdx.input.setInputProcessor(null);
            }
            deathStage.dispose();
            deathStage = null;
        }
    }

}
