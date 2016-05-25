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

package com.sun.javafx.tk.quantum;

import java.nio.IntBuffer;
import com.sun.glass.ui.Pixels;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.RTTexture;
import com.sun.prism.Texture.WrapMode;
import com.sun.prism.impl.Disposer;
import com.sun.prism.impl.QueuedPixelSource;

/**
 * UploadingPainter is used when we need to render into an offscreen buffer.
 * The PresentingPainter is used when we are rendering to the main screen.
 */
final class UploadingPainter extends ViewPainter implements Runnable {

    private RTTexture   rttexture;
    // resolveRTT is a temporary render target to "resolve" a msaa render buffer
    // into a normal color render target.
    private RTTexture   resolveRTT = null;

    private QueuedPixelSource pixelSource = new QueuedPixelSource(true);
    private float penScale;
    private volatile float pixScaleFactor = 1.0f;

    UploadingPainter(GlassScene view) {
        super(view);
    }

    void disposeRTTexture() {
        if (rttexture != null) {
            rttexture.dispose();
            rttexture = null;
        }
        if (resolveRTT != null) {
            resolveRTT.dispose();
            resolveRTT = null;
        }
    }
    
    public void setPixelScaleFactor(float scale) {
        pixScaleFactor = scale;
    }
    
    @Override
    public float getPixelScaleFactor() {
        return pixScaleFactor;
    }    

    @Override public void run() {
        renderLock.lock();

        boolean errored = false;
        try {
            if (!validateStageGraphics()) {
                if (QuantumToolkit.verbose) {
                    System.err.println("UploadingPainter: validateStageGraphics failed");
                }
                paintImpl(null);
                return;
            }
            
            if (factory == null) {
                factory = GraphicsPipeline.getDefaultResourceFactory();
            }
            if (factory == null || !factory.isDeviceReady()) {
                return;
            }

            float scale = pixScaleFactor;
            int bufWidth = Math.round(viewWidth * scale);
            int bufHeight = Math.round(viewHeight * scale);

            boolean needsReset = (penScale != scale ||
                                  penWidth != viewWidth ||
                                  penHeight != viewHeight ||
                                  rttexture == null);

            if (!needsReset) {
                rttexture.lock();
                if (rttexture.isSurfaceLost()) {
                    rttexture.unlock();
                    sceneState.getScene().entireSceneNeedsRepaint();
                    needsReset = true;
                }
            }

            if (needsReset) {
                disposeRTTexture();
                rttexture = factory.createRTTexture(bufWidth, bufHeight, WrapMode.CLAMP_NOT_NEEDED,
                        sceneState.isMSAA());
                if (rttexture == null) {
                    return;
                }
                penScale    = scale;
                penWidth    = viewWidth;
                penHeight   = viewHeight;
                freshBackBuffer = true;
            }
            Graphics g = rttexture.createGraphics();
            if (g == null) {
                disposeRTTexture();
                sceneState.getScene().entireSceneNeedsRepaint();
                return;
            }
            g.scale(scale, scale);
            paintImpl(g);
            freshBackBuffer = false;

            Pixels pix = pixelSource.getUnusedPixels(bufWidth, bufHeight, scale);
            IntBuffer bits = (IntBuffer) pix.getPixels();

            int rawbits[] = rttexture.getPixels();
            
            if (rawbits != null) {
                bits.put(rawbits, 0, bufWidth * bufHeight);
            } else {
                RTTexture rtt = rttexture.isMSAA() ?
                    resolveRenderTarget(g) : rttexture;

                if (!rtt.readPixels(bits)) {
                    /* device lost */
                    sceneState.getScene().entireSceneNeedsRepaint();
                    disposeRTTexture();
                    pix = null;
                }
            }

            if (rttexture != null) {
                rttexture.unlock();
            }

            if (pix != null) {
                /* transparent pixels created and ready for upload */
                // Copy references, which are volatile, used by upload. Thus
                // ensure they still exist once event queue is consumed.
                pixelSource.enqueuePixels(pix);
                sceneState.uploadPixels(pixelSource);
            }
                
        } catch (Throwable th) {
            errored = true;
            th.printStackTrace(System.err);
        } finally {
            if (rttexture != null && rttexture.isLocked()) {
                rttexture.unlock();
            }
            if (resolveRTT != null && resolveRTT.isLocked()) {
                resolveRTT.unlock();
            }

            Disposer.cleanUp();

            sceneState.getScene().setPainting(false);

            if (factory != null) {
                factory.getTextureResourcePool().freeDisposalRequestedAndCheckResources(errored);
            }

            renderLock.unlock();
        }
    }

    private RTTexture resolveRenderTarget(Graphics g) {
        int width = rttexture.getContentWidth();
        int height = rttexture.getContentHeight();
        if (resolveRTT != null &&
                (resolveRTT.getContentWidth() != width ||
                (resolveRTT.getContentHeight() != height)))
        {
            // If msaa rtt is not the same size than resolve buffer, then dispose
            resolveRTT.dispose();
            resolveRTT = null;
        }
        if (resolveRTT == null || resolveRTT.isSurfaceLost()) {
            resolveRTT = g.getResourceFactory().createRTTexture(
                    width, height,
                    WrapMode.CLAMP_NOT_NEEDED, false);
        } else {
            resolveRTT.lock();
        }
        g.blit(rttexture, resolveRTT, 0, 0, width, height, 0, 0, width, height);
        return resolveRTT;
    }
}
