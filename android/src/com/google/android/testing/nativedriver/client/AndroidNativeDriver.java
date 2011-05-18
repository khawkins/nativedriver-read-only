/*
Copyright 2010 NativeDriver committers
Copyright 2010 Google Inc.

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

package com.google.android.testing.nativedriver.client;

import com.google.android.testing.nativedriver.common.AndroidCapabilities;
import com.google.android.testing.nativedriver.common.FindsByText;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import org.openqa.selenium.By;
import org.openqa.selenium.Rotatable;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.remote.internal.JsonToWebElementConverter;

import java.net.URL;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Represents an Android NativeDriver (AND) client used to drive native
 * Android applications.
 *
 * @author Matt DeVore
 * @author Dezheng Xu
 * @author Tomohiro Kaizu
 */
public class AndroidNativeDriver
    extends RemoteWebDriver implements FindsByText, Rotatable {
  @Nullable
  private final AdbConnection adbConnection;

  /**
   * A {@code Navigation} class for native Android applications. Provides
   * {@link #toActivity(String)} in addition to the standard {@code Navigation}
   * methods.
   */
  public class AndroidNativeNavigation implements Navigation {
    private final Navigation navigation;

    private AndroidNativeNavigation(Navigation navigation) {
      this.navigation = navigation;
    }

    public void toActivity(String activityClass) {
      startActivity(activityClass);
    }

    @Override
    public void back() {
      navigation.back();
    }

    @Override
    public void forward() {
      navigation.forward();
    }

    @Override
    public void to(String url) {
      navigation.to(url);
    }

    @Override
    public void to(URL url) {
      navigation.to(url);
    }

    @Override
    public void refresh() {
      navigation.refresh();
    }
  }

  @Deprecated
  @Override
  protected RemoteWebElement newRemoteWebElement() {
    return new AndroidNativeElement(this);
  }

  protected AndroidNativeDriver(CommandExecutor executor) {
    this(executor, null);
  }

  /**
   * Creates an instance which routes all commands to a {@code CommandExecutor}.
   * A mock can be passed as an argument to help with testing. This constructor
   * also takes an {@code AdbConnection}, to which all ADB commands will be
   * sent.
   *
   * @param executor a command executor through which all commands are
   *        routed. Using a mock eliminates the need to connect to an HTTP
   *        server, where the commands are usually routed.
   * @param adbConnection receives all ADB commands, such as event injections.
   *        If {@code null}, this instance will not support ADB functionality.
   * @see AndroidNativeDriverBuilder
   */
  protected AndroidNativeDriver(
      CommandExecutor executor, @Nullable AdbConnection adbConnection) {
    super(Preconditions.checkNotNull(executor), AndroidCapabilities.get());
    setElementConverter(new JsonToWebElementConverter(this) {
        @Override
        protected RemoteWebElement newRemoteWebElement() {
          return new AndroidNativeElement(AndroidNativeDriver.this);
        }
    });
    this.adbConnection = adbConnection;
  }

  /**
   * @deprecated use {@link AndroidNativeDriverBuilder}
   */
  @Deprecated
  public static AndroidNativeDriver withDefaultServer() {
    return new AndroidNativeDriverBuilder()
        .withDefaultServer()
        .build();
  }

  /**
   * @deprecated use {@link AndroidNativeDriverBuilder}
   */
  @Deprecated
  public static AndroidNativeDriver withExecutor(CommandExecutor executor) {
    return new AndroidNativeDriver(executor, null);
  }

  /**
   * @deprecated use {@link AndroidNativeDriverBuilder}
   */
  @Deprecated
  public static AndroidNativeDriver withServer(URL remoteAddress) {
    return new AndroidNativeDriverBuilder()
        .withServer(remoteAddress)
        .build();
  }

  /**
   * Start a new activity either in a new task or the current
   * task. This is done by calling {@code get()} with a coded
   * URL. When not starting in the current task, the activity will
   * start in the task of the currently-focused activity.
   *
   * @param activityClass The full package and class name of the activity.
   */
  public void startActivity(String activityClass) {
    get("and-activity://" + activityClass);
  }

  @Override
  public WebElement findElementByPartialText(String using) {
    return findElement(USING_PARTIALTEXT, using);
  }

  @Override
  public WebElement findElementByText(String using) {
    return findElement(USING_TEXT, using);
  }

  @Override
  public List<WebElement> findElementsByPartialText(String using) {
    return findElements(USING_PARTIALTEXT, using);
  }

  @Override
  public List<WebElement> findElementsByText(String using) {
    return findElements(USING_TEXT, using);
  }

  @SuppressWarnings("unchecked")
  public List<AndroidNativeElement> findAndroidNativeElements(By by) {
    return (List) findElements(by);
  }

  @Override
  public AndroidNativeElement findElement(By by) {
    return (AndroidNativeElement) super.findElement(by);
  }

  @Override
  public void rotate(ScreenOrientation orientation) {
    // Refers to org.openqa.selenium.android.AndroidDriver
    execute(DriverCommand.SET_SCREEN_ORIENTATION,
        ImmutableMap.of("orientation", orientation));
  }

  @Override
  public ScreenOrientation getOrientation() {
    // Refers to org.openqa.selenium.android.AndroidDriver
    return ScreenOrientation.valueOf(
        (String) execute(DriverCommand.GET_SCREEN_ORIENTATION).getValue());
  }

  @Override
  public AndroidNativeNavigation navigate() {
    return new AndroidNativeNavigation(super.navigate());
  }
}
