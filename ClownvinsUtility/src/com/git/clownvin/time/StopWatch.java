package com.git.clownvin.time;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

/**
 * A {@code StopWatch} implementation that supports pausing. Useful for
 * monitoring the runtime of certain blocks of code. Also supplies a global
 * {@code StopWatch} instance, for use in code that does not require multiple
 * {@code StopWatche}s.
 * <p>
 * To use a {@code StopWatch}, you first need to call {@link #start},
 * {@link #restart} or {@link #restartPaused}, which will start the
 * {@code StopWatch}. Once the {@code StopWatch} is started you can call
 * {@link #pause} to pause the {@code StopWatch}, or {@link #unpause} to unpause
 * it. Use pausing to stop the {@code StopWatch} temporarily if you need to
 * execute code unrelated to what {@code StopWatch} is to be monitoring.
 * 
 * @author Clownvin
 * @version 1.0
 *
 */
public class StopWatch {
   
   /**
    * Globally instanced {@code StopWatch}, meant to be statically imported in certain use-cases.
    * @since 1.0
    */
   public static final StopWatch GLOBAL_STOPWATCH = new StopWatch();

   /**
    * The value of {@code System.nanoTime()} from when {@link #start} was
    * last called, plus any pauses.
    * @see #start()
    * @see #start(long)
    * @see #restart()
    * @see #restart(long)
    * @see #restartPaused()
    * @see #restartPaused(long)
    * @since 1.0
    */
   protected volatile long startTime;
   /**
    * The value of {@code System.nanoTime()} from when {@link #pause} was
    * last called.
    * @see #pause()
    * @see #unpause() 
    * @since 1.0
    */
   protected volatile long pauseTime;
   /**
    * How much time should pass, not including pauses, before the {@link #alarm} notifies all waiting on this {@code StopWatch}
    * and the {@code StopWatch} should be considered expired.
    * @see #alarm
    * @see #expired()
    * @see #timeLeft()
    * @since 1.0
    */
   protected volatile long duration = 0;
   
   /**
    * Whether or not this StopWatch is stopped.
    * @see #start()
    * @see #start(long)
    * @see #restart()
    * @see #restart(long)
    * @see #restartPaused()
    * @see #restartPaused(long)
    * @since 1.0
    */
   protected volatile boolean stopped = true;
   /**
    * Whether or not this StopWatch is paused.
    * @see #isPaused()
    * @see #pause()
    * @see #unpause()
    * @since 1.0
    */
   protected volatile boolean paused  = false;

   /**
    * Exists to call {@link #notifyAll()} on this {@code StopWatch} whenever this {@code StopWatch} becomes expired.
    * @see #expired()
    * @see #notifyAll()
    * @since 1.0
    */
   protected Thread alarm;

   /**
    * The actual logic block for {@link #alarm}.
    * @see #alarm
    * @since 1.0
    */
   protected final Runnable alarmRunnable = () -> {
      while (true) { //TODO consider adding ternary condition
         synchronized (alarm) {
            while (stopped || expired()) {
               try {
                  alarm.wait(duration - this.timeElapsed());
               } catch (final InterruptedException e2) {
                  throw new RuntimeException(e2);
               }
            }
         }
         while (!expired()) {
            try {
               Thread.sleep(1);
            } catch (final InterruptedException e1) {
               throw new RuntimeException(e1);
            }
         }
         synchronized (StopWatch.this) {
            StopWatch.this.notifyAll();
         }
      }
   };

   /**
    * Creates a new {@code StopWatch} instance, creating a new {@link #alarm} at the same time.
    * @see #alarm
    * @since 1.0
    */
   public StopWatch() {
      alarm = new Thread(alarmRunnable);
      alarm.setName("StopWatch@" + hashCode() + ".alarm");
      alarm.start();
   }
   
   /**
    * Checks if this {@code StopWatch} is paused. If so, it throws an {@link IllegalStateException}.
    * @see #isPaused()
    * @since 1.0
    */
   protected void checkPaused() {
      if (isPaused()) {
         throw new IllegalStateException("StopWatch is paused.");
      }
   }
   
