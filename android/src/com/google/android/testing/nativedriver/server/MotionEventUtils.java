/*
Copyright 2011 NativeDriver committers
Copyright 2011 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.google.android.testing.nativedriver.server;

import android.app.Instrumentation;
import android.view.MotionEvent;

/**
 * A wrapper to generate and send touch-specific {@code MotionEvent}.
 *
 * @author Dezheng Xu
 */
public class MotionEventUtils {
  private static final int META_STATE = 0;

  private final Instrumentation instrumentation;

  public MotionEventUtils(Instrumentation instrumentation) {
    this.instrumentation = instrumentation;
  }

  /**
   * Create a new {@code MotionEvent}, filling in a subset of the basic motion
   * values and send it.
   *
   * @param downTime the time (in ms) when the user originally pressed down to
   *        start a stream of position events
   * @param eventTime the time (in ms) when this specific event was
   *        generated
   * @param action the kind of action being performed. It can be one of the
   *        following constants in {@code MotionEvent}:
   *        {@code ACTION_DOWN}, {@code ACTION_MOVE}, {@code ACTION_UP}
   * @param x the X coordinate of this event
   * @param y the Y coordinate of this event
   */
  public void sendPointerSync(
      long downTime, long eventTime, int action, float x, float y) {
    MotionEvent motionEvent
        = MotionEvent.obtain(downTime, eventTime, action, x, y, META_STATE);
    instrumentation.waitForIdleSync();
    instrumentation.sendPointerSync(motionEvent);

    // TODO(dxu): think about how to deal with ACTION_CANCEL
  }
}
