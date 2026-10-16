package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.components.*;

public class DestroyEntitySystem extends IteratingSystem {
    private final ComponentMapper<QueueForDestructionComponent> qdMapper = ComponentMapper.getFor(QueueForDestructionComponent.class);
    private final ComponentMapper<PlayerComponent> playerMapper = ComponentMapper.getFor(PlayerComponent.class);
    private final ComponentMapper<Box2DComponent> box2dMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final World world;
    public DestroyEntitySystem(World world) {
        super(Family.all(QueueForDestructionComponent.class).get());
        this.world = world;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        QueueForDestructionComponent qd = qdMapper.get(entity);
        PlayerComponent player = playerMapper.get(entity);

        if(player != null) return;

        if(qd.destructionTimer > 0) {
            qd.destructionTimer-=deltaTime;
        }

        // 1. Safely dispose of the Box2D Body
        Box2DComponent box2d = box2dMapper.get(entity);
        if (box2d != null && box2d.body != null) {
            // Destroy the body and all attached fixtures from the Box2D world
            world.destroyBody(box2d.body);
            box2d.body = null;
        }

        if(qd.destructionTimer <= 0) {
            // 2. Remove the entity from the Ashley Engine
            getEngine().removeEntity(entity);
        }
    }

}
