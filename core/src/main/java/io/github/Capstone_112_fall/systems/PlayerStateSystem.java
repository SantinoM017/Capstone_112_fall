package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import io.github.Capstone_112_fall.components.*;

// updates player state based on velocity and grounded status
public class PlayerStateSystem extends IteratingSystem {
    public PlayerStateSystem() {
        super(Family.all(StateComponent.class, PlayerComponent.class,
            GroundedComponent.class, TextureComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        StateComponent state = ComponentMappers.STATE.get(entity);
        PlayerComponent player = ComponentMappers.PLAYER.get(entity);
        Box2DComponent box2D = ComponentMappers.BOX_2D.get(entity);
        GroundedComponent grounded = ComponentMappers.GROUNDED.get(entity);
        TextureComponent texture = ComponentMappers.TEXTURE.get(entity);
        HealthComponent health = ComponentMappers.HEALTH.get(entity);


        if(player.currentAction == PlayerComponent.Action.DEAD) {
            state.setState(StateComponent.State.DEAD);
            state.stateTime += deltaTime;
            return;
        }

        if(health.hitStunTimer > 0) {
            state.currentState = StateComponent.State.HURT;
            player.resetAction();
            state.stateTime += deltaTime;
            return;
        }

        if(box2D.body == null) return;

        Vector2 velocity = box2D.body.getLinearVelocity();

        // flip texture based on key input
        if(player.currentAction == PlayerComponent.Action.NA) {
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                texture.flipX = false;
                player.isFlipped = false;
            } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                texture.flipX = true;
                player.isFlipped = true;
            }
        }

        // player movement states: jumping, falling, walking, idle
        if(player.currentAction == PlayerComponent.Action.DODGING){
            if(state.currentState != StateComponent.State.DODGING) {
                state.setState(StateComponent.State.DODGING);
                state.stateTime = 0f;
            }
        }

        if(player.currentAction == PlayerComponent.Action.ATTACKING) {
            if(state.currentState != StateComponent.State.ATTACKING) {
                state.setState(StateComponent.State.ATTACKING);
                state.stateTime = 0f;
            }
        }

        if(player.currentAction == PlayerComponent.Action.PARRY) {
            if(state.currentState != StateComponent.State.PARRY) {
                state.setState(StateComponent.State.PARRY);
                state.stateTime = 0f;
            }
        }

        if(player.currentAction == PlayerComponent.Action.NA) {
            if (!grounded.isGrounded) {
                if (velocity.y > 0.1f) {
                    state.setState(StateComponent.State.JUMPING);
                } else if (velocity.y < -0.1f) {
                    state.setState(StateComponent.State.FALLING);
                }
            } else {
                if (velocity.x != 0) {
                    state.setState(StateComponent.State.WALKING);
                } else {
                    if (state.currentState != StateComponent.State.LONG_IDLE) state.setState(StateComponent.State.IDLE);
                }
            }
        }

        if(state.currentState == StateComponent.State.IDLE && state.stateTime > 2f) {
            state.setState(StateComponent.State.LONG_IDLE);
        }

        state.stateTime += deltaTime;

//        System.out.println("State: " + state.currentState);
    }
}
