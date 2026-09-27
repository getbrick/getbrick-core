/*
 * Copyright 2026 Getbrick.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.getbrick.common.exception;

import java.io.Serial;

/**
 * Checked base type for failures a caller is expected to handle.
 *
 * <p>Use it when losing data or an invariant is a real possibility. Anything that should be handled
 * higher up the stack without forcing a {@code throws} clause belongs in {@link
 * GetbrickRuntimeException} instead.
 */
public class GetbrickException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates an exception with a message.
   *
   * @param message description of what failed
   */
  public GetbrickException(String message) {
    super(message);
  }

  /**
   * Creates an exception with a message and a cause.
   *
   * @param message description of what failed
   * @param cause the underlying failure
   */
  public GetbrickException(String message, Throwable cause) {
    super(message, cause);
  }
}
