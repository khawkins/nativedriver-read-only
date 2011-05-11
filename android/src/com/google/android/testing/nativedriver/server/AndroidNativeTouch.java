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

import com.google.android.testing.nativedriver.common.Touch;
import com.google.common.base.Preconditions;

import android.app.Instrumentation;
import android.view.MotionEvent;

import org.openqa.selenium.Point;
import org.openqa.selenium.interactions.internal.Coordinates;
import org.openqa.selenium.support.ui.Clock;

import javax.annotation.Nullable;

/**
 * {@code Touch} interface implementation.
 *
 * @author Dezheng Xu
 */
public class AndroidNativeTouch implements Touch {
  private static final long UNDEFINED_TIME = Long.MIN_VALUE;
  private Coordinates currentActiveCoordinates;
  private final Clock clock;
  private final MotionEventUtils motionEventUtils;
  private long downTime = UNDEFINED_TIME;

  public AndroidNativeTouch(Clock clock, MotionEventUtils motionEventUtils) {
    this.clock = clock;
    this.motionEventUtils = motionEventUtils;
  }

  public static AndroidNativeTouch withDefaults(
      Instrumentation instrumentation) {
    Clock clock = new AndroidSystemClock();
    MotionEventUtils motionEventUtils = new MotionEventUtils(instrumentation);
    return new AndroidNativeTouch(clock, motionEventUtils);
  }

  @Override
  public void tap(@Nullable Coordinates where) {
    if (!isTouchStateReleased()) {
      throw new IllegalStateException(
          "Attempt to tap when touch state is already down");
    }
    updateActiveCoordinates(where);
    Point point = currentActiveCoordinates.getLocationOnScreen();
    downTime = clock.now();
    motionEventUtils.sendPointerSync(downTime, clock.now(),
        MotionEvent.ACTION_DOWN, point.getX(), point.getY());
    motionEventUtils.sendPointerSync(downTime, clock.now(),
        MotionEvent.ACTION_UP, point.getX(), point.getY());
    setTouchStateReleased();
  }

  @Override
  public void touchDown(@Nullable Coordinates where) {
    if (!isTouchStateReleased()) {
      throw new IllegalStateException(
          "Attempt to touch down when touch state is already down");
    }
    updateActiveCoordinates(where);
    Point point = currentActiveCoordinates.getLocationOnScreen();
    downTime = clock.now();
    motionEventUtils.sendPointerSync(downTime, clock.now(),
        MotionEvent.ACTION_DOWN, point.getX(), point.getY());
  }

  @Override
  public void touchUp(@Nullable Coordinates where) {
    if (isTouchStateReleased()) {
      throw new IllegalStateException(
          "Attempt to release touch when touch is already released");
    }
    updateActiveCoordinates(where);
    Point point = currentActiveCoordinates.getLocationOnScreen();
    motionEventUtils.sendPointerSync(downTime, clock.now(),
        MotionEvent.ACTION_UP, point.getX(), point.getY());
    setTouchStateReleased();
  }

  @Override
  public void touchMove(Coordinates where) {
    Preconditions.checkNotNull(where);
    updateActiveCoordinates(where);
    if (downTime != UNDEFINED_TIME) {
      Point point = where.getLocationOnScreen();
      motionEventUtils.sendPointerSync(downTime, clock.now(),
          MotionEvent.ACTION_MOVE, point.getX(), point.getY());
    }
  }

  private void updateActiveCoordinates(Coordinates coordinates) {
    if (coordinates != null) {
      currentActiveCoordinates = coordinates;
    } else if (currentActiveCoordinates == null) {
      throw new IllegalStateException(
          "No current active coordinates and given coordinates is null.");
    }
  }

  private void setTouchStateReleased() {
    downTime = UNDEFINED_TIME;
  }

  private boolean isTouchStateReleased() {
    return downTime == UNDEFINED_TIME;
  }
}
