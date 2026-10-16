package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.utils.EntityManager;

public class AttackSystem extends IteratingSystem {

    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<PlayerExtensionComponent> extensionMapper = ComponentMapper.getFor(PlayerExtensionComponent.class);
    private final ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);

    private final EntityManager entityFactory;

    public AttackSystem(EntityManager entityFactory) {
        super(Family.all(PlayerComponent.class, StateComponent.class).get());
        this.entityFactory = entityFactory;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = playerMapper.get(entity);
        PlayerExtensionComponent extension = extensionMapper.get(entity);
        StateComponent state = stateMapper.get(entity);

        // 1. Attack started, but no sensor fixture created yet
        if (player.currentAction == PlayerComponent.Action.ATTACKING && state.stateTime >= 0.1f) {
//            System.out.println("Creating attack hitbox for entity: " + entity);
            if(extension == null) {
                entityFactory.createPlayerExtension(entity, PlayerExtensionComponent.Type.ATTACK);
                return;
            }
        }

        // 2. Attack fixture is active -> manage timer
        if (state != null && state.currentState == StateComponent.State.ATTACKING) {
            if (state.stateTime >= 0.4f) {
                // Remove sensor fixture from Box2D body
                entityFactory.destroyPlayerExtension(entity);

                // Reset attack flags and state
                player.resetAction();
                state.setState(StateComponent.State.IDLE);
            }
        }

        if (state != null && state.currentState == StateComponent.State.HURT) {
            entityFactory.destroyPlayerExtension(entity);
        }
    }
}
