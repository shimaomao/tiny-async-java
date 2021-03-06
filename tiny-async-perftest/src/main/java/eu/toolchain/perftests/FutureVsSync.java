package eu.toolchain.perftests;

import com.google.common.base.Stopwatch;
import eu.toolchain.async.AsyncFramework;
import eu.toolchain.async.AsyncFuture;
import eu.toolchain.async.Collector;
import eu.toolchain.async.TinyAsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureVsSync {
    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService asyncThreads =
        Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);
    private static final ExecutorService syncThreads =
        Executors.newFixedThreadPool(AVAILABLE_PROCESSORS);

    private static final AsyncFramework async = TinyAsync.builder().executor(asyncThreads).build();

    public static void main(String argv[]) throws Exception {
        final long syncTime;
        final double sync;
        final long asyncTime;
        final double async;

        {
            Stopwatch sw = Stopwatch.createStarted();
            sync = sync();
            syncTime = sw.elapsed(TimeUnit.NANOSECONDS);
        }

        {
            Stopwatch sw = Stopwatch.createStarted();
            async = async();
            asyncTime = sw.elapsed(TimeUnit.NANOSECONDS);
        }

        System.out.println(String.format("sync: %f (%d ns)", sync, syncTime));
        System.out.println(String.format("async: %f (%d ns)", async, asyncTime));
        System.exit(0);
    }

    private static Collector<Double, Double> summer = new Collector<Double, Double>() {
        @Override
        public Double collect(Collection<Double> results) throws Exception {
            double sum = 0.0d;

            for (final Double r : results) {
                sum += r;
            }

            return sum;
        }
    };

    private static double async() throws Exception {
        final List<AsyncFuture<Double>> outer = new ArrayList<>();

        for (int i = 0; i < AVAILABLE_PROCESSORS - 1; i++) {
            outer.add(someAsyncCall());
        }

        return async.collect(outer, summer).get();
    }

    private static AsyncFuture<Double> someAsyncCall() {
        return async.lazyCall(new Callable<AsyncFuture<Double>>() {
            @Override
            public AsyncFuture<Double> call() throws Exception {
                final List<AsyncFuture<Double>> inner = new ArrayList<>();

                for (int i = 0; i < 100; i++) {
                    inner.add(async.call(new Callable<Double>() {
                        @Override
                        public Double call() throws Exception {
                            return doSomeWork();
                        }
                    }));
                }

                return async.collect(inner, summer);
            }
        });
    }

    private static double sync() throws Exception {
        final List<Future<Double>> outer = new ArrayList<>();

        for (int i = 0; i < AVAILABLE_PROCESSORS - 1; i++) {
            outer.add(someSyncCall());
        }

        double sum = 0.0d;

        for (final Future<Double> f : outer) {
            sum += f.get();
        }

        return sum;
    }

    private static Future<Double> someSyncCall() {
        return syncThreads.submit(new Callable<Double>() {
            @Override
            public Double call() throws Exception {
                final List<Future<Double>> inner = new ArrayList<>();

                for (int i = 0; i < 100; i++) {
                    inner.add(syncThreads.submit(new Callable<Double>() {
                        @Override
                        public Double call() throws Exception {
                            return doSomeWork();
                        }
                    }));
                }

                double sum = 0.0d;

                for (final Future<Double> f : inner) {
                    sum += f.get();
                }

                return sum;
            }
        });
    }

    public static double doSomeWork() {
        double sum = 0.0d;

        for (int i = 0; i < 1000000; i++) {
            sum += Math.sqrt(Math.pow(i, 2));
        }

        return sum;
    }
}
