package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.TransformComponent;

// Syncs the TransformComponent with the Box2DComponent's body position and rotation
public class PhysicsSyncSystem extends IteratingSystem {
    public PhysicsSyncSystem() {
        // Family.all(...) returns all entities that have both Box2DComponent and TransformComponent
        super(Family.all(Box2DComponent.class, TransformComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        Box2DComponent box2DComponent = ComponentMappers.BOX_2D.get(entity);
        TransformComponent transformComponent = ComponentMappers.TRANSFORM.get(entity);

        // Update the TransformComponent's position and rotation based on the Box2D body's position and angle
        if (box2DComponent.body == null) return;
        transformComponent.x = box2DComponent.body.getPosition().x;
        transformComponent.y = box2DComponent.body.getPosition().y;
        transformComponent.rotation = (float) Math.toDegrees(box2DComponent.body.getAngle());
    }

}
