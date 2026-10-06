package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.GroundedComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

// written by CoPilot
public class SlopeFrictionSystem extends IteratingSystem {
    private static final String SLOPE_FIXTURE = "slope";
    private static final String PLAYER_SLOPE_FIXTURE = "player_slope_fixture";
    private static final float IDLE_SLOPE_FRICTION = 20f;

    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<GroundedComponent> groundedComponent = ComponentMapper.getFor(GroundedComponent.class);
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);

    public SlopeFrictionSystem() {
        super(Family.all(PlayerComponent.class, Box2DComponent.class, GroundedComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2d = box2DComponent.get(entity);
        PlayerComponent player = playerComponent.get(entity);
        GroundedComponent ground = groundedComponent.get(entity);

        if (box2d == null || box2d.body == null) return;

        Fixture bottomFixture = getBottomCircleFixture(box2d.body);
        if(bottomFixture == null) return;

        boolean touchingSlope = false;
        for(Contact contact : box2d.body.getWorld().getContactList()) {
            if(!contact.isTouching()) continue;

            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            boolean playerFixtureOnContact = fixtureA == bottomFixture || fixtureB == bottomFixture;
            boolean slopeFixtureOnContact = SLOPE_FIXTURE.equals(fixtureA.getUserData())
                || SLOPE_FIXTURE.equals(fixtureB.getUserData());
            if(playerFixtureOnContact && slopeFixtureOnContact) {
                touchingSlope = true;
                break;
            }
        }

        float targetFriction = touchingSlope && ground.isGrounded && !player.isMoving
            ? IDLE_SLOPE_FRICTION : 0f;

        if(bottomFixture.getFriction() != targetFriction) {
            bottomFixture.setFriction(targetFriction);
            refreshContactFriction(box2d.body);
        }
    }

    private Fixture getBottomCircleFixture(Body body) {
        for (Fixture fixture : body.getFixtureList()) {
            if(!fixture.isSensor() && PLAYER_SLOPE_FIXTURE.equals(fixture.getUserData())) {
                return fixture;
            }
        }
        return null;
    }

    private void refreshContactFriction(Body body) {
        if(body.getWorld() == null) return;

        for (Contact contact : body.getWorld().getContactList()) {
            Fixture fixtureA = contact.getFixtureA();
            Fixture fixtureB = contact.getFixtureB();
            boolean playerSlopeContact = (fixtureA.getBody() == body
                && PLAYER_SLOPE_FIXTURE.equals(fixtureA.getUserData())
                && SLOPE_FIXTURE.equals(fixtureB.getUserData()))
                || (fixtureB.getBody() == body
                && PLAYER_SLOPE_FIXTURE.equals(fixtureB.getUserData())
                && SLOPE_FIXTURE.equals(fixtureA.getUserData()));
            if(contact.isTouching() && playerSlopeContact) {
                contact.resetFriction();
            }
        }
    }
}
