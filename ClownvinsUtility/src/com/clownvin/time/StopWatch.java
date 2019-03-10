package com.clownvin.time;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

/**
 * A {@code StopWatch} implementation that supports pausing. Useful for
 * monitoring the run time of certain blocks of code. Also supplies a global
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
    * Globally instanced {@code StopWatch}, meant to be statically imported in
    * certain use-cases.
    *
    * @since 1.0
    */
   public static final StopWatch GLOBAL_STOPWATCH = new StopWatch();

   /**
    * The value of {@code System.nanoTime()} from when {@link #start} was last
    * called, plus any pauses.
    *
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
    * The value of {@code System.nanoTime()} from when {@link #pause} was last
    * called.
    *
    * @see #pause()
    * @see #unpause()
    * @since 1.0
    */
   protected volatile long pauseTime;
   /**
    * How much time should pass, not including pauses, before the {@link #alarm}
    * notifies all waiting on this {@code StopWatch} and the {@code StopWatch}
    * should be considered expired.
    *
    * @see #alarm
    * @see #expired()
    * @see #timeLeft()
    * @since 1.0
    */
   protected volatile long timerLength = 0;
   
   /**
    * Whether or not this StopWatch is stopped.
    *
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
    *
    * @see #isPaused()
    * @see #pause()
    * @see #unpause()
    * @since 1.0
    */
   protected volatile boolean paused  = false;

   /**
    * Exists to call {@link #notifyAll()} on this {@code StopWatch} whenever this
    * {@code StopWatch} becomes expired.
    *
    * @see #expired()
    * @see #notifyAll()
    * @since 1.0
    */
   protected Thread alarm;

   /**
    * The actual logic block for {@link #alarm}.
    *
    * @see #alarm
    * @since 1.0
    */
   protected final Runnable alarmRunnable = () -> {
      while (true) { //TODO consider adding ternary condition
         synchronized (alarm) {
            while (stopped || expired()) {
               try {
                  alarm.wait(Math.max(timerLength - this.timeElapsed(), 0));
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
    * Creates a new {@code StopWatch} instance, creating a new {@link #alarm} at
    * the same time.
    *
    * @see #alarm
    * @since 1.0
    */
   public StopWatch() {
      alarm = new Thread(alarmRunnable);
      alarm.setName("StopWatch@" + hashCode() + ".alarm");
      alarm.start();
   }
   
   /**
    * Checks if this {@code StopWatch} is paused. If so, it throws an
    * {@link IllegalStateException}.
    *
    * @see #isPaused()
    * @since 1.0
    */
   protected void checkPaused() {
      if (isPaused()) {
         throw new IllegalStateException("StopWatch is paused.");
      }
   }
   
   /**
    * Checks if this {@code StopWatch} is started. If so, throws an
    * {@link IllegalStateException}.
    *
    * @see #isStopped()
    * @since 1.0
    */
   protected void checkStarted() {
      if (!isStopped()) {
         throw new IllegalStateException("StopWatch is already started.");
      }
   }
   
   /**
    * Checks if this {@code StopWatch} is stopped. If so, throws an
    * {@link IllegalStateException}.
    *
    * @see #isStopped()
    * @since 1.0
    */
   protected void checkStopped() {
      if (isStopped()) {
         throw new IllegalStateException("StopWatch is not started.");
      }
   }
   
   /**
    * Checks if this {@code StopWatch} is unpaused. If so, throws an
    * {@link IllegalStateException}.
    *
    * @see #isPaused()
    * @since 1.0
    */
   protected void checkUnpaused() {
      if (!paused) {
         throw new RuntimeException("StopWatch is not paused!");
      }
   }
   
   /**
    * Returns {@code true} this {@code StopWatch} is expired by checking
    * {@link #timeLeft()}.
    *
    * @return true if {@code timeLeft() < 0}, false otherwise
    * @see #timeLeft()
    * @since 1.0
    */
   public boolean expired() {
      return timeLeft() < 0;
   }
   
   /**
    * Getter for {@link #paused}.
    *
    * @return paused
    * @see #paused
    * @since 1.0
    */
   public boolean isPaused() {
      return paused;
   }
   
   /**
    * Getter for {@link #stopped}.
    *
    * @return stopped
    * @see #stopped
    * @since 1.0
    */
   public boolean isStopped() {
      return stopped;
   }
   
   /**
    * Pauses this {@code StopWatch}. Useful for executing code outside the scope of
    * what is intended to be monitored.
    *
    * @see #unpause()
    * @since 1.0
    */
   public synchronized void pause() {
      pauseTime = System.nanoTime();
      checkPaused();
      paused = true;
   }
   
   /**
    * Resets this {@code StopWatch} to an effectively default state. Will need to
    * be restarted with {@link #start start}, {@link #restart restart} or
    * {@link #restartPaused restartPaused}.
    *
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch reset() {
      stopped = true;
      paused = false;
      return this;
   }
   
   /**
    * Restarts this {@code StopWatch} with a timer length of 0.
    *
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public StopWatch restart() {
      return restart(0);
   }
   
   /**
    * Restarts this {@code StopWatch} with the specified timer length, ignoring
    * whether or not this {@code StopWatch} was started already.
    *
    * @param timerLength the new timer length to start with
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch restart(final long timerLength) {
      reset().start(timerLength);
      return this;
   }
   
   /**
    * Restarts this {@code StopWatch} in a paused state with a timer length of 0.
    * <p>
    * Starting paused is useful for calling {@link #runForDuration(Runnable)},
    * since it will allow the code executing to chose when to start and stop this
    * {@code StopWatch} without having to immediately pause it itself to achieve
    * the same behavior.
    *
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public StopWatch restartPaused() {
      return restartPaused(0);
   }

   /**
    * Restarts this {@code StopWatch} in a paused state with the specified timer
    * length.
    * <p>
    * Starting paused is useful for calling {@link #runForDuration(Runnable)},
    * since it will allow the code executing to chose when to start and stop this
    * {@code StopWatch} without having to immediately pause it itself to achieve
    * the same behavior.
    *
    * @param timerLength the new timer length to start with
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch restartPaused(final long timerLength) {
      reset().pause();
      start(timerLength);
      return this;
   }

   /**
    * Will continuously execute the {@link Runnable} specified until this
    * {@code StopWatch}es timer is expired.
    * <p>
    * This is useful for executing code in succession, but only for a while. When
    * used in conjunction with pausing, you can also guarantee specific that code
    * executes for approximately the length of the timer
    * <p>
    * If you need very precise control over what segments of code affect this
    * {@code StopWatch}, you can call this method while in a paused state. Doing
    * this means that you will need to manually unpause this {@code StopWatch}
    * inside the specified {@code Runnable}.
    *
    * @param code the code to be executed for the remaining duration of this
    *             {@code StopWatch}.
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch runForDuration(final Runnable code) {
      while (!expired()) {
         code.run();
      }
      return this;
   }

   /**
    * Sets the timer length of this {@code StopWatch} and notifies the alarm to
    * start ticking.
    * 
    * @param timerLength new timer length
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch setTimer(final long timerLength) {
      if (timerLength < 0) {
         throw new IllegalArgumentException("StopWatch timer length cannot be less than 0.");
      }
      this.timerLength = timerLength;
      synchronized (alarm) {
         alarm.notifyAll();
      }
      return this;
   }

   /**
    * Starts this {@code StopWatch}. Effectively calls {@link #start(long)
    * start(0)}.
    * 
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public StopWatch start() {
      return start(0);
   }
   
   /**
    * Starts this {@code StopWatch} with the specified timer length.
    * <p>
    * Will throw an {@code IllegalStateException} if this {@code StopWatch} is
    * already started.
    *
    * @param timerLength new timer length
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch start(final long timerLength) {
      checkStarted();
      stopped = false;
      setTimer(timerLength);
      startTime = System.nanoTime();
      return this;
   }
   
   /**
    * Stops this {@code StopWatch}, and returns the time elapsed.
    * <p>
    * Will unpause if paused, and will throw an {@code IllegalStateException} if
    * this {@code StopWatch} is not started.
    *
    * @return the time elapsed from start
    * @see #timeElapsed()
    * @see #timeElapsed(TimeUnit)
    * @since 1.0
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
    * Simply calls {@link #timeElapsed(TimeUnit)
    * timeElapsed(TimeUnit.MILLISECONDS)}.
    *
    * @return time since the start, in milliseconds
    */
   public long timeElapsed() {
      return timeElapsed(MILLISECONDS);
   }

   /**
    * Returns the time since the start, after converting it from nanoseconds to the
    * specified {@link TimeUnit}.
    *
    * @param unit {@code TimeUnit} to convert the time to.
    * @return time since the start, in the specified {@code TimeUnit}
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
      return timerLength - timeElapsed();
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
