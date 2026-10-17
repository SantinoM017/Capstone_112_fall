package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import io.github.Capstone_112_fall.components.*;

public class HealthSystem extends IteratingSystem {
    public HealthSystem(){
        super(Family.all(HealthComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime){
        HealthComponent health = ComponentMappers.HEALTH.get(entity);
        QueueForDestructionComponent qd = ComponentMappers.QUEUE_FOR_DESTRUCTION.get(entity);
        Box2DComponent box2D = ComponentMappers.BOX_2D.get(entity);

        if(health.previousHp > health.hp) {
            health.invulnerabilityTimer = health.invulnerabilityDuration;
        }

        health.previousHp = health.hp;


        if(health.invulnerabilityTimer > 0){
            health.invulnerabilityTimer-=deltaTime;
        }
        if(health.hitStunTimer > 0){
            health.hitStunTimer-=deltaTime;
        }

        if(box2D.body != null && box2D.body.getPosition().y < -10f) {
            health.hp -= 1;
        }

        if(health.hp <= 0 && qd == null) {
            QueueForDestructionComponent newQd = getEngine().createComponent(QueueForDestructionComponent.class);
            AnimationComponent anim = ComponentMappers.ANIMATION.get(entity);
            StateComponent state = ComponentMappers.STATE.get(entity);
            state.currentState = StateComponent.State.DEAD;
            if (anim != null) {
                float animationDuration = anim.getAnimationDuration(state);
                if (animationDuration >= 0f) {
                    newQd.destructionTimer = animationDuration;
                }
            }
            entity.add(newQd);
        }
    }


}
