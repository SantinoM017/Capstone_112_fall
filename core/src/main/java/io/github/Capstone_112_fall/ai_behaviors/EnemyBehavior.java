package io.github.Capstone_112_fall.ai_behaviors;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.physics.box2d.World;
import io.github.Capstone_112_fall.components.Box2DComponent;
import io.github.Capstone_112_fall.components.EnemyComponent;

public interface EnemyBehavior {
    void execute(Entity entity, Entity playerEntity, EnemyComponent enemy, Box2DComponent box2d, World world, float deltaTime);
}
