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

package com.google.android.testing.nativedriver.client;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.flags.Flag;
import com.google.common.flags.FlagSpec;
import com.google.common.flags.InvalidFlagValueException;

import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.HttpCommandExecutor;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nullable;

/**
 * Configures and creates {@link AndroidNativeDriver} instances.
 *
 * @see AndroidNativeDriver#AndroidNativeDriver(CommandExecutor, AdbConnection)
 * @author Matt DeVore
 * @author Dezheng Xu
 */
public class AndroidNativeDriverBuilder {
  private static final int DEFAULT_SERVER_PORT = 54129;

  private static final String DEFAULT_SERVER_URL
      = "http://localhost:" + DEFAULT_SERVER_PORT + "/hub";

  @FlagSpec(help="Set the Android NativeDriver server URL")
  private static final Flag<URL> FLAG_and_server_url;

  static {
    URL defaultServerUrl;
    try {
      defaultServerUrl = new URL(DEFAULT_SERVER_URL);
    } catch (MalformedURLException exception) {
      throw Throwables.propagate(exception);
    }
    FLAG_and_server_url = new Flag<URL>(defaultServerUrl) {
      @Override
      protected URL parse(String url) throws InvalidFlagValueException {
        try {
          return new URL(url);
        } catch (MalformedURLException exception) {
          throw new InvalidFlagValueException(url);
        }
      }
    };
  }

  @Nullable private CommandExecutor commandExecutor;
  @Nullable private AdbConnection adbConnection;

  public AndroidNativeDriverBuilder withAdbConnection(
      @Nullable AdbConnection adbConnection) {
    this.adbConnection = adbConnection;
    return this;
  }

  /**
   * Set the server using the command-line flag {@code and_server_url} if
   * available. It can be specified by command-line:
   *   <p><test_cases_binary> --and_server_url=<URL> <test_case_class_name>
   *
   * <p>If the command-line flag is not specified, the URL is set to
   * {@code http://localhost:54129/hub}
   *
   * @see com.google.common.flags.Flags#parse(String[])
   */
  public AndroidNativeDriverBuilder withDefaultServer() {
    return withServer(FLAG_and_server_url.get());
  }

  public AndroidNativeDriverBuilder withServer(URL url) {
    this.commandExecutor
        = new HttpCommandExecutor(Preconditions.checkNotNull(url));
    return this;
  }

  public AndroidNativeDriverBuilder
      withCommandExecutor(CommandExecutor commandExecutor) {
    this.commandExecutor = Preconditions.checkNotNull(commandExecutor);
    return this;
  }

  public AndroidNativeDriver build() {
    return new AndroidNativeDriver(
        Preconditions.checkNotNull(commandExecutor), adbConnection);
  }
}
