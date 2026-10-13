package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.utils.EnemyBodyBounds;

public class EnemyAISystem extends IteratingSystem {
    private final World world;

    private final ComponentMapper<EnemyComponent> enemyMapper = ComponentMapper.getFor(EnemyComponent.class);
    private final ComponentMapper<Box2DComponent> box2dMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<TextureComponent> textureMapper = ComponentMapper.getFor(TextureComponent.class);
    private final Vector2 halfExtents = new Vector2();
    private final Vector2 tempVertex = new Vector2();

    public EnemyAISystem(World world) {
        super(Family.all(EnemyComponent.class, Box2DComponent.class).get());
        this.world = world;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        EnemyComponent enemy = enemyMapper.get(entity);
        Box2DComponent box2d = box2dMapper.get(entity);
        HealthComponent health = healthMapper.get(entity);
        TextureComponent texture = textureMapper.get(entity);

        if (box2d == null || box2d.body == null) return;

        // 1. SHARED: Recover the sensor-only body even while hit-stun is active.
        if (box2d.temporarilyDynamicFromKinematic) {
            checkAndRevertKinematicState(box2d);
            if (box2d.temporarilyDynamicFromKinematic) {
                enemy.aiState = EnemyComponent.AIState.STUNNED;
                return;
            }
        }

        // 2. SHARED: Pause AI during hit-stun / knockback.
        if (health != null && health.hitStunTimer > 0) {
            enemy.aiState = EnemyComponent.AIState.STUNNED;
            return;
        }

        // 3. SHARED: Player Target Acquisition & State Machine
        updateTargeting(entity, enemy, box2d);

        // 4. DELEGATED: Execute entity-specific movement behavior strategy
        if (enemy.behavior != null) {
            enemy.behavior.execute(entity, getPlayerEntity(), enemy, box2d, world, deltaTime);
        }

        // 5. SHARED: Visual updates (sprite flipping)
        if (texture != null) {
            texture.flipX = !enemy.facingRight;
        }
    }

    private void updateTargeting(Entity enemyEntity, EnemyComponent enemy, Box2DComponent box2d) {
        Entity playerEntity = getPlayerEntity();
        if (playerEntity == null) {
            enemy.aiState = EnemyComponent.AIState.PATROL;
            return;
        }

        Box2DComponent playerB2d = box2dMapper.get(playerEntity);
        if (playerB2d == null || playerB2d.body == null) {
            enemy.aiState = EnemyComponent.AIState.PATROL;
            return;
        }

        Vector2 enemyPos = box2d.body.getPosition();
        Vector2 playerPos = playerB2d.body.getPosition();

        float dist = enemyPos.dst(playerPos);

        if (dist <= enemy.detectionRange) {
            enemy.aiState = EnemyComponent.AIState.CHASE;

            // Calculate half-widths dynamically for both bodies
            float enemyHalfWidth = getBodyHalfWidth(box2d.body);
            float playerHalfWidth = getBodyHalfWidth(playerB2d.body);

            // Edge calculations: Right side and Left side
            float playerRight = playerPos.x + playerHalfWidth;
            float playerLeft  = playerPos.x - playerHalfWidth;

            float enemyRight  = enemyPos.x + enemyHalfWidth;
            float enemyLeft   = enemyPos.x - enemyHalfWidth;

            // Only flip right if the player's right edge moves past the enemy's right edge
            if (playerRight > enemyRight) {
                enemy.facingRight = true;
            }
            // Only flip left if the player's left edge moves past the enemy's left edge
            else if (playerLeft < enemyLeft) {
                enemy.facingRight = false;
            }
            // If overlapping between these boundaries, retain current facing direction
        } else {
            enemy.aiState = EnemyComponent.AIState.PATROL;
        }
    }

