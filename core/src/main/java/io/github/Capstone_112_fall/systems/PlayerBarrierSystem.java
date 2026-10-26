package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.PlayerBarrierComponent;
import io.github.Capstone_112_fall.components.QueueForDestructionComponent;

public class PlayerBarrierSystem extends IteratingSystem {
    public PlayerBarrierSystem() {
        super(Family.all(PlayerBarrierComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        PlayerBarrierComponent playerBarrier = ComponentMappers.PLAYER_BARRIER.get(entity);

        if (playerBarrier.key == null) {
            if (ComponentMappers.QUEUE_FOR_DESTRUCTION.get(entity) == null) {
                QueueForDestructionComponent destroy =
                    getEngine().createComponent(QueueForDestructionComponent.class);
                entity.add(destroy);
            }
        }
    }
}
