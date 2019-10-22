package com.clownvin.time;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

/**
 * A {@code StopWatch} implementation that supports pausing. Useful for
 * monitoring the run time of certain blocks of code.
 * <p>
 * To use a {@code StopWatch}, you first need to call {@link #start},
 * {@link #restart} or {@link #restartPaused}, which will start the
 * {@code StopWatch}. Once the {@code StopWatch} is started you can call
 * {@link #pause} to pause the {@code StopWatch}, or {@link #unpause} to unpause
 * it. Use pausing to stop the {@code StopWatch} temporarily if you need to
 * execute code unrelated to what {@code StopWatch} is to be monitoring.
 *
 * @author Clownvin
 * @version 1.1
 *
 */
public class StopWatch {

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
    * Whether or not this StopWatch is paused.
    *
    * @see #isPaused()
    * @see #pause()
    * @see #unpause()
    * @since 1.0
    */
   protected volatile boolean paused = false;

   public StopWatch() {
      this(false, 0);
   }

   public StopWatch(final boolean paused) {
      this(paused, 0);
   }

   public StopWatch(final boolean paused, final long timerLength) {
      if (paused) {
         startPaused(timerLength);
      } else {
         start(timerLength);
      }
   }

   public StopWatch(final long timerLength) {
      this(false, timerLength);
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
    * Pauses this {@code StopWatch}. Useful for executing code outside the scope of
    * what is intended to be monitored.
    *
    * @see #unpause()
    * @since 1.0
    */
   public synchronized void pause() {
      checkPaused();
      pauseTime = System.nanoTime();
      paused = true;
   }

   /**
    * Sets the timer length in milliseconds of this {@code StopWatch} and notifies
    * the alarm to start ticking.
    *
    * @param timerLength new timer length
    * @return this {@code StopWatch}, for method chaining
    * @since 1.0
    */
   public synchronized StopWatch setTimer(final long timerLength) {
      if (timerLength < 0) {
         throw new IllegalArgumentException("StopWatch timer length cannot be less than 0.");
      }
      this.timerLength = NANOSECONDS.convert(timerLength, MILLISECONDS);
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
      setTimer(timerLength);
      startTime = System.nanoTime();
      return this;
   }

   public synchronized StopWatch startPaused() {
      return startPaused(0);
   }
   
   public synchronized StopWatch startPaused(final long timerLength) {
      if (isPaused()) {
         unpause(); //Unpause if already paused, as to not violate contract.
      }
      pause();
      return start(timerLength);
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
      if (paused) {
         return unit.convert(System.nanoTime() - (startTime + (System.nanoTime() - pauseTime)), NANOSECONDS);
      }
      return unit.convert(System.nanoTime() - startTime, NANOSECONDS);
   }

   /**
    *
    * @return
    */
   public synchronized long timeLeft() {
      return timerLength - timeElapsed(NANOSECONDS);
   }

   protected long timeLeft(final TimeUnit timeUnit) {
      return timeUnit.convert(timeLeft(), NANOSECONDS);
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
