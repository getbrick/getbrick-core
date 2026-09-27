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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetbrickExceptionTest {

  @Test
  @DisplayName("carries the message it was created with")
  void carriesMessage() {
    var exception = new GetbrickException("bean definition is circular");

    assertThat(exception).hasMessage("bean definition is circular");
  }

  @Test
  @DisplayName("keeps the cause when one is supplied")
  void keepsCause() {
    var cause = new IOException("connection reset");

    assertThatThrownBy(
            () -> {
              throw new GetbrickException("cannot load remote catalog", cause);
            })
        .isInstanceOf(GetbrickException.class)
        .hasMessage("cannot load remote catalog")
        .hasCause(cause);
  }

  @Test
  @DisplayName("has no cause when none is supplied")
  void hasNoCauseByDefault() {
    assertThat(new GetbrickException("missing configuration key")).hasNoCause();
  }

  @Test
  @DisplayName("runtime variant is unchecked and serialisable")
  void runtimeVariantIsUnchecked() {
    var exception = new GetbrickRuntimeException("invalid bean scope", new IllegalStateException());

    assertThat(RuntimeException.class).isAssignableFrom(GetbrickRuntimeException.class);
    assertThat(exception)
        .hasMessage("invalid bean scope")
        .hasCauseInstanceOf(IllegalStateException.class);
    assertThat((Object) exception).isInstanceOf(java.io.Serializable.class);
  }

  @Test
  @DisplayName("runtime variant carries only a message when no cause is given")
  void runtimeVariantWithoutCause() {
    assertThat(new GetbrickRuntimeException("unresolved placeholder ${db.url}")).hasNoCause();
  }
}
