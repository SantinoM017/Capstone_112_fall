package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.GroundedComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

// class revised by Gemini. Movement got very complicated once slopes were incorporated.
public class PlayerInputSystem extends IteratingSystem {
    private static final float MAX_SLOPE_SPEED_MULTIPLIER = 1.30f;
    private static final float JUMP_VELOCITY = 4.0f; // Set explicit upward launch velocity (m/s)

    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<GroundedComponent> groundedComponent = ComponentMapper.getFor(GroundedComponent.class);

    public PlayerInputSystem() {
        super(Family.all(Box2DComponent.class, PlayerComponent.class, GroundedComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2D = box2DComponent.get(entity);
        PlayerComponent player = playerComponent.get(entity);
        GroundedComponent grounded = groundedComponent.get(entity);

        if (box2D.body == null) return;

        float speed = 2f; // meters per second
        boolean movingLeft = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean movingRight = Gdx.input.isKeyPressed(Input.Keys.D);
        player.isMoving = movingLeft || movingRight;

        player.jumpCooldown = Math.max(0f, player.jumpCooldown - deltaTime);

        Vector2 currentVel = box2D.body.getLinearVelocity();

        // --- 1. JUMP EXECUTION ---
        boolean jumpRequested = Gdx.input.isKeyPressed(Input.Keys.W);

        // Ensure jump only triggers when fully grounded and NOT already ascending
        if (jumpRequested && grounded.isGrounded && currentVel.y <= 0.05f && player.jumpCooldown == 0f) {
            player.jumpCooldown = 0.35f;

            // DIRECT VELOCITY ASSIGNMENT: Prevents impulse accumulation with slope contact forces
            box2D.body.setLinearVelocity(currentVel.x, JUMP_VELOCITY);

            // Re-fetch updated velocity for movement calculation below
            currentVel = box2D.body.getLinearVelocity();
        }

        // --- 2. MOVEMENT & SLOPE STEERING ---
        float direction = movingLeft == movingRight ? 0f : movingLeft ? -1f : 1f;

        if (direction == 0f) {
            box2D.body.setLinearVelocity(0f, currentVel.y);
        } else {
            // SAFEGUARD: Bypasses slope velocity steering completely if jumping or rising (vel.y > 0.05f)
            boolean isAscending = currentVel.y > 0.05f;
            Vector2 slopeVelocity = (grounded.isGrounded && !isAscending)
                ? getSlopeVelocity(box2D.body, direction, speed)
                : null;

            if (slopeVelocity != null) {
                box2D.body.setLinearVelocity(slopeVelocity.x, slopeVelocity.y);
            } else {
                box2D.body.setLinearVelocity(direction * speed, currentVel.y);
            }
        }

        // --- 3. VARIABLE JUMP HEIGHT CUTOFF ---
        // Only damp velocity if key was released while ascending and speed hasn't already been cut
        if (!Gdx.input.isKeyPressed(Input.Keys.W) && box2D.body.getLinearVelocity().y > 1.0f) {
            Vector2 vel = box2D.body.getLinearVelocity();
            box2D.body.setLinearVelocity(vel.x, vel.y * 0.5f);
        }

        // --- 4. ATTACK INPUT ---
        player.attackCooldown = Math.max(0f, player.attackCooldown - deltaTime);
        if ((Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) && player.attackCooldown == 0f) {
            player.attackRequested = true;
            player.attackCooldown = 0.5f;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            player.isDroppingThrough = true;
            player.dropThroughTimer = 0.1f;
            box2D.body.setAwake(true);
        }

        if (player.isDroppingThrough) {
            player.dropThroughTimer -= deltaTime;
            if (player.dropThroughTimer <= 0f) {
                player.isDroppingThrough = false;
            }
        }
    }

    private final Vector2 tempNormal = new Vector2();
    private final Vector2 tempVertexA = new Vector2();
    private final Vector2 tempVertexB = new Vector2();
    private final Vector2 tempSlopeNormal = new Vector2();
    private final Vector2 tempTangent = new Vector2();
    private final Vector2 tempResult = new Vector2();

    private Vector2 getSlopeVelocity(Body body, float direction, float speed) {
        if (body == null || body.getWorld() == null || direction == 0f) return null;

        for (Contact contact : body.getWorld().getContactList()) {
            if (!contact.isTouching()) continue;

            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();

            boolean playerFixtureIsA = ("player_slope_fixture".equals(fixtureA.getUserData()) || "player_body".equals(fixtureA.getUserData()))
                && "slope".equals(fixtureB.getUserData());
            boolean playerFixtureIsB = ("player_slope_fixture".equals(fixtureB.getUserData()) || "player_body".equals(fixtureB.getUserData()))
                && "slope".equals(fixtureA.getUserData());

            if (!playerFixtureIsA && !playerFixtureIsB) continue;

            Fixture slopeFixture = playerFixtureIsA ? fixtureB : fixtureA;
            if (!(slopeFixture.getShape() instanceof PolygonShape)) continue;
            PolygonShape slopeShape = (PolygonShape) slopeFixture.getShape();
            if (slopeShape.getVertexCount() != 3) continue;

            slopeShape.getVertex(0, tempVertexA);
            slopeShape.getVertex(2, tempVertexB);
            tempSlopeNormal.set(tempVertexB).sub(tempVertexA);
            tempSlopeNormal.set(-tempSlopeNormal.y, tempSlopeNormal.x);
            if (tempSlopeNormal.y < 0f) tempSlopeNormal.scl(-1f);
            tempSlopeNormal.nor().rotateRad(slopeFixture.getBody().getAngle());

            WorldManifold manifold = contact.getWorldManifold();
            tempNormal.set(manifold.getNormal());
            if (playerFixtureIsA) tempNormal.scl(-1f);
            if (tempNormal.dot(tempSlopeNormal) < 0.9f) continue;

            tempTangent.set(tempSlopeNormal.y, -tempSlopeNormal.x);

            if (tempTangent.x * direction < 0f) {
                tempTangent.scl(-1f);
            }

            if (Math.abs(tempTangent.x) < 0.01f) continue;

            float targetTangentSpeed = Math.min(
                speed / Math.abs(tempTangent.x),
                speed * MAX_SLOPE_SPEED_MULTIPLIER
            );

            tempResult.set(tempTangent).scl(targetTangentSpeed);

            // CRITICAL SAFEGUARD: Clamp upward vertical component of slope velocity.
            // Slopes should pull down when running downhill, but natural circle geometry
            // pushes the player uphill. Allowing positive slope Y velocity creates launches.
            if (tempResult.y > 0f) {
                tempResult.y = 0f;
            }

            return tempResult;
        }

        return null;
    }
}