   /**
    * Getter for {@link #paused}.
    * @return paused
    * @see #paused
    * @since 1.0
    */
   public boolean isPaused() {
      return paused;
   }
   
   /**
    * Getter for {@link #stopped}.
    * @return stopped
    * @see #stopped
    * @since 1.0
    */
   public boolean isStopped() {
      return stopped;
   }
   
   /**
    * Checks if this {@code StopWatch} is stopped. If so, throws an {@link IllegalStateException}.
    * @see #isStopped()
    * @since 1.0
    */
   protected void checkStopped() {
      if (isStopped()) {
         throw new IllegalStateException("StopWatch is not started.");
      }
   }
   
   /**
    * Checks if this {@code StopWatch} is started. If so, throws an {@link IllegalStateException}.
    * @see #isStopped()
    * @since 1.0
    */
   protected void checkStarted() {
      if (!isStopped()) {
         throw new IllegalStateException("StopWatch is already started.");
      }
   }
   
   /**
    * Checks if this {@code StopWatch} is unpaused. If so, throws an {@link IllegalStateException}.
    * @see #isPaused()
    * @since 1.0
    */
   protected void checkUnpaused() {
      if (!paused) {
         throw new RuntimeException("StopWatch is not paused!");
      }
   }
   
   /**
    * Returns {@code true} this {@code StopWatch} is expired.
    * @return true if {@code timeLeft() < 0}, false otherwise
    * @see timeLeft()
    * @since 1.0
    */
   public boolean expired() {
      return timeLeft() < 0;
   }
   
   /**
    * 
    */
   public synchronized void pause() {
      pauseTime = System.nanoTime();
      checkPaused();
      paused = true;
   }
   
   /**
    *
    * @return
    */
   public synchronized StopWatch reset() {
      stopped = true;
      paused = false;
      return this;
   }
   
   /**
    *
    * @return
    */
   public StopWatch restart() {
      return restart(0);
   }
   
   /**
    *
    * @param duration
    * @return
    */
   public synchronized StopWatch restart(final long duration) {
      reset().start(duration);
      return this;
   }
   
   /**
    *
    * @return
    */
   public StopWatch restartPaused() {
      return restartPaused(0);
   }

   /**
    *
    * @param duration
    * @return
    */
   public synchronized StopWatch restartPaused(final long duration) {
      reset().pause();
      start(duration);
      return this;
   }

   /**
    *
    * @param r
    * @return
    */
   public synchronized StopWatch runForDuration(final Runnable r) {
      while (!expired()) {
         r.run();
      }
      return this;
   }

   /**
    *
    * @param duration
    * @return
    */
   public synchronized StopWatch setTimer(final long duration) {
      if (duration < 0) {
         throw new IllegalArgumentException("StopWatch timer duration cannot be less than 0.");
      }
      this.duration = duration;
      synchronized (alarm) {
         alarm.notifyAll();
      }
      return this;
   }

   /**
    *
    * @return
    */
   public StopWatch start() {
      return start(0);
   }
   
   /**
    *
    * @param duration
    * @return
    */
   public synchronized StopWatch start(final long duration) {
      if (!stopped) {
         throw new RuntimeException("StopWatch is already started!");
      }
      stopped = false;
      setTimer(duration);
      startTime = System.nanoTime();
      return this;
   }
   
   /**
    *
    * @return
    */
   public synchronized long stop() {
      checkStopped();
      if (paused) {
         unpause();
      }
      stopped = false;
      return timeElapsed();
   }
   
   /**
    *
    * @return
    */
   public long timeElapsed() {
      return timeElapsed(MILLISECONDS);
   }

   /**
    *
    * @param unit
    * @return
    */
   public synchronized long timeElapsed(final TimeUnit unit) {
      checkStopped();
      return unit.convert(System.nanoTime() - (paused ? startTime + (System.nanoTime() - pauseTime) : startTime), NANOSECONDS);
   }
   
   /**
    *
    * @return
    */
   public synchronized long timeLeft() {
      return duration - timeElapsed();
   }
   
   /**
    *
    */
   public synchronized void unpause() {
      checkUnpaused();
      paused = false;
      startTime += System.nanoTime() - pauseTime;
   }
}
