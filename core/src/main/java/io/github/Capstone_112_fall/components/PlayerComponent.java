package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tags a player entity
public class PlayerComponent implements Component {
    public float attackCooldown;
    public float jumpCooldown;
    public boolean isFlipped;
    public boolean isMoving;
    public boolean isDroppingThrough;
    public float dropThroughTimer;
    public enum Action {
        NA,
        ATTACKING,
        DODGING,
        PARRY,
        DEAD
    }

    public Action currentAction = PlayerComponent.Action.NA;
    public float actionTime = 0f;

    public void setAction(Action action, float time) {
        if(currentAction == PlayerComponent.Action.NA) {
            currentAction = action;
            actionTime = time;
        }
    }

    public void resetAction(){
        currentAction = PlayerComponent.Action.NA;
    }
}
