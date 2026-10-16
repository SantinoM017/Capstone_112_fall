package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.PlayerExtensionComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.components.StateComponent;
import io.github.Capstone_112_fall.utils.EntityManager;

public class ParrySystem extends IteratingSystem {
    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);
    private final ComponentMapper<Box2DComponent> box2DMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<PlayerExtensionComponent> extensionMapper =
        ComponentMapper.getFor(PlayerExtensionComponent.class);

    private final EntityManager entityManager;

    public ParrySystem(EntityManager entityManager) {
        super(Family.all(PlayerComponent.class, StateComponent.class, Box2DComponent.class).get());
        this.entityManager = entityManager;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime){
        PlayerComponent player = playerMapper.get(entity);
        StateComponent state = stateMapper.get(entity);
        Box2DComponent box2d = box2DMapper.get(entity);

        if (box2d == null || box2d.body == null) return;
        if (state != null && state.currentState == StateComponent.State.PARRY) {
            if (extensionMapper.get(entity) == null) {
                entityManager.createPlayerExtension(entity, PlayerExtensionComponent.Type.PARRY);
            }
            if (state.stateTime >= 0.2f) {
                entityManager.destroyPlayerExtension(entity);
                player.resetAction();
                state.setState(StateComponent.State.IDLE);
            }
        } else {
            PlayerExtensionComponent extension = extensionMapper.get(entity);
            if (extension != null && extension.type == PlayerExtensionComponent.Type.PARRY) {
                entityManager.destroyPlayerExtension(entity);
            }
        }
    }
}
