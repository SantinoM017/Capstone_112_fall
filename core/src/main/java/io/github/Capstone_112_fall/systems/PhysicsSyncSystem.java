package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.TransformComponent;

public class PhysicsSyncSystem extends IteratingSystem {
    // allows for O(1) access to components
    private final ComponentMapper<Box2DComponent> box2DMapper = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<TransformComponent> transformMapper = ComponentMapper.getFor(TransformComponent.class);

    public PhysicsSyncSystem() {
        // Family.all(...) returns all entities that have both Box2DComponent and TransformComponent
        super(Family.all(Box2DComponent.class, TransformComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2DComponent = box2DMapper.get(entity);
        TransformComponent transformComponent = transformMapper.get(entity);

        // Update the TransformComponent's position and rotation based on the Box2D body's position and angle
        if (box2DComponent.body == null) return;
        transformComponent.x = box2DComponent.body.getPosition().x;
        transformComponent.y = box2DComponent.body.getPosition().y;
        transformComponent.rotation = (float) Math.toDegrees(box2DComponent.body.getAngle());
    }

}
