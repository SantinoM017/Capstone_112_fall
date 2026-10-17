package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.*;

import java.util.ArrayDeque;
import java.util.Queue;

// checks for all contacts
public class PhysicsContactSystem extends EntitySystem implements ContactListener {

    private final Vector2 tempVertex = new Vector2(); // Reusable vector to prevent GC allocation

    public PhysicsContactSystem(World world) {
        world.setContactListener(this);
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkFootContact(fixtureA, fixtureB, true);
        checkSensorContact(fixtureA, fixtureB, true);
        checkPlayerAttackContact(fixtureA, fixtureB, true);
        checkParryContact(fixtureA, fixtureB, true);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkFootContact(fixtureA, fixtureB, false);
        checkSensorContact(fixtureA, fixtureB, false);
        checkPlayerAttackContact(fixtureA, fixtureB, false);
        checkParryContact(fixtureA, fixtureB, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkOneWayPlatform(contact, fixtureA, fixtureB);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    private void checkFootContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        Fixture footFixture = null;

        if ("player_foot".equals(fixtureA.getUserData())) {
            footFixture = fixtureA;
        } else if ("player_foot".equals(fixtureB.getUserData())) {
            footFixture = fixtureB;
        }

        if (footFixture == null) return;

        Fixture groundFixture = footFixture == fixtureA ? fixtureB : fixtureA;
        if (groundFixture.isSensor()) return;

        Object bodyUserData = footFixture.getBody().getUserData();
        if (bodyUserData instanceof Entity) {
            GroundedComponent groundedComponent = ComponentMappers.GROUNDED.get((Entity) bodyUserData);
            if (groundedComponent == null) return;
            if (isBegin) {
                groundedComponent.groundContacts++;
            } else {
                groundedComponent.groundContacts = Math.max(0, groundedComponent.groundContacts - 1);
            }
            groundedComponent.isGrounded = groundedComponent.groundContacts > 0;
        }
    }

    private void checkSensorContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        Fixture playerFixture = isPlayerBodyFixture(fixtureA) ? fixtureA
            : isPlayerBodyFixture(fixtureB) ? fixtureB : null;
        Fixture sensorFixture = isDamageSensor(fixtureA) ? fixtureA
            : isDamageSensor(fixtureB) ? fixtureB : null;

        // Both a valid player fixture and a sensor fixture must be present
        if (playerFixture == null || sensorFixture == null) {
            return;
        }

        Object sensorTag = sensorFixture.getUserData();
        Object sensorBodyData = sensorFixture.getBody().getUserData();
        Object bodyUserData = playerFixture.getBody().getUserData();

        if (!(sensorBodyData instanceof Entity) || !(bodyUserData instanceof Entity)) {
            return;
        }

        Entity playerEntity = (Entity) bodyUserData;

        Entity sensorEntity = (Entity) sensorBodyData;
        KnockbackComponent kb = ComponentMappers.KNOCKBACK.get(sensorEntity);
        DamageComponent damage = ComponentMappers.DAMAGE.get(sensorEntity);

        String sensorType = sensorTag.toString();

        // Dispatch logic based on sensor type string
        switch (sensorType) {
            case "spike":
                damageSystem(isBegin, damage, playerEntity); return;
            case "enemy_attackbox":
                damageSystem(isBegin, damage, playerEntity);
                setKnockbackState(isBegin, playerEntity, kb);
        }
    }

    private void checkPlayerAttackContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        Fixture attackFixture = null;
        Fixture enemyHurtboxFixture = null;


        if ("player_attack_hitbox".equals(fixtureA.getUserData())) attackFixture = fixtureA;
        if ("player_attack_hitbox".equals(fixtureB.getUserData())) attackFixture = fixtureB;

        if ("enemy_hurtbox".equals(fixtureA.getUserData())) enemyHurtboxFixture = fixtureA;
        if ("enemy_hurtbox".equals(fixtureB.getUserData())) enemyHurtboxFixture = fixtureB;

        // Both a valid attack fixture and a sensor fixture must be present
        if (attackFixture == null || enemyHurtboxFixture == null) {
            return;
        }

