/*
 * Copyright 2013-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package brave.context.rxjava2.internal;

import brave.propagation.CurrentTraceContext;
import brave.propagation.CurrentTraceContext.Scope;
import brave.propagation.TraceContext;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;

final class TraceContextObserver<T> implements Observer<T>, Disposable {
  final Observer<T> downstream;
  final CurrentTraceContext contextScoper;
  final TraceContext assembled;
  Disposable upstream;
  boolean done;

  TraceContextObserver(
    Observer<T> downstream, CurrentTraceContext contextScoper, TraceContext assembled) {
    this.downstream = downstream;
    this.contextScoper = contextScoper;
    this.assembled = assembled;
  }

  @Override public final void onSubscribe(Disposable d) {
    if (!Util.validate(upstream, d)) return;
    upstream = d;

    // Operators need to detect the fuseable feature of their immediate upstream. We pass "this"
    // to ensure downstream don't interface with the wrong operator (s).
    downstream.onSubscribe(this);
  }

  @Override public void onNext(T t) {
    Scope scope = contextScoper.maybeScope(assembled);
    try { // retrolambda can't resolve this try/finally
      downstream.onNext(t);
    } finally {
      scope.close();
    }
  }

  @Override public void onError(Throwable t) {
    if (done) {
      RxJavaPlugins.onError(t);
      return;
    }
    done = true;

    Scope scope = contextScoper.maybeScope(assembled);
    try { // retrolambda can't resolve this try/finally
      downstream.onError(t);
    } finally {
      scope.close();
    }
  }

  @Override public void onComplete() {
    if (done) return;
    done = true;

    Scope scope = contextScoper.maybeScope(assembled);
    try { // retrolambda can't resolve this try/finally
      downstream.onComplete();
    } finally {
      scope.close();
    }
  }

  @Override public void dispose() {
    upstream.dispose();
  }

  @Override public boolean isDisposed() {
    return upstream.isDisposed();
  }
}
