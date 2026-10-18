package io.github.Capstone_112_fall.ai_behaviors;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.utils.EnemyBodyBounds;

public class SlimeBossBehavior implements EnemyBehavior {
    private final float moveSpeed;
    private final float chaseSpeed;
    private final float edgeProbeOffset;
    private boolean enraged = false;
    private float stateTimer;
    private boolean airborne;
    private final Vector2 halfExtents = new Vector2();
    private final Vector2 tempVertex = new Vector2();

    public SlimeBossBehavior(float moveSpeed, float chaseSpeed, float edgeProbeOffset) {
        this.moveSpeed = moveSpeed;
        this.chaseSpeed = chaseSpeed;
        this.edgeProbeOffset = edgeProbeOffset;
    }

    public SlimeBossBehavior() {
        this(0.8f, 1f, 0.1f); // Default values
    }

    @Override
    public void execute(Entity entity, Entity player, EnemyComponent enemy, Box2DComponent box2d, World world, float deltaTime) {
        Body body = box2d.body;
        HealthComponent health = ComponentMappers.HEALTH.get(entity);
        EnemyBodyBounds.getHalfExtents(body, halfExtents, tempVertex);
        float probeOffset = Math.max(edgeProbeOffset, halfExtents.x + 0.05f);

        // 1. Edge Detection: Turn around if approaching a drop-off
        boolean groundAhead = checkGroundAhead(world, body, enemy.facingRight, probeOffset, halfExtents.y);
        boolean wallAhead = checkWallAhead(world, body, enemy.facingRight, probeOffset, halfExtents.y);
        if ((!groundAhead || wallAhead) && enemy.aiState != EnemyComponent.AIState.CHASE) {
            enemy.facingRight = !enemy.facingRight;
        }

        // 2. Determine Speed based on State
        float speed = 0f;
        if(!wallAhead) {
            speed = (enemy.aiState == EnemyComponent.AIState.CHASE) ? chaseSpeed : moveSpeed;
            if(enraged) {
                speed *= 3f;
            }
        }
        float dir = enemy.facingRight ? 1.0f : -1.0f;

        // 3. Apply Horizontal Velocity (preserving current Y for gravity)
        Vector2 currentVel = body.getLinearVelocity();
        body.setLinearVelocity(dir * speed, currentVel.y);

        // 4. Update Animation State
        StateComponent state = entity.getComponent(StateComponent.class);
        if (state != null && state.currentState != StateComponent.State.HURT) {
            state.currentState = StateComponent.State.WALKING;
        }

        // Avoid crowding the player
        if (player != null) {
            HealthComponent playerHealth = player.getComponent(HealthComponent.class);
            // If player was just hit, pause or back off slightly so they aren't enveloped
            if (playerHealth != null && playerHealth.hitStunTimer > 0) {
                float awayDir = enemy.facingRight ? -1.0f : 1.0f;
                body.setLinearVelocity(awayDir * (moveSpeed * 0.5f), body.getLinearVelocity().y);
            }
        }

        if(((float) health.hp / health.maxHp) <= 0.15f) {
            enraged = true;
        }

        stateTimer += deltaTime;
        if(stateTimer >= 2f) {
            box2d.temporarilyDynamicFromKinematic = true;
            body.setType(BodyDef.BodyType.DynamicBody);
            body.applyLinearImpulse(new Vector2(0, 5f), body.getWorldCenter(), true);
            System.out.println("a");
            stateTimer = 0f;
            airborne = true;
        }
    }

    private boolean checkGroundAhead(
        World world, Body body, boolean facingRight, float offset, float halfHeight) {
        Vector2 pos = body.getPosition();
        float probeX = pos.x + (facingRight ? offset : -offset);
        float startY = pos.y;
        float endY = pos.y - halfHeight - 0.15f;

        final boolean[] hitGround = {false};

        world.rayCast((Fixture fixture, Vector2 point, Vector2 normal, float fraction) -> {
            if (isGround(fixture)) {
                hitGround[0] = true;
                return 0; // Terminate raycast early on first ground hit
            }
            return 1;
        }, probeX, startY, probeX, endY);

        return hitGround[0];
    }

    private boolean checkWallAhead(
        World world, Body body, boolean facingRight, float offset, float halfHeight) {
        Vector2 pos = body.getPosition();

        // Start raycast from body center
        float startX = pos.x;
        float endX = pos.x + (facingRight ? offset : -offset);
        final boolean[] hitWall = {false};

        float verticalInset = Math.min(0.05f, halfHeight * 0.5f);
        float bottomY = pos.y - halfHeight + verticalInset;
        float topY = pos.y + halfHeight - verticalInset;
        int intervals = Math.max(1, (int) Math.ceil((topY - bottomY) / 0.2f));
        for (int i = 0; i <= intervals; i++) {
            float sampleY = bottomY + (topY - bottomY) * i / intervals;
            world.rayCast((Fixture fixture, Vector2 point, Vector2 normal, float fraction) -> {
                if (!isWall(fixture, body)) return 1;
                hitWall[0] = true;
                return 0;
            }, startX, sampleY, endX, sampleY);
            if (hitWall[0]) break;
        }

        return hitWall[0];
    }

    private boolean isGround(Fixture fixture) {
        Object userData = fixture.getUserData();
        return !fixture.isSensor()
            && fixture.getBody().getType() == BodyDef.BodyType.StaticBody
            && ("ground".equals(userData)
            || "static_block".equals(userData)
            || "slope".equals(userData)
            || "one_way_platform".equals(userData));
    }

    private boolean isWall(Fixture fixture, Body body) {
        Object userData = fixture.getUserData();
        return fixture.getBody() != body
            && ((!fixture.isSensor()
            && fixture.getBody().getType() == BodyDef.BodyType.StaticBody)
            || "entity_barrier".equals(userData));
    }
}
