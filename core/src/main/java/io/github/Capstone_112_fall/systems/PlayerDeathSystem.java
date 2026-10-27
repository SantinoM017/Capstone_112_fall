package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.PlayerComponent;
import io.github.Capstone_112_fall.components.QueueForDestructionComponent;
import io.github.Capstone_112_fall.components.RespawnComponent;

public class PlayerDeathSystem extends IteratingSystem {
    private final World world;

    public PlayerDeathSystem(World world) {
        super(Family.all(PlayerComponent.class, Box2DComponent.class, QueueForDestructionComponent.class).get());
        this.world = world;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2d = ComponentMappers.BOX_2D.get(entity);
        QueueForDestructionComponent qd = ComponentMappers.QUEUE_FOR_DESTRUCTION.get(entity);

        if (qd.destructionTimer > 0) {
            qd.destructionTimer -= deltaTime;
        }

        if (box2d != null && box2d.body != null && qd.destructionTimer <= 0) {
            world.destroyBody(box2d.body);
            box2d.body = null;
        }

        if (qd.destructionTimer <= 0 && ComponentMappers.RESPAWN.get(entity) == null) {
            entity.add(getEngine().createComponent(RespawnComponent.class));
        }
    }
}