        Entity enemy = (Entity) enemyHurtboxFixture.getBody().getUserData();
        Entity player = (Entity) attackFixture.getBody().getUserData();
        DamageComponent damageComponent = ComponentMappers.DAMAGE.get(player);
        KnockbackComponent kb = ComponentMappers.KNOCKBACK.get(player);
        setKnockbackState(isBegin, enemy, kb);
        damageSystem(isBegin, damageComponent, enemy);
    }

    private void checkParryContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        Fixture parryFixture = null;
        Fixture enemyAttackFixture = null;


        if ("player_parry_hitbox".equals(fixtureA.getUserData())) parryFixture = fixtureA;
        if ("player_parry_hitbox".equals(fixtureB.getUserData())) parryFixture = fixtureB;

        if ("enemy_attackbox".equals(fixtureA.getUserData())) enemyAttackFixture = fixtureA;
        if ("enemy_attackbox".equals(fixtureB.getUserData())) enemyAttackFixture = fixtureB;

        // Both a valid attack fixture and a sensor fixture must be present
        if (parryFixture == null || enemyAttackFixture == null) {
            return;
        }

        Entity enemy = (Entity) enemyAttackFixture.getBody().getUserData();
        Entity player = (Entity) parryFixture.getBody().getUserData();
        HealthComponent health = ComponentMappers.HEALTH.get(enemy);
        DamageComponent damage = ComponentMappers.DAMAGE.get(enemy);
        KnockbackComponent kb = ComponentMappers.KNOCKBACK.get(player);
        StunComponent stun = ComponentMappers.STUN.get(player);
        setKnockbackState(isBegin, enemy, kb);
        if(isBegin) {
            damage.takeDamage(damage.damage, health);
            stun.applyStun = true;
            stun.stunnedEntity = enemy;
        }
    }

    private boolean isPlayerFixture(Fixture fixture) {
        Object tag = fixture.getUserData();
        return "player_body".equals(tag)
            || "player_foot".equals(tag)
            || "player_slope_fixture".equals(tag);
    }

    private boolean isPlayerBodyFixture(Fixture fixture) {
        Object tag = fixture.getUserData();
        return "player_body".equals(tag) || "player_slope_fixture".equals(tag);
    }

    private boolean isDamageSensor(Fixture fixture) {
        Object tag = fixture.getUserData();
        return "spike".equals(tag) || ("enemy_attackbox".equals(tag) && fixture.isSensor());
    }

    private void checkOneWayPlatform(Contact contact, Fixture fixtureA, Fixture fixtureB) {
        // Match player body/foot fixtures against one-way platforms
        boolean aIsPlayer = isPlayerFixture(fixtureA);
        boolean bIsPlayer = isPlayerFixture(fixtureB);

        boolean aIsPlatform = "one_way_platform".equals(fixtureA.getUserData());
        boolean bIsPlatform = "one_way_platform".equals(fixtureB.getUserData());

        if (!((aIsPlayer && bIsPlatform) || (bIsPlayer && aIsPlatform))) {
            return;
        }

        Fixture playerFixture = aIsPlayer ? fixtureA : fixtureB;
        Fixture platformFixture = aIsPlayer ? fixtureB : fixtureA;

        Body playerBody = playerFixture.getBody();

        // 1. Drop-Through Check
        Object entityData = playerBody.getUserData();
        if (entityData instanceof Entity) {
            PlayerComponent player = ComponentMappers.PLAYER.get((Entity) entityData);
            if (player != null && player.isDroppingThrough) {
                contact.setEnabled(false);
                return;
            }
        }

        // 2. Ascent Check: Pass straight through while moving upward (jumping)
        if (playerBody.getLinearVelocity().y > 0.01f) {
            contact.setEnabled(false);
            return;
        }

        // 3. Feet Position Check: Only collide if player's bottom feet (Y offset ~ -0.45m)
        // are strictly ABOVE the platform's top surface.
        float playerFeetY = playerBody.getPosition().y - 0.45f;
        float platformTopY = getPlatformTopY(platformFixture);

        // Allow a small 0.05m tolerance buffer so the player lands smoothly without popping through
        if (playerFeetY < platformTopY - 0.05f) {
            contact.setEnabled(false);
        }
    }

    /**
     * Calculates the absolute world Y-coordinate of a platform fixture's top edge.
     */
    private float getPlatformTopY(Fixture platformFixture) {
        Body body = platformFixture.getBody();
        Shape shape = platformFixture.getShape();

        if (shape instanceof PolygonShape) {
            PolygonShape poly = (PolygonShape) shape;
            float maxY = -Float.MAX_VALUE;

            // Find the highest local vertex of the polygon platform
            for (int i = 0; i < poly.getVertexCount(); i++) {
                poly.getVertex(i, tempVertex);
                if (tempVertex.y > maxY) {
                    maxY = tempVertex.y;
                }
            }
            return body.getPosition().y + maxY;
        }

        // Default fallback if rectangle or box shape centered around origin
        return body.getPosition().y;
    }

    private void damageSystem(boolean isBegin, DamageComponent damage, Entity affectedEntity) {
        if(isBegin) {
            damage.applyDamage = true;
            damage.dealingDamageTo = affectedEntity;
        } else {
            damage.applyDamage = false;
            damage.dealingDamageTo = null;
        }
    }

    private void setKnockbackState(boolean isBegin, Entity afffectedEntity, KnockbackComponent kb) {
        if(isBegin) {
            kb.applyKnockback = true;
            kb.dealingKnockbackTo = afffectedEntity;
        } else {
            kb.applyKnockback = false;
            kb.dealingKnockbackTo = null;
        }
    }

}
