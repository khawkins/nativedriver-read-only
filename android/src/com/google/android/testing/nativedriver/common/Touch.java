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

package com.google.android.testing.nativedriver.common;

import org.openqa.selenium.interactions.internal.Coordinates;

import javax.annotation.Nullable;

/**
 * Interface representing basic touch operations.
 *
 * @author Dezheng Xu
 */
public interface Touch {
  /**
   * Performs a tap at the given coordinates.
   *
   * @param where coordinates where the tap is performed.
   *        If null, the tap is performed at the last active coordinates.
   *
   * @throw IllegalStateException if there are no last active coordinates
   */
  void tap(@Nullable Coordinates where);

  /**
   * Issues a touch down event at the given coordinates.
   *
   * @param where coordinates where the touch down event is issued.
   *        If null, the touch down event is issued at the last active
   *        coordinates.
   *
   * @throw IllegalStateException if there are no last active coordinates
   */
  void touchDown(@Nullable Coordinates where);

  /**
   * Issues a touch up event at the given coordinates.
   *
   * @param where coordinates where the touch up event is issued.
   *        If null, the touch up event is issued at the last active
   *        coordinates.
   *
   * @throw IllegalStateException if there are no last active coordinates
   */
  void touchUp(@Nullable Coordinates where);

  /**
   * Issues a touch move event at the given coordinates.
   *
   * @param where destination to move to. It cannot be null.
   */
  void touchMove(Coordinates where);

  // TODO(dxu): once the touch interaction interface is added into the
  // Selenium source tree, replace usages of this interface with the Selenium
  // interface.
}
