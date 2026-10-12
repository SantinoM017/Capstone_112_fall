package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.*;

public class EnemyAISystem extends IteratingSystem {
    private final World world;

    private final ComponentMapper<EnemyComponent> enemyMapper = ComponentMapper.getFor(EnemyComponent.class);
    private final ComponentMapper<Box2DComponent> box2dMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<TextureComponent> textureMapper = ComponentMapper.getFor(TextureComponent.class);
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

        // 1. SHARED: Pause AI during hit-stun / knockback
        if (health != null && health.hitStunTimer > 0) {
            enemy.aiState = EnemyComponent.AIState.STUNNED;
            return;
        }

        if (box2d.temporarilyDynamicFromKinematic) {
            checkAndRevertKinematicState(box2d);
            if (box2d.temporarilyDynamicFromKinematic) {
                enemy.aiState = EnemyComponent.AIState.STUNNED;
                return;
            }
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
            enemy.facingRight = (playerPos.x > enemyPos.x);
        } else {
            enemy.aiState = EnemyComponent.AIState.PATROL;
        }
    }

    private void checkAndRevertKinematicState(Box2DComponent box2d) {
        Body body = box2d.body;
        float halfHeight = getBodyHalfHeight(body);
        if (halfHeight <= 0f) {
            box2d.previousDynamicY = body.getPosition().y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        Vector2 position = body.getPosition();
        float previousY = box2d.hasPreviousDynamicY ? box2d.previousDynamicY : position.y;
        float velocityY = body.getLinearVelocity().y;
        if (velocityY > 0.1f) {
            box2d.previousDynamicY = position.y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        float rayStartY = Math.max(previousY, position.y);
        float rayEndY = position.y - halfHeight - 0.1f;
        final float[] groundY = {Float.NaN};

        if (rayStartY > rayEndY) {
            world.rayCast((fixture, point, normal, fraction) -> {
                if (fixture.getBody() == body || fixture.isSensor()
                    || fixture.getBody().getType() != BodyDef.BodyType.StaticBody
                    || !isGroundFixture(fixture)) {
                    return 1f;
                }

                groundY[0] = point.y;
                return fraction;
            }, position.x, rayStartY, position.x, rayEndY);
        }

        if (Float.isNaN(groundY[0])) {
            box2d.previousDynamicY = position.y;
            box2d.hasPreviousDynamicY = true;
            return;
        }

        float groundedY = groundY[0] + halfHeight;
        body.setTransform(position.x, groundedY, body.getAngle());
        body.setLinearVelocity(0f, 0f);
        body.setType(BodyDef.BodyType.KinematicBody);
        box2d.temporarilyDynamicFromKinematic = false;
        box2d.hasPreviousDynamicY = false;
    }

    private float getBodyHalfHeight(Body body) {
        float halfHeight = 0f;
        for (Fixture fixture : body.getFixtureList()) {
            if ("enemy_attackbox".equals(fixture.getUserData())) continue;

            if (fixture.getShape() instanceof PolygonShape) {
                PolygonShape shape = (PolygonShape) fixture.getShape();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    shape.getVertex(i, tempVertex);
                    halfHeight = Math.max(halfHeight, Math.abs(tempVertex.y));
                }
            } else if (fixture.getShape() instanceof CircleShape) {
                CircleShape shape = (CircleShape) fixture.getShape();
                halfHeight = Math.max(halfHeight, Math.abs(shape.getPosition().y) + shape.getRadius());
            }
        }
        return halfHeight;
    }

    private boolean isGroundFixture(Fixture fixture) {
        Object userData = fixture.getUserData();
        return "ground".equals(userData)
            || "static_block".equals(userData)
            || "slope".equals(userData)
            || "one_way_platform".equals(userData);
    }

    private Entity getPlayerEntity() {
        ImmutableArray<Entity> players = getEngine().getEntitiesFor(Family.all(PlayerComponent.class).get());
        return players.size() > 0 ? players.first() : null;
    }
}
