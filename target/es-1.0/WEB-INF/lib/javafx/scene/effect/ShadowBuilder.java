/* 
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javafx.scene.effect;

/**
Builder class for javafx.scene.effect.Shadow
@see javafx.scene.effect.Shadow
@deprecated This class is deprecated and will be removed in the next version
* @since JavaFX 2.0
*/
@javax.annotation.Generated("Generated by javafx.builder.processor.BuilderProcessor")
@Deprecated
public class ShadowBuilder<B extends javafx.scene.effect.ShadowBuilder<B>> implements javafx.util.Builder<javafx.scene.effect.Shadow> {
    protected ShadowBuilder() {
    }
    
    /** Creates a new instance of ShadowBuilder. */
    @SuppressWarnings({"deprecation", "rawtypes", "unchecked"})
    public static javafx.scene.effect.ShadowBuilder<?> create() {
        return new javafx.scene.effect.ShadowBuilder();
    }
    
    private int __set;
    public void applyTo(javafx.scene.effect.Shadow x) {
        int set = __set;
        if ((set & (1 << 0)) != 0) x.setBlurType(this.blurType);
        if ((set & (1 << 1)) != 0) x.setColor(this.color);
        if ((set & (1 << 2)) != 0) x.setHeight(this.height);
        if ((set & (1 << 3)) != 0) x.setInput(this.input);
        if ((set & (1 << 4)) != 0) x.setRadius(this.radius);
        if ((set & (1 << 5)) != 0) x.setWidth(this.width);
    }
    
    private javafx.scene.effect.BlurType blurType;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getBlurType() blurType} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B blurType(javafx.scene.effect.BlurType x) {
        this.blurType = x;
        __set |= 1 << 0;
        return (B) this;
    }
    
    private javafx.scene.paint.Color color;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getColor() color} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B color(javafx.scene.paint.Color x) {
        this.color = x;
        __set |= 1 << 1;
        return (B) this;
    }
    
    private double height;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getHeight() height} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B height(double x) {
        this.height = x;
        __set |= 1 << 2;
        return (B) this;
    }
    
    private javafx.scene.effect.Effect input;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getInput() input} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B input(javafx.scene.effect.Effect x) {
        this.input = x;
        __set |= 1 << 3;
        return (B) this;
    }
    
    private double radius;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getRadius() radius} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B radius(double x) {
        this.radius = x;
        __set |= 1 << 4;
        return (B) this;
    }
    
    private double width;
    /**
    Set the value of the {@link javafx.scene.effect.Shadow#getWidth() width} property for the instance constructed by this builder.
    */
    @SuppressWarnings("unchecked")
    public B width(double x) {
        this.width = x;
        __set |= 1 << 5;
        return (B) this;
    }
    
    /**
    Make an instance of {@link javafx.scene.effect.Shadow} based on the properties set on this builder.
    */
    public javafx.scene.effect.Shadow build() {
        javafx.scene.effect.Shadow x = new javafx.scene.effect.Shadow();
        applyTo(x);
        return x;
    }
}