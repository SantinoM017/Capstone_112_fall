package io.github.Capstone_112_fall;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.SerializationException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.components.TransformComponent;
import io.github.Capstone_112_fall.systems.CameraSystem;
import io.github.Capstone_112_fall.systems.PhysicsSyncSystem;
import io.github.Capstone_112_fall.systems.PlayerInputSystem;
import io.github.Capstone_112_fall.utils.MapBuilder;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public static final float PPM = 32.0f; // 32 pixels per meter
    private SpriteBatch batch; // renders sprites
    private OrthographicCamera camera; // world camera
    private Viewport viewport; // manages the camera and screen size
    private World world; // sandbox world
    private Box2DDebugRenderer debugRenderer; // For testing. Draws hitboxes and physics
    private int numFootContacts = 0;
    private Engine engine; // Ashley engine for managing entities
    private PlayerComponent playerComponent; // creates a reference to player component
    private TiledMap map; // stores the map
    private OrthogonalTiledMapRenderer mapRenderer; // draws the map

    // viewport size
    private final static float V_WIDTH = 25.0f;
    private final static float V_HEIGHT = 15.0f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new FitViewport(V_WIDTH, V_HEIGHT, camera); // adjusts viewport based on screen size
        viewport.apply();
        camera.position.set(V_WIDTH/2, V_HEIGHT/2, 0); // centers camera to middle of viewport
        camera.setToOrtho(false, 800f/PPM, 480f/PPM); // set the camera size
        // handles camera movement
        CameraSystem cameraSystem = new CameraSystem(camera);
        world = new World(new Vector2(0, -6f), true);
        debugRenderer = new Box2DDebugRenderer();

        // ensure that the player can only jump when grounded
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact){
                Fixture fixtureA = contact.getFixtureA();
                Fixture fixtureB = contact.getFixtureB();
                if("player_foot".equals(fixtureA.getUserData()) || "player_foot".equals(fixtureB.getUserData())){
                    numFootContacts++;
                    if(playerComponent != null) playerComponent.isGrounded = numFootContacts > 0;
                }
            }

            public void endContact(Contact contact){
                Fixture fixtureA = contact.getFixtureA();
                Fixture fixtureB = contact.getFixtureB();
                if("player_foot".equals(fixtureA.getUserData()) || "player_foot".equals(fixtureB.getUserData())){
                    numFootContacts--;
                    if(playerComponent != null) playerComponent.isGrounded = numFootContacts > 0;
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold){}
            @Override
            public void postSolve(Contact contact, ContactImpulse impulse){}
        });

        // Ashley setup
        engine = new Engine();
        engine.addSystem(new PlayerInputSystem());
        engine.addSystem(new PhysicsSyncSystem());
        engine.addSystem(cameraSystem);

        // Tiled setup
        try {
            map = new TmxMapLoader().load("levels/level1.tmx"); // reads from level file
        } catch(SerializationException e){
            System.out.println("File not found");
        }
        mapRenderer = new OrthogonalTiledMapRenderer(map, 1/PPM); // draws the map converting from pixels to meters
        MapBuilder.buildShapes(map.getLayers().get("Ground"), PPM, world); // creates the actual Box2D hitboxes

        // player setup
        Body playerBody = createPlayer(400f/PPM, 320f/PPM);
        createPlayerEntity(playerBody);

        // treat tiles as 1x1 meters, so no conversion rate needed
        int mapWidthTiles = map.getProperties().get("width", Integer.class);
        int mapHeightTiles = map.getProperties().get("height", Integer.class);
        cameraSystem.setMapBounds(mapWidthTiles, (mapHeightTiles));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f); // refresh the screen
        world.step(1/60f, 6, 2); // advance world by 1/60th of a second
        engine.update(Gdx.graphics.getDeltaTime()); // updates the screen to match the engine
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
    }

    // runs when window is resized
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private Body createPlayer(float x, float y){
        // Definition: Type and position
        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.DynamicBody;
        bDef.position.set(x, y);
        bDef.fixedRotation = true;

        Body playerBody = world.createBody(bDef); // create body in world

        // Create shape for body
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.48f, 0.5f);

        // Attach shape and fixtures to body
        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = 0f;
        fDef.density = 1/0.98f; // Mass = area * density
        fDef.restitution = 0.1f;
        Fixture mainFixture = playerBody.createFixture(fDef);
        mainFixture.setUserData("player_body"); // to identify body later
        shape.dispose();

        // Create shape for foot
        PolygonShape shape2 = new PolygonShape();
        shape2.setAsBox(0.40f, 0.1f, new Vector2(0, -0.45f), 0);

        // Attach foot to body
        FixtureDef fDef2 = new FixtureDef();
        fDef2.shape = shape2;
        fDef2.isSensor = true; // only used to detect collisions
        Fixture footFixture = playerBody.createFixture(fDef2);
        footFixture.setUserData("player_foot"); // to identify foot later
        shape2.dispose();

        return playerBody;
    }

    private void createPlayerEntity(Body body){
        Entity player = new Entity();

        // create and add body component
        Box2DComponent box2DComponent = engine.createComponent(Box2DComponent.class);
        box2DComponent.body = body;
        player.add(box2DComponent);

        // create and add transform component
        TransformComponent transformComponent = engine.createComponent(TransformComponent.class);
        player.add(transformComponent);

        // create and add player component/tag
        playerComponent = engine.createComponent(PlayerComponent.class);
        player.add(playerComponent);

        engine.addEntity(player);
    }
}
