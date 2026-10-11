package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.components.AnimationComponent;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.EnemyComponent;
import io.github.Capstone_112_fall.components.QueueForDestructionComponent;

public class DestroyEntitySystem extends IteratingSystem {
    private final ComponentMapper<QueueForDestructionComponent> qdMapper = ComponentMapper.getFor(QueueForDestructionComponent.class);
    private final ComponentMapper<Box2DComponent> box2dMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<AnimationComponent> animComp = ComponentMapper.getFor(AnimationComponent.class);
    private final World world;
    public DestroyEntitySystem(World world) {
        super(Family.all(QueueForDestructionComponent.class, EnemyComponent.class).get());
        this.world = world;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        QueueForDestructionComponent qd = qdMapper.get(entity);
        AnimationComponent anim = animComp.get(entity);

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
