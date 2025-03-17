package info.kgeorgiy.ja.riazanova.iterative;

import info.kgeorgiy.java.advanced.iterative.NewScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class IterativeParallelism implements NewScalarIP {

    private final ParallelMapper mapper;

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    public IterativeParallelism() {
        this.mapper = null;
    }

    /**
     * Returns maximum value.
     *
     * @param threads    number of concurrent threads.
     * @param values     values to get maximum of.
     * @param comparator value comparator.
     * @return maximum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if no values are given.
     */
    @Override
    public <T> T maximum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator,
                         int step) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("No values were provided");
        }

        return process(
                threads,
                values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow(),
                step
        );
    }

    /**
     * Returns minimum value.
     *
     * @param threads    number of concurrent threads.
     * @param values     values to get minimum of.
     * @param comparator value comparator.
     * @param <T>        value type.
     * @return minimum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(int threads,
                         List<? extends T> values,
                         Comparator<? super T> comparator,
                         int step) throws InterruptedException {
        return maximum(threads, values, comparator.reversed(), step);
    }

    /**
     * Returns whether all values satisfy predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether all values satisfy predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate,
                           int step) throws InterruptedException {
        return process(
                threads,
                values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(p -> p),
                step
        );

    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threads,
                           List<? extends T> values,
                           Predicate<? super T> predicate,
                           int step) throws InterruptedException {
        return !all(threads, values, predicate.negate(), step);
    }

    /**
     * Returns number of values satisfying predicate.
     *
     * @param threads   number of concurrent threads.
     * @param values    values to test.
     * @param predicate test predicate.
     * @param <T>       value type.
     * @return number of values satisfying predicate.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> int count(int threads,
                         List<? extends T> values,
                         Predicate<? super T> predicate,
                         int step) throws InterruptedException {
        return process(
                threads,
                values,
                stream -> (int) stream.filter(predicate).count(),
                stream -> stream.reduce(0, Integer::sum),
                step);
    }

    private <T> List<Stream<? extends T>> getSubStreams(int threads, List<? extends T> values, int step) {
        List<? extends T> nthValues = IntStream.iterate(0, i -> i < values.size(), i -> i + step)
                .mapToObj(values::get)
                .toList();

        int size = nthValues.size();

        threads = Math.min(size, threads);
        int c = size / threads;

        List<Stream<? extends T>> results = new ArrayList<>(threads);

        int from = 0;
        int to = c;
        int ost = size % threads;

        while (to <= size) {
            if (ost > 0) {
                to++;
                ost--;
            }

            List<? extends T> subValues = nthValues.subList(from, to);
            results.add(subValues.stream());

            from = to;
            to += c;
        }

        return results;
    }

    private <T, R> R process(
            int threads,
            List<? extends T> values,
            Function<Stream<? extends T>, R> firstProcessing,
            Function<Stream<R>, R> finalProcessing,
            int step
    ) throws InterruptedException {
        List<Stream<? extends T>> subStreams = getSubStreams(threads, values, step);
        threads = subStreams.size();

        List<R> results;

        if (mapper != null) {
            results = mapper.map(firstProcessing, subStreams);
        } else {
            Thread[] threadsList = new Thread[threads];
            results = new ArrayList<>(threads);

            for (int i = 0; i < subStreams.size(); i++) {
                int finalI = i;

                results.add(null);

                Thread t = new Thread(() -> {
                    Stream<? extends T> substream = subStreams.get(finalI);
                    R result = firstProcessing.apply(substream);
                    results.set(finalI, result);
                });

                threadsList[finalI] = t;

                t.start();
            }

            joinThreads(threadsList);
        }

        return finalProcessing.apply(results.stream());
    }

    private static void joinThreads(Thread[] threads) throws InterruptedException {
        InterruptedException exception = null;

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                if (exception == null) {
                    exception = new InterruptedException("At least one of the threads was interrupted");
                }
                exception.addSuppressed(e);
            }
        }

        if (exception != null) {
            throw exception;
        }
    }
}


