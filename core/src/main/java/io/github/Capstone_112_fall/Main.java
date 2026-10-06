package io.github.Capstone_112_fall;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.systems.*;
import io.github.Capstone_112_fall.utils.AnimationFactory;
import io.github.Capstone_112_fall.utils.EntityManager;
import io.github.Capstone_112_fall.utils.MapBuilder;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public static final float PPM = 16.0f;
    private SpriteBatch batch; // renders sprites
    private OrthographicCamera camera; // world camera
    private Viewport viewport; // manages the camera and screen size
    private World world; // sandbox world
    private Box2DDebugRenderer debugRenderer; // For testing. Draws hitboxes and physics
    private Engine engine; // Ashley engine for managing entities
    private TiledMap map; // stores the map
    private OrthogonalTiledMapRenderer mapRenderer; // draws the map
    private TextureAtlas atlas;

    // viewport size
    private static final float V_WIDTH = 20;
    private static final float V_HEIGHT = 10f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera); // adjusts viewport based on screen size
        viewport.apply();
        camera.position.set(V_WIDTH/2, V_HEIGHT/2, 0); // centers camera to middle of viewport
        camera.setToOrtho(false, V_WIDTH, V_HEIGHT); // set the camera size
        // handles camera movement
        CameraSystem cameraSystem = new CameraSystem(camera);
        world = new World(new Vector2(0, -2f), true);
        debugRenderer = new Box2DDebugRenderer();
        engine = new Engine();

        // atlas setup
        try {
            atlas = new TextureAtlas("game_assets.atlas");
        } catch (GdxRuntimeException e){
            System.out.println("Texture atlas not found");
        }

        AnimationFactory animationFactory = new AnimationFactory(atlas);
        EntityManager entityManager = new EntityManager(engine, animationFactory, world, atlas);

        try {
            map = new TmxMapLoader().load("levels/level1.tmx"); // reads from level file
        } catch(SerializationException e){
            System.out.println("Level not found");
        }
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1/PPM); // draws the map converting from pixels to meters

        int mapWidthTiles = map.getProperties().get("width", Integer.class);
        int mapHeightTiles = map.getProperties().get("height", Integer.class);
        int tileWidth = map.getProperties().get("tilewidth", Integer.class);
        int tileHeight = map.getProperties().get("tileheight", Integer.class);
        float mapWidth = mapWidthTiles * tileWidth / PPM;
        float mapHeight = mapHeightTiles * tileHeight / PPM;
        Entity playerEntity = entityManager.createPlayerEntity(mapWidth / 2f, mapHeight / 2f);
        entityManager.createMapSideBoundaries(mapWidth, mapHeight);

        // Ashley setup
        engine.addSystem(new PlayerInputSystem());
        engine.addSystem(new PhysicsSyncSystem());
        engine.addSystem(new PhysicsContactSystem(world));
        engine.addSystem(new SlopeFrictionSystem());
        engine.addSystem(new PlayerStateSystem());
        engine.addSystem(new AttackSystem(entityManager));
        engine.addSystem(cameraSystem);
        engine.addSystem(new RenderSystem(batch, camera));

        // Modified by Claude (Anthropic AI assistant)
        entityManager.createMapEntities(MapBuilder.parse(map, PPM)); // turns tiles and objects into entities

        cameraSystem.setMapBounds(mapWidth, mapHeight);
    }

    @Override
    public void render() {
        ScreenUtils.clear(1f, 1f, 1f, 1f); // refresh the screen
        world.step(1/60f, 6, 2); // advance the physics simulation
        engine.update(Gdx.graphics.getDeltaTime()); // runs the engine
        camera.update(); // update the camera
        mapRenderer.setView(camera);
        mapRenderer.render();
        debugRenderer.render(world, camera.combined);

    }

    // cleans up after the program is closed
    @Override
    public void dispose() {
        batch.dispose();
        world.dispose();
        debugRenderer.dispose();
        map.dispose();
        mapRenderer.dispose();
        atlas.dispose();
    }

    // runs when window is resized
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
