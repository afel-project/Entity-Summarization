/*
 * Copyright (c) 2008, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.scenario.effect;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.DirtyRegionPool;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.scenario.effect.impl.Renderer;
import com.sun.scenario.effect.impl.state.GaussianBlurState;
import com.sun.scenario.effect.impl.state.LinearConvolveKernel;

/**
 * A blur effect using a Gaussian convolution kernel, with a configurable
 * radius.
 */
public class GaussianBlur extends LinearConvolveCoreEffect {

    private GaussianBlurState state = new GaussianBlurState();

    /**
     * Constructs a new {@code GaussianBlur} effect with the default radius
     * (10.0), using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GaussianBlur(10f, DefaultInput)
     * </pre>
     */
    public GaussianBlur() {
        this(10f, DefaultInput);
    }

    /**
     * Constructs a new {@code GaussianBlur} effect with the given radius,
     * using the default input for source data.
     * This is a shorthand equivalent to:
     * <pre>
     *     new GaussianBlur(radius, DefaultInput)
     * </pre>
     *
     * @param radius the radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public GaussianBlur(float radius) {
        this(radius, DefaultInput);
    }

    /**
     * Constructs a new {@code GaussianBlur} effect with the given radius.
     *
     * @param radius the radius of the Gaussian kernel
     * @param input the single input {@code Effect}
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range, or if {@code input} is null
     */
    public GaussianBlur(float radius, Effect input) {
        super(input);
        state.setRadius(radius);
    }

    @Override
    LinearConvolveKernel getState() {
        return state;
    }

    @Override
    public AccelType getAccelType(FilterContext fctx) {
        return Renderer.getRenderer(fctx).getAccelType();
    }

    /**
     * Returns the input for this {@code Effect}.
     *
     * @return the input for this {@code Effect}
     */
    public final Effect getInput() {
        return getInputs().get(0);
    }

    /**
     * Sets the input for this {@code Effect} to a specific {@code Effect}
     * or to the default input if {@code input} is {@code null}.
     *
     * @param input the input for this {@code Effect}
     */
    public void setInput(Effect input) {
        setInput(0, input);
    }

    /**
     * Returns the radius of the Gaussian kernel.
     *
     * @return the radius of the Gaussian kernel
     */
    public float getRadius() {
        return state.getRadius();
    }

    /**
     * Sets the radius of the Gaussian kernel.
     * <pre>
     *       Min:  0.0
     *       Max: 63.0
     *   Default: 10.0
     *  Identity:  0.0
     * </pre>
     *
     * @param radius the radius of the Gaussian kernel
     * @throws IllegalArgumentException if {@code radius} is outside the
     * allowable range
     */
    public void setRadius(float radius) {
        float old = state.getRadius();
        state.setRadius(radius);
    }

    @Override
    public BaseBounds getBounds(BaseTransform transform, Effect defaultInput) {
        BaseBounds r = super.getBounds(null, defaultInput);
        int hpad = state.getPad(0);
        int vpad = state.getPad(1);
        RectBounds ret = new RectBounds(r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY());
        ret.grow(hpad, vpad);
        return transformBounds(transform, ret);
    }

    @Override
    public Rectangle getResultBounds(BaseTransform transform,
                                     Rectangle outputClip,
                                     ImageData... inputDatas)
    {
        Rectangle r = super.getResultBounds(transform, outputClip, inputDatas);
        int hpad = state.getPad(0);
        int vpad = state.getPad(1);
        Rectangle ret = new Rectangle(r);
        ret.grow(hpad, vpad);
        return ret;
    }

    @Override
    public boolean reducesOpaquePixels() {
        if (!state.isNop()) {
            return true;
        }
        final Effect input = getInput();
        return input != null && input.reducesOpaquePixels();
    }

    @Override
    public DirtyRegionContainer getDirtyRegions(Effect defaultInput, DirtyRegionPool regionPool) {
        Effect di = getDefaultedInput(0, defaultInput);
        DirtyRegionContainer drc = di.getDirtyRegions(defaultInput, regionPool);
        
        drc.grow(state.getPad(0), state.getPad(1));

        return drc;
    }
}
