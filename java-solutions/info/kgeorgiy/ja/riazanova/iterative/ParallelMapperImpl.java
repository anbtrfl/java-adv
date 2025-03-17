package info.kgeorgiy.ja.riazanova.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;


public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threads;
    private final Deque<Task<?>> tasks;


    /**
     * Constructor for {@link ParallelMapperImpl},
     *
     * @param threadsNumber is a number of threads to create and work on.
     */
    public ParallelMapperImpl(int threadsNumber) {
        this.threads = new ArrayList<>();
        this.tasks = new ArrayDeque<>();

        Runnable runnable = () -> {
            try {
                Task<?> task;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (tasks) {
                        while (tasks.isEmpty()) {
                            tasks.wait();
                        }
                        task = tasks.poll();
                    }
                    // :NOTE: Задача завершилась exception'ом
                    task.run();
                }
            } catch (InterruptedException ignored) {
            }
        };

        for (int i = 0; i < threadsNumber; i++) {
            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performed in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(
            Function<? super T, ? extends R> f,
            List<? extends T> args
    ) throws InterruptedException {
        final List<Task<R>> localTasks = new ArrayList<>(args.size());
        final Counter counter = new Counter();

        for (int i = 0; i < args.size(); i++) {
            final int index = i;

            Runnable runnable = () -> {
                R result = f.apply(args.get(index));

                synchronized (localTasks) {
                    localTasks.get(index).setSuccessResult(result);
                }

                synchronized (counter) {
                    counter.increment();

                    if (counter.getValue() == args.size()) {
                        counter.notify();
                    }
                }
            };

            Task<R> task = new Task<>(runnable);
            localTasks.add(task);

            synchronized (tasks) {
                tasks.add(task);
                tasks.notify();
            }
        }

        synchronized (counter) {
            while (counter.getValue() < args.size()) {
                counter.wait();
            }
        }

        return localTasks.stream().map(Task::getResult).toList();
    }

    /**
     * Stops all threads. All unfinished mappings are left in undefined state.
     */
    @Override
    public void close() {
        for (Thread thread : threads) {
            thread.interrupt();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static class Counter {
        private int value = 0;

        int getValue() {
            return value;
        }

        void increment() {
            value++;
        }
    }
}

final class Task<R> {
    private final Runnable task;
    private Result<R> result;

    public Task(Runnable task) {
        this.task = task;
    }

    public void setException(RuntimeException exception) {
        this.result = new Error<>(exception);
    }

    public void setSuccessResult(R result) {
        this.result = new Success<>(result);
    }

    public void run() {
        try {
            task.run();
        } catch (RuntimeException e) {
            setException(e);
        }
    }

    public R getResult() {
        return result.get();
    }
}

interface Result<R> {
    R get();
}

final class Success<R> implements Result<R> {
    private final R result;

    public Success(R result) {
        this.result = result;
    }

    @Override
    public R get() {
        return result;
    }
}

final class Error<R> implements Result<R> {
    private final RuntimeException exception;

    public Error(RuntimeException exception) {
        this.exception = exception;
    }

    @Override
    public R get() {
        throw exception;
    }
}
