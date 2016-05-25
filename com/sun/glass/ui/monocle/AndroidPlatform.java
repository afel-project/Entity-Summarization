/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle;

/** AndroidPlatform matches any Linux system */
class AndroidPlatform extends NativePlatform {

    AndroidPlatform() {
    }

    @Override
    protected InputDeviceRegistry createInputDeviceRegistry() {
        return new InputDeviceRegistry();
    }

    @Override
    protected NativeCursor createCursor() {
        return null;
    }

    @Override
    protected NativeScreen createScreen() {
        return null;
    }
}
