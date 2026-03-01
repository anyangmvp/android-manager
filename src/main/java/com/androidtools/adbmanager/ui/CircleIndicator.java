package com.androidtools.adbmanager.ui;

import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * 圆形状态指示器
 */
public class CircleIndicator extends Circle {
    
    private boolean active = false;
    
    public CircleIndicator() {
        super(8);
        updateColor();
    }
    
    public void setActive(boolean active) {
        this.active = active;
        updateColor();
    }
    
    private void updateColor() {
        if (active) {
            setFill(javafx.scene.paint.Color.web("#48bb78"));
            setStroke(javafx.scene.paint.Color.web("#2f855a"));
        } else {
            setFill(javafx.scene.paint.Color.web("#fc8181"));
            setStroke(javafx.scene.paint.Color.web("#c53030"));
        }
        setStrokeWidth(2);
        getStyleClass().clear();
        getStyleClass().add(active ? "circle-indicator" : "circle-indicator-inactive");
    }
}
