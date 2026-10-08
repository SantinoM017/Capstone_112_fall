package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.DamageComponent;

public class DamageSystem extends IteratingSystem {
    private final ComponentMapper<DamageComponent> dMap = ComponentMapper.getFor(DamageComponent.class);

    public DamageSystem(){
        super(Family.all(DamageComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity,float deltaTime){
        DamageComponent damage = dMap.get(entity);


    }
}
