package eu.toolchain.async;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * The asynchronous framework.
 * <p>
 * This type is intended to be passed around in your application, preferably through dependency
 * injection.
 * <p>
 * It makes the contract between the framework and your application decoupled, which has several
 * benefits for your application's code (see README for details).
 * <p>
 * All methods exposed are fully thread-safe.
 *
 * @author udoprog
 */
public interface AsyncFramework {
    /**
     * Retrieve the default caller.
     *
     * @return The default caller.
     */
    AsyncCaller caller();

    /**
     * Retrieve a caller implementation that is threaded, or fail if none is available.
     *
     * @return An async caller that is threaded.
     */
    AsyncCaller threadedCaller();

    /**
     * Build a new resolvable future.
     * <p>
     * The future is returned in a running state, and can be resolved, failed, or cancelled. See
     * documentation for {@link AsyncFuture} for details on the various states.
     * <p>
     * These futures are guaranteed to be thread-safe, all of their methods can be called from any
     * thread, at any time.
     *
     * @param <T> type of the future.
     * @return A new <em>resolvable</em> future.
     */
    <T> ResolvableFuture<T> future();

    /**
     * Returns an already resolved void future.
     * <p>
     * The future is immediately resolved with a {@code null} value.
     *
     * @return A new <em>already resolved</em> future.
     * @see #resolved(Object)
     */
    AsyncFuture<Void> resolved();

    /**
     * Build an already resolved future.
     *
     * @param value The value which the future was resolved using.
     * @param <T> type of the future.
     * @return A new resolved future.
     */
    <T> AsyncFuture<T> resolved(T value);

    /**
     * Build an already failed future.
     *
     * @param e The Error which the future is failed using.
     * @param <T> type of the future.
     * @return A new <em>failed</em> future.
     */
    <T> AsyncFuture<T> failed(Throwable e);

    /**
     * Build an immediately cancelled future.
     *
     * @param <T> type of the future.
     * @return A new cancelled future.
     */
    <T> AsyncFuture<T> cancelled();

    /**
     * Transform a future of type C, to a future of type T.
     * <p>
     * Use {@link AsyncFuture#transform(Transform)} instead of this directly.
     *
     * @param future A future of type C to transform.
     * @param transform The transforming implementation to use.
     * @param <S> source type of the future.
     * @param <T> target type the future is being transformed into.
     * @return A new future of type T.
     */
    <S, T> AsyncFuture<T> transform(
        AsyncFuture<S> future, Transform<? super S, ? extends T> transform
    );

    /**
     * Transform a future of type C, to a future of type T using lazy transformation.
     * <p>
     * Use {@link AsyncFuture#lazyTransform(LazyTransform)} instead of this directly.
     * <p>
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future A future of type C to transform.
     * @param transform The transforming implementation to use.
     * @param <S> source type of the future.
     * @param <T> target type the future is being transformed into.
     * @return A new future of type T.
     */
    <S, T> AsyncFuture<T> transform(
        AsyncFuture<S> future, LazyTransform<? super S, ? extends T> transform
    );

    /**
     * Transform a failing future into a resolved future.
     * <p>
     * Use {@link AsyncFuture#catchFailed(Transform)} instead of this directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @return A new future which does not fail.
     */
    <T> AsyncFuture<T> error(AsyncFuture<T> future, Transform<Throwable, ? extends T> transform);

    /**
     * Transform a failing future into a resolved future.
     * <p>
     * Use {@link AsyncFuture#catchFailed(Transform)} instead of this directly.
     * <p>
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not fail.
     */
    <T> AsyncFuture<T> error(
        AsyncFuture<T> future, LazyTransform<Throwable, ? extends T> transform
    );

