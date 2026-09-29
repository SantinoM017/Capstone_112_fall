package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.physics.box2d.*;
import io.github.Capstone_112_fall.components.GroundedComponent;

// uses contactlistener to check if the player is grounded or not
public class PhysicsContactSystem extends EntitySystem implements ContactListener {
    public PhysicsContactSystem(World world) {
        world.setContactListener(this);
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkFootContact(fixtureA, fixtureB, true);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();

        checkFootContact(fixtureA, fixtureB, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    private void checkFootContact(Fixture fixtureA, Fixture fixtureB, boolean isBegin) {
        Fixture footFixture = null;

        if("player_foot".equals(fixtureA.getUserData())) {
            footFixture = fixtureA;
        } else if("player_foot".equals(fixtureB.getUserData())) {
            footFixture = fixtureB;
        }

        if(footFixture !=null) {
            Object userData = footFixture.getBody().getUserData();
            if(userData instanceof GroundedComponent) {
                GroundedComponent groundedComponent = (GroundedComponent) userData;
                if(isBegin) {
                    groundedComponent.groundContacts++;
                } else {
                    groundedComponent.groundContacts--;
                }
                groundedComponent.isGrounded = groundedComponent.groundContacts > 0;
//                System.out.println("Grounded: " + groundedComponent.groundContacts);
            }
        }
    }
}
