package io.github.Capstone_112_fall.components;

import com.badlogic.ashley.core.Component;

public class CheckpointComponent implements Component {
    public float x, y;
    public int order = -1;
    public boolean active = false;
    public boolean used = false;
}
