package io.github.Capstone_112_fall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    public static final float PPM = 32.0f; // 32 pixels per meter
    private SpriteBatch batch; // renders sprites
    private OrthographicCamera camera; // world camera
    private World world; // sandbox world
    private Box2DDebugRenderer debugRenderer; // For testing. Draws hitboxes and physics
    private Body playerBody; // player body

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800f/PPM, 480f/PPM); // set the camera size
        world = new World(new Vector2(0, -9.81f), true); // set gravity to 9.81m/s^2
        debugRenderer = new Box2DDebugRenderer();

        createBody(400f/PPM, 240f/PPM, 128f/PPM, 32f/PPM);
        createPlayer(400/PPM, 480/PPM, 64f/PPM, 32f/PPM);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f); // refresh the screen
        handleInput();
        world.step(1/60f, 6, 2); // advance world by 1/60th of a second
        camera.update(); // update the camera
        debugRenderer.render(world, camera.combined);

    }

    @Override
    public void dispose() {
        // cleans up after the program is closed
        batch.dispose();
        world.dispose();
        debugRenderer.dispose();
    }

    private void createBody(float x, float y, float width, float height){
        // Definition: Type and position
        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.StaticBody;
        bDef.position.set(x, y);

        Body body = world.createBody(bDef); // create body in world

        // Create shape
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width/2, height/2);

        // Attach shape and fixtures to body
        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = 0.5f;
        body.createFixture(fDef);

        shape.dispose();
    }

    private void createPlayer(float x, float y, float width, float height){
        // Definition: Type and position
        BodyDef bDef = new BodyDef();
        bDef.type = BodyDef.BodyType.DynamicBody;
        bDef.position.set(x, y);

        playerBody = world.createBody(bDef); // create body in world

        // Create shape
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(width/2, height/2);

        // Attach shape and fixtures to body
        FixtureDef fDef = new FixtureDef();
        fDef.shape = shape;
        fDef.friction = 0.5f;
        fDef.density = 1f; // Mass = area * density
        fDef.restitution = 0.1f;
        playerBody.createFixture(fDef);

        shape.dispose();
    }

    private void handleInput(){
        // prevent null errors
        if(playerBody == null) return;

        Vector2 vel = playerBody.getLinearVelocity();
        float speed = 2f; // meters per second

        // horizontal movement
        if(Gdx.input.isKeyPressed(Input.Keys.A)){
            playerBody.setLinearVelocity(-speed, vel.y);
        } else if(Gdx.input.isKeyPressed(Input.Keys.D)){
            playerBody.setLinearVelocity(speed, vel.y);
        } else {
            playerBody.setLinearVelocity(0, vel.y);
        }

        // vertical movement
        if(Gdx.input.isKeyJustPressed(Input.Keys.W)){
            playerBody.applyLinearImpulse(
                new Vector2(0, 12f),
                playerBody.getWorldCenter(),
                true
            );
        }
    }
}
