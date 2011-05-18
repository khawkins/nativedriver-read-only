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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import java.io.IOException;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Encapsulates actions that can be accomplished with the Android Debug Bridge
 * (ADB) or the Android shell commands (such as
 * {@code adb shell cat FILENAME}). By using this class to invoke {@code adb},
 * you can customize the path to {@code adb} and override the port numbers used
 * by {@code adb}. (See {@link #AdbConnection(File, Integer, Integer, Integer)}
 * for more information).
 *
 * <p>For readability purposes, the configuration and construction of instances
 * of this class are performed with {@code AdbConnectionBuilder}.
 *
 * @author Matt DeVore
 * @author Gagan Gupta
 */
public class AdbConnection {
  @VisibleForTesting
  static final String IOCTL_RETURNBUFFERHEADER = "return buf: ";

  private final String adbPath;
  @Nullable private final Integer adbServerPort;
  @Nullable private final Integer emulatorConsolePort;
  @Nullable private final Integer emulatorAdbPort;

  public String getAdbPath() {
    return adbPath;
  }

  /**
   * Performs the {@code ioctl} command on device corresponding to the given
   * filename. This is equivalent to
   * {@code adb shell ioctl -rl (buffer.length) (filename) (requestCode)}.
   *
   * @param filename the name of the device file to perform the request on
   * @param requestCode the request code to perform
   * @param buffer where to store the output of the command. This array should
   *        be initialized to the size of the expected data to return.
   * @return the {@code buffer} parameter
   */
  public byte[] doIoctlForReading(
      String filename, int requestCode, int length) {
    byte[] buffer = new byte[length];
    Process adbProcess = runAdb("shell",
        "ioctl", "-rl", "" + buffer.length, filename, "" + requestCode);

    String rawOutput = outputAsString(adbProcess);

    confirmExitValueIs(0, adbProcess);

    int returnedDataIndex = rawOutput.indexOf(IOCTL_RETURNBUFFERHEADER);
    if (returnedDataIndex == -1) {
      throw new AdbException(
          "Could not find the return data header in ioctl output.");
    }

    rawOutput = rawOutput
        .substring(returnedDataIndex + IOCTL_RETURNBUFFERHEADER.length());
    // Now rawOutput is a string in the form of "xx xx xx ...", where "xx" is a
    // hexadecimal byte.

    for (int byteIndex = 0, stringIndex = 0; byteIndex < buffer.length;
        byteIndex++, stringIndex += 3) {
      int nextByte = Integer.parseInt(
          rawOutput.substring(stringIndex, stringIndex + 2), 16);

      buffer[byteIndex] = (byte) nextByte;
    }

    return buffer;
  }

  /**
   * Gets the contents of a file or device on the Android device. This is
   * equivalent to running {@code adb shell cat (FILENAME)} on the command
   * line.
   *
   * @param filename the path to the file to read from
   * @return a {@code Process} object representing the {@code cat} process
   */
  public Process catFile(String filename) {
    return runAdb("shell", "cat", filename);
  }

  /**
   * Runs {@code adb} using the given arguments and under the configuration
   * values passed to the constructor.
   *
   * @param arguments the arguments to pass to the {@code adb} utility
   * @return a {@code Process} object representing the {@code adb} process
   */
  protected Process runAdb(String... arguments) {
    List<String> commandLine = Lists.asList(adbPath, arguments);
    ProcessBuilder processBuilder = newProcessBuilder(commandLine);

    // If ports are initialized add them to the environment.
    Map<String, String> environment = processBuilder.environment();

    if (adbServerPort != null) {
      environment.put("ANDROID_ADB_SERVER_PORT", adbServerPort.toString());
    }
    if (emulatorConsolePort != null) {
      environment.put(
          "ANDROID_EMULATOR_CONSOLE_PORT", emulatorConsolePort.toString());
    }
    if (emulatorAdbPort != null) {
      environment.put("ANDROID_EMULATOR_ADB_PORT", emulatorAdbPort.toString());
    }

    try {
      return callProcessBuilderStart(processBuilder);
    } catch (IOException exception) {
      throw new AdbException(
          "An IOException occurred when starting ADB.", exception);
    }
  }

  @VisibleForTesting
  protected ProcessBuilder newProcessBuilder(List<String> commandLine) {
    return new ProcessBuilder(commandLine);
  }

  @VisibleForTesting
  protected Process callProcessBuilderStart(ProcessBuilder processBuilder)
      throws IOException {
    return processBuilder.start();
  }

  /**
   * Confirms the exit value of a process is equal to an expected value, and
   * throws an exception if it is not. This method will also wait for the
   * process to finish before checking the exit value.
   *
   * @param expected the expected exit value, usually {@code 0}
   * @param process the process whose exit value will be confirmed
   * @throws AdbException if the exit value was not equal to {@code expected}
   */
  public static void confirmExitValueIs(int expected, Process process) {
    // TODO(matvore): Consider if we need to add timeout logic here.
    while (true) {
      try {
        process.waitFor();
        break;
      } catch (InterruptedException exception) {
        // do nothing, try to wait again
      }
    }

    int actual = process.exitValue();

    if (expected != actual) {
      throw new AdbException("Exit value of process was " + actual
          + " but expected " + expected);
    }
  }

  private static String outputAsString(Process process) {
    try {
      return new String(ByteStreams.toByteArray(process.getInputStream()));
    } catch (IOException exception) {
      throw new AdbException(
          "Could not read output of ADB command.", exception);
    }
  }

  /**
   * Constructs a new instance which uses {@code adb} from a particular path and
   * possible overriding of configurable ports.
   *
   * <p>For readability purposes, the configuration and construction of
   * instances of this class should be performed with
   * {@link AdbConnectionBuilder}.
   *
   * @param adbPath the path to the {@code adb} utility
   * @param adbServerPort the port that the ADB daemon listens on to serve
   *        requests. If {@code null}, the default port (possibly 5037) is used.
   * @param emulatorConsolePort the port the emulator is listening on for
   *        console commands. If {@code null}, the default port (possibly 5554)
   *        is used.
   * @param emulatorAdbPort the port the emulator is listening on for ADB
   *        commands. If {@code null}, the default port (possibly 5555) is used.
   */
  protected AdbConnection(String adbPath, @Nullable Integer adbServerPort,
      @Nullable Integer emulatorConsolePort,
      @Nullable Integer emulatorAdbPort) {
    this.adbPath = Preconditions.checkNotNull(adbPath);
    this.adbServerPort = adbServerPort;
    this.emulatorConsolePort = emulatorConsolePort;
    this.emulatorAdbPort = emulatorAdbPort;
  }
}
