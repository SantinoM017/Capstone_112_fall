package io.github.Capstone_112_fall.ai_behaviors;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.ai_behaviors.EnemyBehavior;
import io.github.Capstone_112_fall.components.*;

public class SlimeBehavior implements EnemyBehavior {
    private final float moveSpeed;
    private final float chaseSpeed;
    private final float edgeProbeOffset;

    public SlimeBehavior(float moveSpeed, float chaseSpeed, float edgeProbeOffset) {
        this.moveSpeed = moveSpeed;
        this.chaseSpeed = chaseSpeed;
        this.edgeProbeOffset = edgeProbeOffset;
    }

    public SlimeBehavior() {
        this(0.8f, 1f, 0.35f); // Default values
    }

    @Override
    public void execute(Entity entity, Entity player, EnemyComponent enemy, Box2DComponent box2d, World world, float deltaTime) {
        Body body = box2d.body;

        // 1. Edge Detection: Turn around if approaching a drop-off
        boolean groundAhead = checkGroundAhead(world, body, enemy.facingRight, edgeProbeOffset);
        if (!groundAhead) {
            enemy.facingRight = !enemy.facingRight;
        }

        // 2. Determine Speed based on State
        float speed = (enemy.aiState == EnemyComponent.AIState.CHASE) ? chaseSpeed : moveSpeed;
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
                return;
            }
        }
    }

    private boolean checkGroundAhead(World world, Body body, boolean facingRight, float offset) {
        Vector2 pos = body.getPosition();
        float probeX = pos.x + (facingRight ? offset : -offset);
        float startY = pos.y;
        float endY = pos.y - 0.6f;

        final boolean[] hitGround = {false};

        world.rayCast((Fixture fixture, Vector2 point, Vector2 normal, float fraction) -> {
            if (fixture.isSensor()) return 1;

            Object userData = fixture.getUserData();
            if ("ground".equals(userData) || "static_block".equals(userData)
                || "slope".equals(userData) || "one_way_platform".equals(userData)) {
                hitGround[0] = true;
                return 0; // Terminate raycast early on first ground hit
            }
            return 1;
        }, probeX, startY, probeX, endY);

        return hitGround[0];
    }

}
