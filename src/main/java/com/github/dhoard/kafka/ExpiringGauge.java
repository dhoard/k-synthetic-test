package com.github.dhoard.kafka;

import io.prometheus.client.Collector;
import io.prometheus.client.DoubleAdder;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.Summary;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class ExpiringGauge extends SimpleCollector<ExpiringGauge.Child> implements Collector.Describable {

    private final long ttl;
    private static final AtomicLong noLabelsChildLastUpdated = new AtomicLong(System.currentTimeMillis());

    ExpiringGauge(Builder b) {
        super(b);
        this.ttl = b.ttl;
    }

    public static class Builder extends SimpleCollector.Builder<Builder, ExpiringGauge> {

        private long ttl = Long.MAX_VALUE;

        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        @Override
        public ExpiringGauge create() {
            return new ExpiringGauge(this);
        }
    }

    /**
     *  Return a Builder to allow configuration of a new Gauge. Ensures required fields are provided.
     *
     *  @param name The name of the metric
     *  @param help The help string of the metric
     */
    public static Builder build(String name, String help) {
        return new Builder().name(name).help(help);
    }

    /**
     *  Return a Builder to allow configuration of a new Gauge.
     */
    public static Builder build() {
        return new Builder();
    }

    @Override
    protected Child newChild() {
        return new Child(ttl);
    }

    /**
     * Represents an event being timed.
     */
    public static class Timer implements Closeable {
        private final Child child;
        private final long start;
        private Timer(Child child) {
            this.child = child;
            start = Child.timeProvider.nanoTime();
        }
        /**
         * Set the amount of time in seconds since {@link Child#startTimer} was called.
         * @return Measured duration in seconds since {@link Child#startTimer} was called.
         */
        public double setDuration() {
            double elapsed = (Child.timeProvider.nanoTime() - start) / NANOSECONDS_PER_SECOND;
            child.set(elapsed);
            return elapsed;
        }

        /**
         * Equivalent to calling {@link #setDuration()}.
         */
        @Override
        public void close() {
            setDuration();
        }
    }

    /**
     * The value of a single Gauge.
     * <p>
     * <em>Warning:</em> References to a Child become invalid after using
     * {@link SimpleCollector#remove} or {@link SimpleCollector#clear},
     */
    public static class Child {

        private final DoubleAdder value = new DoubleAdder();
        private final long ttl;
        private final AtomicLong lastUpdated = new AtomicLong(System.currentTimeMillis());

        static TimeProvider timeProvider = new TimeProvider();

        public Child(long ttl) {
            this.ttl = ttl;
        }

        /**
         * Increment the gauge by 1.
         */
        public void inc() {
            inc(1);
        }
        /**
         * Increment the gauge by the given amount.
         */
        public void inc(double amt) {
            value.add(amt);
            lastUpdated.set(System.currentTimeMillis());
        }
        /**
         * Decrement the gauge by 1.
         */
        public void dec() {
            dec(1);
        }
        /**
         * Decrement the gauge by the given amount.
         */
        public void dec(double amt) {
            value.add(-amt);
            lastUpdated.set(System.currentTimeMillis());
        }
        /**
         * Set the gauge to the given value.
         */
        public void set(double val) {
            value.set(val);
            lastUpdated.set(System.currentTimeMillis());
        }
        /**
         * Set the gauge to the current unixtime.
         */
        public void setToCurrentTime() {
            set(timeProvider.currentTimeMillis() / MILLISECONDS_PER_SECOND);
        }
        /**
         * Start a timer to track a duration.
         * <p>
         * Call {@link Timer#setDuration} at the end of what you want to measure the duration of.
         * <p>
         * This is primarily useful for tracking the durations of major steps of batch jobs,
         * which are then pushed to a PushGateway.
         * For tracking other durations/latencies you should usually use a {@link Summary}.
         */
        public Timer startTimer() {
            return new Timer(this);
        }

        /**
         * Executes runnable code (e.g. a Java 8 Lambda) and observes a duration of how long it took to run.
         *
         * @param timeable Code that is being timed
         * @return Measured duration in seconds for timeable to complete.
         */
        public double setToTime(Runnable timeable){
            Timer timer = startTimer();

            double elapsed;
            try {
                timeable.run();
            } finally {
                elapsed = timer.setDuration();
            }

            return elapsed;
        }

        /**
         * Executes callable code (e.g. a Java 8 Lambda) and observes a duration of how long it took to run.
         *
         * @param timeable Code that is being timed
         * @return Result returned by callable.
         */
        public <E> E setToTime(Callable<E> timeable){
            Timer timer = startTimer();

            try {
                return timeable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                timer.setDuration();
            }
        }

        /**
         * Get the value of the gauge.
         */
        public double get() {
            long now = System.currentTimeMillis();
            if (now >= (lastUpdated.get() + ttl)) {
                value.set(-1);
                lastUpdated.set(now);
            }
            return value.sum();
        }
    }

    // Convenience methods.
    /**
     * Increment the gauge with no labels by 1.
     */
    public void inc() {
        inc(1);
    }
    /**
     * Increment the gauge with no labels by the given amount.
     */
    public void inc(double amt) {
        noLabelsChild.inc(amt);
        noLabelsChildLastUpdated.set(System.currentTimeMillis());
    }
    /**
     * Decrement the gauge with no labels by 1.
     */
    public void dec() {
        dec(1);
    }
    /**
     * Decrement the gauge with no labels by the given amount.
     */
    public void dec(double amt) {
        noLabelsChild.dec(amt);
        noLabelsChildLastUpdated.set(System.currentTimeMillis());
    }
    /**
     * Set the gauge with no labels to the given value.
     */
    public void set(double val) {
        noLabelsChild.set(val);
        noLabelsChildLastUpdated.set(System.currentTimeMillis());
    }
    /**
     * Set the gauge with no labels to the current unixtime.
     */
    public void setToCurrentTime() {
        noLabelsChild.setToCurrentTime();
    }
    /**
     * Start a timer to track a duration, for the gauge with no labels.
     * <p>
     * This is primarily useful for tracking the durations of major steps of batch jobs,
     * which are then pushed to a PushGateway.
     * For tracking other durations/latencies you should usually use a {@link Summary}.
     * <p>
     * Call {@link Timer#setDuration} at the end of what you want to measure the duration of.
     */
    public Timer startTimer() {
        return noLabelsChild.startTimer();
    }

    /**
     * Executes runnable code (e.g. a Java 8 Lambda) and observes a duration of how long it took to run.
     *
     * @param timeable Code that is being timed
     * @return Measured duration in seconds for timeable to complete.
     */
    public double setToTime(Runnable timeable){
        return noLabelsChild.setToTime(timeable);
    }

    /**
     * Executes callable code (e.g. a Java 8 Lambda) and observes a duration of how long it took to run.
     *
     * @param timeable Code that is being timed
     * @return Result returned by callable.
     */
    public <E> E setToTime(Callable<E> timeable){
        return noLabelsChild.setToTime(timeable);
    }

    /**
     * Get the value of the gauge.
     */
    public double get() {
        long now = System.currentTimeMillis();
        if (now >= (noLabelsChildLastUpdated.get() + ttl)) {
            noLabelsChild.set(-1);
            noLabelsChildLastUpdated.set(now);
        }
        return noLabelsChild.get();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>(children.size());
        for(Map.Entry<List<String>, Child> c: children.entrySet()) {
            samples.add(new MetricFamilySamples.Sample(fullname, labelNames, c.getKey(), c.getValue().get()));
        }
        return familySamplesList(Type.GAUGE, samples);
    }

    @Override
    public List<MetricFamilySamples> describe() {
        return Collections.<MetricFamilySamples>singletonList(new GaugeMetricFamily(fullname, help, labelNames));
    }

    static class TimeProvider {
        long currentTimeMillis() {
            return System.currentTimeMillis();
        }
        long nanoTime() {
            return System.nanoTime();
        }
    }
}