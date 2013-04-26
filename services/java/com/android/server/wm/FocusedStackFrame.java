/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.wm;

import static com.android.server.wm.WindowManagerService.DEBUG_STACK;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceSession;

class FocusedStackFrame {
    private static final String TAG = "FocusedStackFrame";
    private static final int THICKNESS = 10;
    private static final float ALPHA = 0.3f;

    private final SurfaceControl mSurfaceControl;
    private final Surface mSurface = new Surface();
    private Rect mLastBounds = new Rect();
    private Rect mBounds = new Rect();
    private Rect mTmpDrawRect = new Rect();

    public FocusedStackFrame(Display display, SurfaceSession session) {
        SurfaceControl ctrl = null;
        try {
            ctrl = new SurfaceControl(session, "FocusedStackFrame",
                1, 1, PixelFormat.TRANSLUCENT, SurfaceControl.HIDDEN);
            ctrl.setLayerStack(display.getLayerStack());
            ctrl.setLayer(WindowManagerService.TYPE_LAYER_MULTIPLIER * 102);
            ctrl.setAlpha(ALPHA);
            mSurface.copyFrom(ctrl);
        } catch (SurfaceControl.OutOfResourcesException e) {
        }
        mSurfaceControl = ctrl;
    }

    private void draw(Rect bounds, int color) {
        if (DEBUG_STACK) Slog.i(TAG, "draw: bounds=" + bounds.toShortString() +
                " color=" + Integer.toHexString(color));
        mTmpDrawRect.set(bounds);
        Canvas c = null;
        try {
            c = mSurface.lockCanvas(mTmpDrawRect);
        } catch (IllegalArgumentException e) {
        } catch (Surface.OutOfResourcesException e) {
        }
        if (c == null) {
            return;
        }

        final int w = bounds.width();
        final int h = bounds.height();

        // Top
        mTmpDrawRect.set(0, 0, w, THICKNESS);
        c.clipRect(mTmpDrawRect, Region.Op.REPLACE);
        c.drawColor(color);
        // Left (not including Top or Bottom stripe).
        mTmpDrawRect.set(0, THICKNESS, THICKNESS, h - THICKNESS);
        c.clipRect(mTmpDrawRect, Region.Op.REPLACE);
        c.drawColor(color);
        // Right (not including Top or Bottom stripe).
        mTmpDrawRect.set(w - THICKNESS, THICKNESS, w, h - THICKNESS);
        c.clipRect(mTmpDrawRect, Region.Op.REPLACE);
        c.drawColor(color);
        // Bottom
        mTmpDrawRect.set(0, h - THICKNESS, w, h);
        c.clipRect(mTmpDrawRect, Region.Op.REPLACE);
        c.drawColor(color);

        mSurface.unlockCanvasAndPost(c);
    }

    private void positionSurface(Rect bounds) {
        if (DEBUG_STACK) Slog.i(TAG, "positionSurface: bounds=" + bounds.toShortString());
        mSurfaceControl.setSize(bounds.width(), bounds.height());
        mSurfaceControl.setPosition(bounds.left, bounds.top);
    }

    // Note: caller responsible for being inside
    // Surface.openTransaction() / closeTransaction()
    public void setVisibility(boolean on) {
        if (DEBUG_STACK) Slog.i(TAG, "setVisibility: on=" + on +
                " mLastBounds=" + mLastBounds.toShortString() +
                " mBounds=" + mBounds.toShortString());
        if (mSurfaceControl == null) {
            return;
        }
        if (on) {
            if (!mLastBounds.equals(mBounds)) {
                // Erase the previous rectangle.
                positionSurface(mLastBounds);
                draw(mLastBounds, Color.TRANSPARENT);
                // Draw the latest rectangle.
                positionSurface(mBounds);
                draw(mBounds, Color.WHITE);
                // Update the history.
                mLastBounds.set(mBounds);
            }
            mSurfaceControl.show();
        } else {
            mSurfaceControl.hide();
        }
    }

    public void setBounds(Rect bounds) {
        if (DEBUG_STACK) Slog.i(TAG, "setBounds: bounds=" + bounds);
        mBounds.set(bounds);
    }
}