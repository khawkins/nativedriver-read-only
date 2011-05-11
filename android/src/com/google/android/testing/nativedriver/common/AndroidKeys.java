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

import org.openqa.selenium.Keys;

/**
 * Contains keys that can be sent to Android Native driver implementations of
 * {@link org.openqa.selenium.WebElement#sendKeys}. These keys are not easily
 * represented as strings, so an instance of this {@code enum} can be used
 * instead. Each instance is a {@code CharSequence} that is exactly one
 * character long, and that character is a value in the Unicode private use
 * space.
 *
 * <p>For keys that are also in {@link Keys}, the key code in this class is the
 * same.
 *
 * @author Matt DeVore
 */
public enum AndroidKeys implements CharSequence {
  // Keys that are shared with normal WebDriver (sorted alphabetically)
  DEL(Keys.DELETE),
  DPAD_DOWN(Keys.ARROW_DOWN),
  DPAD_LEFT(Keys.ARROW_LEFT),
  DPAD_RIGHT(Keys.ARROW_RIGHT),
  DPAD_UP(Keys.ARROW_UP),
  ENTER(Keys.ENTER),

  // Keys only for native Android apps (sorted by key code)
  BACK('\uE100'),
  HOME('\uE101'),
  MENU('\uE102'),
  SEARCH('\uE103');

  private final char keyCode;

  private AndroidKeys(char keyCode) {
    this.keyCode = keyCode;
  }

  private AndroidKeys(Keys key) {
    keyCode = key.charAt(0);
  }

  public char getKeyCode() {
    return keyCode;
  }

  @Override
  public char charAt(int index) {
    if (index != 0) {
      throw new IndexOutOfBoundsException();
    }

    return keyCode;
  }

  @Override
  public int length() {
    return 1;
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    if (end == start) {
      return "";
    } else if (start == 0 && end == 1) {
      return this;
    } else {
      throw new IndexOutOfBoundsException();
    }
  }

  @Override
  public String toString() {
    return String.valueOf(keyCode);
  }
}
