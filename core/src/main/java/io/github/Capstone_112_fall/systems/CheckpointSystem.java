package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.ashley.utils.ImmutableArray;
import io.github.Capstone_112_fall.components.CheckpointComponent;
import io.github.Capstone_112_fall.components.ComponentMappers;
import io.github.Capstone_112_fall.components.HealthComponent;
import io.github.Capstone_112_fall.components.PlayerComponent;

public class CheckpointSystem extends IteratingSystem {
    private int nextOrder = 1;

    public CheckpointSystem() {
        super(Family.all(CheckpointComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CheckpointComponent checkpoint = ComponentMappers.CHECKPOINT.get(entity);
        if (checkpoint.active) {
            checkpoint.order = nextOrder++;
            checkpoint.active = false;
            checkpoint.used = true;
        }
    }
}
