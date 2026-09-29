package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

// tracks state of an entity
public class StateComponent implements Component {
    public enum State {
        IDLE,
        WALKING,
        JUMPING,
        FALLING
    }

    public float stateTime = 0f;
    public State currentState = State.IDLE;

    public void setState(State state) {
        if (currentState != state) {
            currentState = state;
            stateTime = 0f; // Reset state time when state changes
        }
    }
}
