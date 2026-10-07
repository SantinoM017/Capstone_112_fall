package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import io.github.Capstone_112_fall.components.*;

// updates player state based on velocity and grounded status
public class PlayerStateSystem extends IteratingSystem {
    private final ComponentMapper<StateComponent> stateComponent = ComponentMapper.getFor(StateComponent.class);
    private final ComponentMapper<PlayerComponent> playerComponent = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<Box2DComponent> box2DComponent = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<GroundedComponent> groundedComponent = ComponentMapper.getFor(GroundedComponent.class);
    private final ComponentMapper<TextureComponent> textureComponent = ComponentMapper.getFor(TextureComponent.class);
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);

    public PlayerStateSystem() {
        super(Family.all(StateComponent.class, PlayerComponent.class,
            GroundedComponent.class, TextureComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        StateComponent state = stateComponent.get(entity);
        PlayerComponent player = playerComponent.get(entity);
        Box2DComponent box2D = box2DComponent.get(entity);
        GroundedComponent grounded = groundedComponent.get(entity);
        TextureComponent texture = textureComponent.get(entity);
        HealthComponent health = healthComponent.get(entity);

        if(box2D.body == null) return;

        Vector2 velocity = box2D.body.getLinearVelocity();

        // flip texture based on velocity
        if(velocity.x > 0.1f) {
            texture.flipX = false;
            player.isFlipped = false;
        } else if(velocity.x < -0.1f) {
            texture.flipX = true;
            player.isFlipped = true;
        }

        // player movement states: jumping, falling, walking, idle
        if(player.attackRequested) {
            if(state.currentState != StateComponent.State.ATTACKING) {
                player.isAttacking = true;
                state.setState(StateComponent.State.ATTACKING);
                state.stateTime = 0f;
            }
        } else if(!grounded.isGrounded) {
            if(velocity.y > 0.1f) {
                state.setState(StateComponent.State.JUMPING);
            } else if (velocity.y < -0.1f) {
                state.setState(StateComponent.State.FALLING);
            }
        } else {
            if(velocity.x != 0) {
                state.setState(StateComponent.State.WALKING);
            } else {
                state.setState(StateComponent.State.IDLE);
            }
        }

        if(health.hp <= 0) {
            state.setState(StateComponent.State.DEAD);
        }

        state.stateTime += deltaTime;
//        System.out.println("State: " + state.currentState);
    }
}