    /**
     * Sweeps downward from the entity's previous/current Y position to detect ground contact,
     * resolves horizontal wall overlaps safely, and converts the body back to Kinematic.
     */
    private void checkAndRevertKinematicState(Box2DComponent box2d) {
        final Body body = box2d.body;

        EnemyBodyBounds.getHalfExtents(body, halfExtents, tempVertex);
        final float halfHeight = halfExtents.y;
        final float halfWidth = halfExtents.x;

        if (halfHeight <= 0f) {
            box2d.previousDynamicX = body.getPosition().x;
            box2d.previousDynamicY = body.getPosition().y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        final Vector2 position = body.getPosition();
        final float previousX = box2d.hasPreviousDynamicY ? box2d.previousDynamicX : position.x;
        final float previousY = box2d.hasPreviousDynamicY ? box2d.previousDynamicY : position.y;
        resolveKnockbackWallCollision(
            body, previousX, previousY, position.x, position.y, halfWidth, halfHeight);

        final Vector2 resolvedPosition = body.getPosition();
        final float velocityY = body.getLinearVelocity().y;

        // 2. Ignore reversion while still ascending from knockback
        if (velocityY > 0.1f) {
            box2d.previousDynamicX = resolvedPosition.x;
            box2d.previousDynamicY = resolvedPosition.y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        // 3. Vertical Swept Ground Raycast
        final float rayStartY = Math.max(previousY, resolvedPosition.y);
        final float rayEndY = resolvedPosition.y - halfHeight - 0.15f; // Slight padding extension
        final float[] groundY = {Float.NaN};
        int groundProbeIntervals = Math.max(2, 2 * (int) Math.ceil(halfWidth / 0.25f));

        if (rayStartY > rayEndY) {
            for (int i = 0; i <= groundProbeIntervals; i++) {
                float probeX = resolvedPosition.x - halfWidth
                    + 2f * halfWidth * i / groundProbeIntervals;
                world.rayCast((fixture, point, normal, fraction) -> {
                    if (fixture.getBody() == body || fixture.isSensor()
                        || fixture.getBody().getType() != BodyDef.BodyType.StaticBody
                        || !isGroundFixture(fixture)) {
                        return 1f;
                    }

                    if (Float.isNaN(groundY[0]) || point.y > groundY[0]) {
                        groundY[0] = point.y;
                    }
                    return 1f;
                }, probeX, rayStartY, probeX, rayEndY);
            }
        }

        // If no ground detected under feet, keep falling as Dynamic
        if (Float.isNaN(groundY[0])) {
            box2d.previousDynamicX = resolvedPosition.x;
            box2d.previousDynamicY = resolvedPosition.y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        // 4. Safe Grounded Y position
        final float groundedY = groundY[0] + halfHeight;

        // 6. Apply Transformation & Convert back to Kinematic
        body.setTransform(resolvedPosition.x, groundedY, body.getAngle());
        body.setLinearVelocity(0f, 0f);
        body.setType(BodyDef.BodyType.KinematicBody);

        // 7. Reset flags
        box2d.temporarilyDynamicFromKinematic = false;
        box2d.hasPreviousDynamicY = false;
    }

    private void resolveKnockbackWallCollision(
        Body body, float previousX, float previousY,
        float currentX, float currentY, float halfWidth, float halfHeight) {
        final float direction = Math.signum(currentX - previousX);
        if (direction == 0f) return;

        final float startX = previousX + direction * halfWidth;
        final float endX = currentX + direction * halfWidth;
        final float[] hitFraction = {Float.POSITIVE_INFINITY};
        final float[] hitX = {currentX};
        float verticalInset = Math.min(0.05f, halfHeight * 0.5f);
        float bottomY = Math.min(previousY, currentY) - halfHeight + verticalInset;
        float topY = Math.max(previousY, currentY) + halfHeight - verticalInset;
        int intervals = Math.max(1, (int) Math.ceil((topY - bottomY) / 0.2f));
        for (int i = 0; i <= intervals; i++) {
            float sampleY = bottomY + (topY - bottomY) * i / intervals;
            world.rayCast((fixture, point, normal, fraction) -> {
                if (!isSolidStaticFixture(fixture)) return 1f;
                if (fraction < hitFraction[0]) {
                    hitFraction[0] = fraction;
                    hitX[0] = point.x;
                }
                return fraction;
            }, startX, sampleY, endX, sampleY);
        }

        if (!Float.isInfinite(hitFraction[0])) {
            float resolvedX = hitX[0] - direction * (halfWidth + 0.01f);
            body.setTransform(resolvedX, currentY, body.getAngle());
            body.setLinearVelocity(0f, body.getLinearVelocity().y);
        }
    }

    private boolean isSolidStaticFixture(Fixture fixture) {
        Object userData = fixture.getUserData();
        return (!fixture.isSensor()
            && fixture.getBody().getType() == BodyDef.BodyType.StaticBody)
            || "entity_barrier".equals(userData);
    }

    private boolean isGroundFixture(Fixture fixture) {
        Object userData = fixture.getUserData();
        return "ground".equals(userData)
            || "static_block".equals(userData)
            || "slope".equals(userData)
            || "one_way_platform".equals(userData);
    }

    private float getBodyHalfHeight(Body body) {
        float halfHeight = 0.5f; // Safe fallback value (0.5m)
        float maxAbsY = 0f;

        for (Fixture fixture : body.getFixtureList()) {
            if (fixture.isSensor()) continue; // Ignore attackboxes/hurtboxes

            if (fixture.getShape() instanceof PolygonShape) {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    shape.getVertex(i, tempVertex);
                    maxAbsY = Math.max(maxAbsY, Math.abs(tempVertex.y));
                }
            } else if (fixture.getShape() instanceof CircleShape) {
                CircleShape shape = (CircleShape) fixture.getShape();
                maxAbsY = Math.max(maxAbsY, Math.abs(shape.getPosition().y) + shape.getRadius());
            }
        }

        return maxAbsY > 0f ? maxAbsY : halfHeight;
    }

    private float getBodyHalfWidth(Body body) {
        float halfWidth = 0f;
        for (Fixture fixture : body.getFixtureList()) {
            if (fixture.isSensor()) continue;

            if (fixture.getShape() instanceof PolygonShape) {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    shape.getVertex(i, tempVertex);
                    halfWidth = Math.max(halfWidth, Math.abs(tempVertex.x));
                }
            } else if (fixture.getShape() instanceof CircleShape) {
                CircleShape shape = (CircleShape) fixture.getShape();
                halfWidth = Math.max(halfWidth, Math.abs(shape.getPosition().x) + shape.getRadius());
            }
        }
        return halfWidth > 0f ? halfWidth : 0.35f; // Safe fallback if no physical shapes match
    }

    private Entity getPlayerEntity() {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get());
        return players.size() > 0 ? players.first() : null;
    }
}
