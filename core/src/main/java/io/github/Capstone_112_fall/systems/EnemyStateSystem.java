package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.github.Capstone_112_fall.components.AnimationComponent;
import io.github.Capstone_112_fall.components.EnemyComponent;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.StateComponent;

public class EnemyStateSystem extends IteratingSystem {
    private final ComponentMapper<StateComponent> stateComponent = ComponentMapper.getFor(StateComponent.class);
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);
    public EnemyStateSystem() {
        super(Family.all(EnemyComponent.class, StateComponent.class, HealthComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        StateComponent state = stateComponent.get(entity);
        HealthComponent health = healthComponent.get(entity);

        if(health.hp <= 0) {
            state.setState(StateComponent.State.DEAD);
            state.stateTime += deltaTime;
            return;
        }

        state.stateTime += deltaTime;

    }
}