    /**
     * Transform a cancelled future into a resolved future.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not cancel.
     * @see AsyncFuture#catchCancelled(Transform)
     */
    <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, Transform<Void, ? extends T> transform);

    /**
     * Transform a cancelled future into a resolved future.
     * <p>
     * Lazy transformations returns another future instead of the result directly.
     *
     * @param future The failing future to transform.
     * @param transform The transform implementation to use.
     * @param <T> type of the transformed future.
     * @return A new future which does not cancel.
     * @see AsyncFuture#lazyCatchCancelled(LazyTransform)
     */
    <T> AsyncFuture<T> cancelled(AsyncFuture<T> future, LazyTransform<Void, ? extends T> transform);

    /**
     * Build a new future that is the result of collecting all the results in a collection.
     *
     * @param futures The collection of future to collect.
     * @param <T> type of the collected future.
     * @return A new future that is the result of collecting all results.
     */
    <T> AsyncFuture<Collection<T>> collect(Collection<? extends AsyncFuture<? extends T>> futures);

    /**
     * Build a new future that is the result of reducing the provided collection of futures using
     * the provided collector.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A new future that is the result of reducing the collection of futures.
     */
    <S, T> AsyncFuture<T> collect(
        Collection<? extends AsyncFuture<? extends S>> futures,
        Collector<? super S, ? extends T> collector
    );

    /**
     * Build a new future that is the result of reducing the provided collection of futures using
     * the provided collector.
     * <p>
     * This is similar to {@link #collect(Collection, Collector)}, but uses {@link StreamCollector}
     * which operates on the stream of results as they arrive.
     * <p>
     * This allows the implementor to reduce memory usage for certain operations since all results
     * does not have to be collected.
     * <p>
     * If the returned future ends up in a non-resolved state, this will be forwarded to the given
     * list of futures as well.
     *
     * @param futures The collection of futures to collect.
     * @param collector The implementation for how to reduce the collected futures.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A new future that is the result of reducing the collection of futures.
     */
    <S, T> AsyncFuture<T> collect(
        Collection<? extends AsyncFuture<? extends S>> futures,
        StreamCollector<? super S, ? extends T> collector
    );

    /**
     * Collect the results from a collection of futures, then discard them.
     * <p>
     * Signals like cancellations and failures will be communicated in a similar fashion to {@link
     * #collect(Collection, StreamCollector)}.
     *
     * @param futures The collection of futures to collect.
     * @param <T> type of the futures being collected and discarded.
     * @return A new future that is the result of collecting the provided futures, but discarding
     * their results.
     */
    <T> AsyncFuture<Void> collectAndDiscard(Collection<? extends AsyncFuture<T>> futures);

    /**
     * Collect the result from a collection of futures, that are lazily created. Futures will be
     * created using the given {@code callables}, but will only create as many pending futures to be
     * less than or equal to the given {@code parallelism} setting.
     * <p>
     * If a single future is cancelled, or failed, all the other will be as well.
     * <p>
     * This method is intended to be used for rate-limiting requests that could potentially be
     * difficult to stop cleanly.
     *
     * @param callables The list of constructor methods.
     * @param collector The collector to reduce the result.
     * @param parallelism The number of futures that are allowed to be constructed at the same
     * time.
     * @param <S> source type of the collected futures.
     * @param <T> target type the collected futures are being transformed into.
     * @return A future that will be resolved when all of the collected futures are resolved.
     */
    <S, T> AsyncFuture<T> eventuallyCollect(
        Collection<? extends Callable<? extends AsyncFuture<? extends S>>> callables,
        StreamCollector<? super S, ? extends T> collector, int parallelism
    );

    /**
     * Call the given callable on the default executor and track the result using a future.
     *
     * @param callable Callable to call.
     * @param <T> type of the future.
     * @return A future tracking the result of the callable.
     * @throws IllegalStateException if no default executor service is configured.
     * @see #call(Callable, ExecutorService)
     */
    <T> AsyncFuture<T> call(Callable<? extends T> callable);

    /**
     * Call the given callable on the default executor and track the lazy result using a future,
     * this expects the callable to return an {@code AsyncFuture}.
     *
     * @param callable Callable to call.
     * @param <T> type of the future.
     * @return A future tracking the result of the lazy callable.
     * @throws IllegalStateException if no default executor service is configured.
     * @see #lazyCall(Callable, ExecutorService)
     */
    <T> AsyncFuture<T> lazyCall(Callable<? extends AsyncFuture<T>> callable);

    /**
     * Call the given callable on the provided executor and track the result using a future.
     *
     * @param callable Callable to invoke.
     * @param executor Executor service to invoke on.
     * @param <T> type of the future.
     * @return A future tracking the result of the callable.
     * @see #call(Callable, ExecutorService, ResolvableFuture)
     */
    <T> AsyncFuture<T> call(Callable<? extends T> callable, ExecutorService executor);

    /**
     * Call the given callable on the provided executor and track the lazy result using a future,
     * this expects the callable to return an {@code AsyncFuture}.
     *
     * @param callable Callable to invoke.
     * @param executor Executor service to invoke on.
     * @param <T> type of the future.
     * @return A future tracking the result of the callable.
     * @see #call(Callable, ExecutorService, ResolvableFuture)
     */
    <T> AsyncFuture<T> lazyCall(
        Callable<? extends AsyncFuture<T>> callable, ExecutorService executor
    );

    /**
     * Call the given callable and resolve the given future with its result.
     * <p>
     * This operation happens on the provided executor.
     *
     * @param callable The resolver to use.
     * @param executor The executor to schedule the resolver on.
     * @param future The future to resolve.
     * @param <T> type of the future.
     * @return The future that will be resolved.
     */
    <T> AsyncFuture<T> call(
        Callable<? extends T> callable, ExecutorService executor, ResolvableFuture<T> future
    );

    /**
     * Setup a managed reference.
     *
     * @param setup The setup method for the managed reference.
     * @param <T> type of the managed reference.
     * @return The managed reference.
     */
    <T> Managed<T> managed(ManagedSetup<T> setup);

    /**
     * Setup a reloadable, managed reference.
     *
     * @param setup The setup method for the managed reference.
     * @param <T> type of the managed reference.
     * @return The managed reference.
     */
    <T> ReloadableManaged<T> reloadableManaged(ManagedSetup<T> setup);

    <T> AsyncFuture<RetryResult<T>> retryUntilResolved(
        Callable<? extends AsyncFuture<? extends T>> action, RetryPolicy policy
    );

    /**
     * Retry the given operation until it has been resolved, or the provided {@link
     * eu.toolchain.async.RetryPolicy} expire.
     *
     * @param action The action to run.
     * @param policy The retry policy to use.
     * @param clockSource Clock source to use.
     * @return A future that will be resolved, when the called future is resolved.
     */
    <T> AsyncFuture<RetryResult<T>> retryUntilResolved(
        Callable<? extends AsyncFuture<? extends T>> action, RetryPolicy policy,
        ClockSource clockSource
    );
}
