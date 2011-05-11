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

import com.google.android.testing.nativedriver.common.AndroidKeys;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import android.app.Instrumentation;
import android.view.KeyEvent;

import java.util.Map;

/**
 * Provides a method to send a string to an application under test. Keys are
 * sent using an {@code Instrumentation} instance. The strings may contain any
 * character in the {@link AndroidKeys} {@code enum}.
 *
 * <p>Note that this class does not focus on a particular {@code View} before
 * sending keys, nor does it require that some {@code View} has focus. This is
 * fine if you are sending the Menu key, or using the arrow keys to select an
 * item in a list. If you are trying to type into a certain widget, be sure it
 * has focus before using this class.
 *
 * @author Matt DeVore
 */
public class KeySender {
  /**
   * A map of character codes in the Unicode Private Use Area to the
   * corresponding Android key code.
   */
  private static final Map<Character, Integer> SPECIAL_KEYS;

  private final Instrumentation instrumentation;

  static {
    SPECIAL_KEYS = ImmutableMap.<Character, Integer>builder()
        .put(AndroidKeys.DPAD_DOWN.getKeyCode(), KeyEvent.KEYCODE_DPAD_DOWN)
        .put(AndroidKeys.DPAD_LEFT.getKeyCode(), KeyEvent.KEYCODE_DPAD_LEFT)
        .put(AndroidKeys.DPAD_RIGHT.getKeyCode(), KeyEvent.KEYCODE_DPAD_RIGHT)
        .put(AndroidKeys.DPAD_UP.getKeyCode(), KeyEvent.KEYCODE_DPAD_UP)
        .put(AndroidKeys.BACK.getKeyCode(), KeyEvent.KEYCODE_BACK)
        .put(AndroidKeys.DEL.getKeyCode(), KeyEvent.KEYCODE_DEL)
        .put(AndroidKeys.ENTER.getKeyCode(), KeyEvent.KEYCODE_ENTER)
        .put(AndroidKeys.HOME.getKeyCode(), KeyEvent.KEYCODE_HOME)
        .put(AndroidKeys.MENU.getKeyCode(), KeyEvent.KEYCODE_MENU)
        .put(AndroidKeys.SEARCH.getKeyCode(), KeyEvent.KEYCODE_SEARCH)
        .build();
  }

  /**
   * Creates a new instance which sends keys to the given
   * {@code Instrumentation}.
   */
  public KeySender(Instrumentation instrumentation) {
    this.instrumentation = Preconditions.checkNotNull(instrumentation);
  }

  private static boolean isSpecialKey(char character) {
    return SPECIAL_KEYS.keySet().contains(character);
  }

  private static int indexOfSpecialKey(CharSequence string, int startIndex) {
    for (int i = startIndex; i < string.length(); i++) {
      if (isSpecialKey(string.charAt(i))) {
        return i;
      }
    }

    return string.length();
  }

  /**
   * Sends key events to the {@code Instrumentation}. This method will send
   * a portion of the given {@code CharSequence} as a single {@code String} if
   * the portion does not contain any special keys.
   *
   * @param string the keys to send to the {@code Instrumentation}.
   */
  public void send(CharSequence string) {
    int currentIndex = 0;

    instrumentation.waitForIdleSync();

    while (currentIndex < string.length()) {
      char currentCharacter = string.charAt(currentIndex);
      if (isSpecialKey(currentCharacter)) {
        // The next character is special and must be sent individually
        instrumentation.sendKeyDownUpSync(SPECIAL_KEYS.get(currentCharacter));
        currentIndex++;
      } else {
        // There is at least one "normal" character, that is a character
        // represented by a plain Unicode character that can be sent with
        // sendStringSync. So send as many such consecutive normal characters
        // as possible in a single String.
        int nextSpecialKey = indexOfSpecialKey(string, currentIndex);
        instrumentation.sendStringSync(
            string.subSequence(currentIndex, nextSpecialKey).toString());
        currentIndex = nextSpecialKey;
      }
    }
  }
}
