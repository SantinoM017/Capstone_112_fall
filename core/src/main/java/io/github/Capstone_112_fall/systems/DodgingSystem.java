package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.*;

// solution created by me. Code written by Gemini
public class DodgingSystem extends IteratingSystem {

    private final World world;
    private final Camera camera;

    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);
    private final ComponentMapper<Box2DComponent> box2DMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);

    private final Vector3 unprojectTmp = new Vector3();
    private final Vector2 tempVertex = new Vector2();

    private final TextureAtlas atlas;

    public DodgingSystem(World world, Camera camera, TextureAtlas atlas) {
        super(Family.all(PlayerComponent.class, StateComponent.class, Box2DComponent.class).get());
        this.world = world;
        this.camera = camera;
        this.atlas = atlas;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        StateComponent state = stateMapper.get(entity);
        Box2DComponent box2d = box2DMapper.get(entity);
        HealthComponent health = healthMapper.get(entity);

        if (box2d == null || box2d.body == null) return;

        // Manage Dodge execution timer
        if (state != null && state.currentState == StateComponent.State.DODGING) {
            // Note: Adjust stateTime check depending on whether dodge is instant or delayed
            health.invulnerabilityTimer = 0.8f;
            if (state.stateTime >= 0.5f) { // Trigger dodge action
                executeDodge(box2d, health);
                createSmoke(entity);
                box2d.body.setAwake(true);
                player.resetAction();
                state.setState(StateComponent.State.IDLE);
            }
        }
    }

    private void executeDodge(Box2DComponent box2d, HealthComponent health) {
        Body body = box2d.body;
        Vector2 startPos = body.getPosition();

        // 1. Unproject Mouse Cursor Screen Coordinates to World Coordinates
        unprojectTmp.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(unprojectTmp);
        Vector2 targetPos = new Vector2(unprojectTmp.x, unprojectTmp.y);

        // Optional: Cap max teleport distance if desired
        float maxDodgeDist = 15.0f; // in meters
        Vector2 dodgeDir = new Vector2(targetPos).sub(startPos);
        if (dodgeDir.len() > maxDodgeDist) {
            dodgeDir.nor().scl(maxDodgeDist);
            targetPos.set(startPos).add(dodgeDir);
        }

        // 2. Measure Player's Half-Width and Half-Height to calculate safety margin
        float halfWidth = getBodyHalfWidth(body);
        float halfHeight = getBodyHalfHeight(body);

        final Vector2 finalDestination = new Vector2(targetPos);
        final float[] closestFraction = {1.0f};

        // 3. Raycast toward mouse target
        world.rayCast((Fixture fixture, Vector2 point, Vector2 normal, float fraction) -> {
            // Ignore the player's own fixtures, sensors, and non-static entities (Enemies, NPCs)
            if (fixture.getBody() == body || fixture.isSensor()
                || fixture.getBody().getType() != BodyDef.BodyType.StaticBody) {
                return 1.0f; // Ignore fixture, pass through entities
            }

            // Only block on static ground/wall terrain
            if (isStaticTerrain(fixture)) {
                if (fraction < closestFraction[0]) {
                    closestFraction[0] = fraction;

                    // BACK-OFF OFFSET: Back up destination point by player bounding box radius
                    // along the impact normal so player doesn't materialize inside the wall shape
                    float safeOffsetX = normal.x * (halfWidth + 0.05f);
                    float safeOffsetY = normal.y * (halfHeight + 0.05f);

                    finalDestination.set(point.x + safeOffsetX, point.y + safeOffsetY);
                }
                return fraction; // Clip ray to closest hit point
            }

            return 1.0f;
        }, startPos.x, startPos.y, targetPos.x, targetPos.y);

        // 4. Safely Teleport Player
        body.setTransform(finalDestination.x, finalDestination.y, body.getAngle());

        // Zero out residual velocities to prevent launching immediately after teleport
        body.setAwake(true);
        body.setLinearVelocity(1f, 1f);
    }

    private void createSmoke(Entity entity) {
        Entity smoke = getEngine().createEntity();
        Box2DComponent box2d = box2DMapper.get(entity);
        TransformComponent transform = getEngine().createComponent(TransformComponent.class);
        transform.x = box2d.body.getPosition().x;
        transform.y = box2d.body.getPosition().y;
        smoke.add(transform);
        TextureComponent texture = getEngine().createComponent(TextureComponent.class);
        texture.textureRegion = atlas.findRegion("smoke");
        smoke.add(texture);
        QueueForDestructionComponent qd = getEngine().createComponent(QueueForDestructionComponent.class);
        qd.destructionTimer = 0.5f;
        smoke.add(qd);
        getEngine().addEntity(smoke);
        System.out.println("smoke created");
    }

    private boolean isStaticTerrain(Fixture fixture) {
        Object userData = fixture.getUserData();
        return "ground".equals(userData) || "static_block".equals(userData)
            || "wall".equals(userData) || "slope".equals(userData);
    }

    private float getBodyHalfWidth(Body body) {
        float halfWidth = 0.35f;
        for (Fixture fixture : body.getFixtureList()) {
            if (fixture.isSensor()) continue;
            if (fixture.getShape() instanceof PolygonShape) {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    shape.getVertex(i, tempVertex);
                    halfWidth = Math.max(halfWidth, Math.abs(tempVertex.x));
                }
            }
        }
        return halfWidth;
    }

    private float getBodyHalfHeight(Body body) {
        float halfHeight = 0.5f;
        for (Fixture fixture : body.getFixtureList()) {
            if (fixture.isSensor()) continue;
            if (fixture.getShape() instanceof PolygonShape) {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    shape.getVertex(i, tempVertex);
                    halfHeight = Math.max(halfHeight, Math.abs(tempVertex.y));
                }
            }
        }
        return halfHeight;
    }
}
