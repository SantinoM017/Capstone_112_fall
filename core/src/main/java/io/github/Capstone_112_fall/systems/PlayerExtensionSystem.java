package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.*;
import io.github.Capstone_112_fall.utils.EntityManager;
import io.github.Capstone_112_fall.utils.GameAssets;
import io.github.Capstone_112_fall.utils.SoundManager;

public class PlayerExtensionSystem extends IteratingSystem {

    private final EntityManager entityFactory;
    private final SoundManager soundManager;

    public PlayerExtensionSystem(EntityManager entityFactory, SoundManager soundManager) {
        super(Family.all(PlayerComponent.class, StateComponent.class).get());
        this.entityFactory = entityFactory;
        this.soundManager = soundManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerComponent player = ComponentMappers.PLAYER.get(entity);
        PlayerExtensionComponent extension = ComponentMappers.PLAYER_EXTENSION.get(entity);
        StateComponent state = ComponentMappers.STATE.get(entity);

        if (state.currentState == StateComponent.State.PARRY) {
            if (extension != null && extension.type != PlayerExtensionComponent.Type.PARRY) {
                entityFactory.destroyPlayerExtension(entity);
                extension = null;
            }
            if (extension == null) {
                entityFactory.createPlayerExtension(entity, PlayerExtensionComponent.Type.PARRY);
                extension = ComponentMappers.PLAYER_EXTENSION.get(entity);
            }
            if (state.stateTime >= 0.2f) {
                if (extension != null && !extension.successfulParry) {
                    soundManager.playSound(GameAssets.SFX_UNSUCCESSFUL_PARRY);
                }
                entityFactory.destroyPlayerExtension(entity);
                player.resetAction();
                state.setState(StateComponent.State.IDLE);
            }
            return;
        }

        if (extension != null && extension.type == PlayerExtensionComponent.Type.PARRY) {
            entityFactory.destroyPlayerExtension(entity);
            extension = null;
        }

        // 1. Attack started, but no sensor fixture created yet
        if (player.currentAction == PlayerComponent.Action.ATTACKING && state.stateTime >= 0.1f) {
//            System.out.println("Creating attack hitbox for entity: " + entity);
            if(extension == null) {
                entityFactory.createPlayerExtension(entity, PlayerExtensionComponent.Type.ATTACK);
                soundManager.playRandomizedSound(GameAssets.SFX_SWORD_SWING, 0.95f, 1.05f);
                return;
            }
        }

        // 2. Attack fixture is active -> manage timer
        if (state.currentState == StateComponent.State.ATTACKING) {
            if (state.stateTime >= 0.4f) {
                // Remove sensor fixture from Box2D body
                entityFactory.destroyPlayerExtension(entity);

                // Reset attack flags and state
                player.resetAction();
                state.setState(StateComponent.State.IDLE);
            }
        }

        if (state.currentState == StateComponent.State.HURT) {
            entityFactory.destroyPlayerExtension(entity);
        }
    }
}
