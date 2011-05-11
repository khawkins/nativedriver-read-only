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

import com.google.android.testing.nativedriver.common.FindsByText;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.FindsById;
import org.openqa.selenium.support.ui.TimeoutException;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Encapsulates element search functionality. An instance of this class knows
 * how to filter a sequence of {@code AndroidNativeElement}s using any of the
 * search methods supported by Android NativeDriver.
 *
 * <p>The currently-supported methods of searching are explained in detail
 * below.
 *
 * <h3>Finding by ID ({@link FindsById})</h3>
 *
 * <p>All elements are assigned at most one of each kind of the following IDs:
 * <ul>
 *   <li>An integer that is preceded by a {@code #}. The integer corresponds to
 *   the value returned by {@code View.getId} or {@code MenuItem.getItemId}
 *   <li>A string that corresponds to the string ID of an Android {@code View}
 *   or, more generally, a field in {@code R.id}. This is converted to an
 *   integer value by reading the field through reflection, the processed in the
 *   same way as the previous ID type
 *   <li>A string that begins with {@code $} which acts as a <em>literal</em>
 *   ID
 * </ul>
 *
 * <p>Any Android view ID which is associated with more than one view should be
 * used in a search from some parent element that only contains one of the
 * views, unless returning all views is desired. For instance, if there are two
 * views with ID "ambiguousId", but only one view is a descendant of "parent",
 * you can narrow your search with code like this:<br>
 *
 * <pre>androidNativeDriver.findElement(By.id("parent"))
 * .findElement(By.id("ambiguousId"))</pre>
 *
 * <p>Note that not all elements are capable of enumerating their children (see
 * the corresponding {@code WebElement} implementation's JavaDoc). In this case,
 * to find a child of element {@code x}, which does not support child
 * enumeration, both {@code x} and its child must be searched for
 * explicitly. For instance, if element {@code x} is an options menu:<br>
 *
 * <pre>androidNativeDriver.findElement(By.id("$optionsMenu"))
 * .findElement(By.id("child"))</pre>
 *
 * @author Matt DeVore
 * @author Tomohiro Kaizu
 */
public class ElementFinder {
  private final RClassReader rClassReader;
  private final AndroidWait wait;

  private static interface FilterCondition
      extends Predicate<AndroidNativeElement> {
    String notFoundExceptionMessage();
  }

  private static class ByAndroidIdPredicate
      implements Predicate<AndroidNativeElement> {
    private final Integer androidId;

    public ByAndroidIdPredicate(Integer androidId) {
      this.androidId = androidId;
    }

    @Override
    public boolean apply(AndroidNativeElement input) {
      return androidId.equals(input.getAndroidId());
    }
  }

  private static class ByLiteralIdPredicate
      implements Predicate<AndroidNativeElement> {
    private final String literalId;

    public ByLiteralIdPredicate(String literalId) {
      this.literalId = literalId;
    }

    @Override
    public boolean apply(AndroidNativeElement input) {
      return literalId.equals(input.getLiteralId());
    }
  }

  private static class ByTextFilterCondition implements FilterCondition {
    private final String text;

    public ByTextFilterCondition(String text) {
      this.text = text;
    }

    @Override
    public boolean apply(AndroidNativeElement input) {
      return text.equals(input.getText());
    }

    @Override
    public String notFoundExceptionMessage() {
      return "Could not find element with exact text: '" + text + "'";
    }
  }

  private static class ByPartialTextFilterCondition implements FilterCondition {
    private final String text;

    public ByPartialTextFilterCondition(String text) {
      this.text = text;
    }

    @Override
    public boolean apply(AndroidNativeElement input) {
      String elementText = input.getText();
      return (elementText != null) && (elementText.indexOf(text) != -1);
    }

    @Override
    public String notFoundExceptionMessage() {
      return "Could not find element containing text: '" + text + "'";
    }
  }

  private class SearchContextImpl
      implements SearchContext, FindsById, FindsByText {
    private final ElementSearchScope scope;

    private SearchContextImpl(ElementSearchScope scope) {
      this.scope = scope;
    }

    @Override
    public WebElement findElement(final By by) {
      try {
        return wait.until(new Function<Void, WebElement>() {
          @Override
          public WebElement apply(Void input) {
            return by.findElement(SearchContextImpl.this);
          }
        });
      } catch (TimeoutException exception) {
        if (exception.getCause() instanceof WebDriverException) {
          throw (WebDriverException) exception.getCause();
        } else {
          throw exception;
        }
      }
    }

    @Override
    public List<WebElement> findElements(final By by) {
      try {
        return wait.until(new Function<Void, List<WebElement>>() {
          @Override
          public List<WebElement> apply(Void input) {
            List<WebElement> found = by.findElements(SearchContextImpl.this);
            if (found.isEmpty()) {
              return null;
            } else {
              return found;
            }
          }
        });
      } catch (TimeoutException exception) {
        return ImmutableList.of();
      }
    }

    @Override
    public WebElement findElementById(String using) {
      if (isLiteralId(using)) {
        List<WebElement> result = findElementsById(using);

        if (!result.isEmpty()) {
          return result.get(0);
        }
      } else {
        Integer androidId = parseAsAndroidId(using);

        if (androidId != null) {
          WebElement result = scope.findElementByAndroidId(androidId);

          if (scope.equals(result)) {
            // The search returned the root element, but the root is not
            // included in the search according to the WebDriver's SearchContext
            // API. So delegate to the plural method finder, which only searches
            // children.
            result = Iterables.getFirst(findElementsById(using), null);
          }

          if (result != null) {
            return result;
          }
        }
      }

      throw new NoSuchElementException("Cannot find element with ID: " + using);
    }

    @Override
    public List<WebElement> findElementsById(String using) {
      Preconditions.checkNotNull(using);
      Predicate<AndroidNativeElement> filter;

      if (isLiteralId(using)) {
        filter = new ByLiteralIdPredicate(using);
      } else {
        Integer androidId = parseAsAndroidId(using);

        if (androidId == null) {
          return ImmutableList.of();
        }

        filter = new ByAndroidIdPredicate(androidId);
      }

      return addElementsFromHierarchy(Lists.<WebElement>newArrayList(),
          scope.getChildren(), filter, Integer.MAX_VALUE /* maxResults */);
    }

    @Override
    public WebElement findElementByText(String using) {
      Preconditions.checkNotNull(using);
      FilterCondition filter = new ByTextFilterCondition(using);
      return findElementFromHierarchy(scope.getChildren(), filter);
    }

    @Override
    public WebElement findElementByPartialText(String using) {
      Preconditions.checkNotNull(using);
      FilterCondition filter = new ByPartialTextFilterCondition(using);
      return findElementFromHierarchy(scope.getChildren(), filter);
    }

    @Override
    public List<WebElement> findElementsByText(String using) {
      Preconditions.checkNotNull(using);
      return addElementsFromHierarchy(Lists.<WebElement>newArrayList(),
          scope.getChildren(), new ByTextFilterCondition(using),
          Integer.MAX_VALUE /* maxResults */);
    }

    @Override
    public List<WebElement> findElementsByPartialText(String using) {
      Preconditions.checkNotNull(using);
      return addElementsFromHierarchy(Lists.<WebElement>newArrayList(),
          scope.getChildren(), new ByPartialTextFilterCondition(using),
          Integer.MAX_VALUE /* maxResults */);
    }
  }

  public ElementFinder(RClassReader rClassReader, AndroidWait wait) {
    this.rClassReader = rClassReader;
    this.wait = wait;
  }

  public RClassReader getRClassReader() {
    return rClassReader;
  }

  public AndroidWait getWait() {
    return wait;
  }

  public SearchContext getSearchContext(ElementSearchScope scope) {
    return new SearchContextImpl(scope);
  }

  private static boolean isLiteralId(String id) {
    return id.startsWith("$");
  }

  private static WebElement findElementFromHierarchy(
      Iterable<? extends AndroidNativeElement> topLevelElements,
      FilterCondition filter) {
    List<WebElement> result = addElementsFromHierarchy(
        Lists.<WebElement>newArrayList(), topLevelElements,
        filter, 1 /* maxResults */);

    if (result.isEmpty()) {
      throw new NoSuchElementException(filter.notFoundExceptionMessage());
    }

    return result.get(0);
  }

  private static List<WebElement> addElementsFromHierarchy(
      List<WebElement> destination,
      Iterable<? extends AndroidNativeElement> topLevelElements,
      Predicate<AndroidNativeElement> filter, int maxResults) {
    for (AndroidNativeElement element : topLevelElements) {
      if (destination.size() >= maxResults) {
        break;
      }

      if (filter.apply(element)) {
        destination.add(element);
      }

      addElementsFromHierarchy(
          destination, element.getChildren(), filter, maxResults);
    }

    return destination;
  }

  @Nullable
  private Integer parseAsAndroidId(String id) {
    if (id.startsWith("#")) {
      try {
        return Integer.parseInt(id.substring(1));
      } catch (NumberFormatException exception) {
        return null;
      }
    } else {
      return readIdFromR(id);
    }
  }

  /**
   * Converts the given ID (such as {@code TextView01}) to an integral Android
   * ID. This default implementation uses the {@code RClassReader} passed to the
   * constructor and reads the value of the ID from the {@code id} inner class.
   *
   * @param id the string ID to convert
   * @return the Android ID corresponding to the given string ID, or
   *         {@code null} if the given ID is invalid or not found
   */
  @Nullable
  protected Integer readIdFromR(String id) {
    return rClassReader.getRField("id", id);
  }
}
