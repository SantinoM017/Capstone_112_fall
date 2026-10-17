package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.EnemyComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.StateComponent;
import io.github.Capstone_112_fall.components.ComponentMappers;

public class EnemyStateSystem extends IteratingSystem {
    public EnemyStateSystem() {
        super(Family.all(EnemyComponent.class, StateComponent.class, HealthComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        StateComponent state = ComponentMappers.STATE.get(entity);
        HealthComponent health = ComponentMappers.HEALTH.get(entity);

        if(health.hp <= 0) {
            state.setState(StateComponent.State.DEAD);
            state.stateTime += deltaTime;
            return;
        }

        state.stateTime += deltaTime;

    }
}
