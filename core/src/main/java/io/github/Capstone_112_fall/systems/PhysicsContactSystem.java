package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.GroundedComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

// uses contactlistener to check if the player is grounded or not
public class PhysicsContactSystem extends EntitySystem implements ContactListener {
    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<GroundedComponent> groundedMapper = ComponentMapper.getFor(GroundedComponent.class);
    private final ComponentMapper<HealthComponent> healthMapper = ComponentMapper.getFor(HealthComponent.class);
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
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkFootContact(fixtureA, fixtureB, false);
        checkSensorContact(fixtureA, fixtureB, false);
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

        if (footFixture != null) {
            Object bodyUserData = footFixture.getBody().getUserData();
            if (bodyUserData instanceof Entity) {
                GroundedComponent groundedComponent = groundedMapper.get((Entity) bodyUserData);
                if (groundedComponent == null) return;
                if (isBegin) {
                    groundedComponent.groundContacts++;
                } else {
                    groundedComponent.groundContacts = Math.max(0, groundedComponent.groundContacts - 1);
                }
                groundedComponent.isGrounded = groundedComponent.groundContacts > 0;
            }
        }
    }

    private void checkSensorContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        // Determine which fixture is the player and which is the sensor
        Fixture playerFixture = null;
        Fixture sensorFixture = null;

        if (isPlayerFixture(fixtureA)) playerFixture = fixtureA;
        if (isPlayerFixture(fixtureB)) playerFixture = fixtureB;

        if (fixtureA.isSensor()) sensorFixture = fixtureA;
        if (fixtureB.isSensor()) sensorFixture = fixtureB;

        // Both a valid player fixture and a sensor fixture must be present
        if (playerFixture == null || sensorFixture == null) {
            return;
        }

        Object sensorTag = sensorFixture.getUserData();
        Object bodyUserData = playerFixture.getBody().getUserData();

        if (sensorTag == null || !(bodyUserData instanceof Entity)) {
            return;
        }

        Entity playerEntity = (Entity) bodyUserData;
        PlayerComponent player = playerMapper.get(playerEntity);
        HealthComponent health = healthMapper.get(playerEntity);
        if (player == null) return;

        String sensorType = sensorTag.toString();

        // Dispatch logic based on sensor type string
        switch (sensorType) {
            case "spike":
                if (isBegin) {
                    health.takeDamage(20);
                    System.out.println(health.hp);
                }
        }
    }

    private boolean isPlayerFixture(Fixture fixture) {
        Object tag = fixture.getUserData();
        return "player_body".equals(tag)
            || "player_foot".equals(tag)
            || "player_slope_fixture".equals(tag);
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
            PlayerComponent player = playerMapper.get((Entity) entityData);
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

        // 3. Feet Position Check: Only collide if player's bottom feet (Y offset ~ -0.5m)
        // are strictly ABOVE the platform's top surface.
        float playerFeetY = playerBody.getPosition().y - 0.5f;
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
}
