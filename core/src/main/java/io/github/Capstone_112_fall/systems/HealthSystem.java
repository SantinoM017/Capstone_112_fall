package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.HealthComponent;

public class HealthSystem extends IteratingSystem {
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);

    public HealthSystem(){
        super(Family.all(HealthComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime){
        HealthComponent health = healthComponent.get(entity);
        if(health.invulnerabilityTimer >= 0){
            health.invulnerabilityTimer-=deltaTime;
        }
    }
}
