package io.github.Capstone_112_fall.systems;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import io.github.Capstone_112_fall.components.*;

public class HealthSystem extends IteratingSystem {
    private final ComponentMapper<HealthComponent> healthComponent = ComponentMapper.getFor(HealthComponent.class);
    private final ComponentMapper<DamageComponent> damageComponent = ComponentMapper.getFor(DamageComponent.class);
    private final ComponentMapper<QueueForDestructionComponent> qdComponent = ComponentMapper.getFor(QueueForDestructionComponent.class);
    private final ComponentMapper<AnimationComponent> animComp = ComponentMapper.getFor(AnimationComponent.class);
    private final ComponentMapper<Box2DComponent> box2DComp = ComponentMapper.getFor(Box2DComponent.class);
    private final ComponentMapper<StateComponent> stateComp = ComponentMapper.getFor(StateComponent.class);
    public HealthSystem(){
        super(Family.all(HealthComponent.class).get());
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime){
        HealthComponent health = healthComponent.get(entity);
        QueueForDestructionComponent qd = qdComponent.get(entity);
        Box2DComponent box2D = box2DComp.get(entity);

        if(health.invulnerabilityTimer >= 0){
            health.invulnerabilityTimer-=deltaTime;
        }
        if(health.hitStunTimer > 0){
            health.hitStunTimer-=deltaTime;
        }

        if(box2D.body != null && box2D.body.getPosition().y < -10f) {
            health.hp -= 1;
        }

        if(health.isTakingDamage) {
            Entity hazard = health.takingDamageFrom;
            if (hazard != null && health.invulnerabilityTimer <= 0) {
                DamageComponent damage = damageComponent.get(hazard);
                health.takeDamage(damage.damage);
                System.out.println(health.hp);
            }
        }

        if(health.hp <= 0 && qd == null) {
            QueueForDestructionComponent newQd = getEngine().createComponent(QueueForDestructionComponent.class);
            AnimationComponent anim = animComp.get(entity);
            StateComponent state = stateComp.get(entity);
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
