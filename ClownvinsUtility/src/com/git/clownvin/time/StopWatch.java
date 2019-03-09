package com.git.clownvin.time;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Clownvin
 * @version 1.0
 * 
 */
public class StopWatch {

   /**
    * Globally instanced StopWatch object, meant to be statically imported.
    */
   public static final StopWatch GLOBAL_STOPWATCH = new StopWatch();
   
   /**
    * Holds the value of {@code System.nanoTime()} called during {@link #start start}.
    */
   protected long start;
   /**
    *
    */
   protected long pause;
   /**
    *
    */
   protected long duration = 0;

   /**
    *
    */
   protected volatile boolean stopped = true;
   /**
    *
    */
   protected volatile boolean paused  = false;
   
   /**
    * 
    */
   protected Thread alarm;
   
   /**
    * 
    */
   protected final Runnable alarmRunnable = () -> {
      while (true) { //TODO consider adding ternary condition
         synchronized (alarm) {
            while (stopped || expired()) {
               try {
                  alarm.wait();
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
    * 
    */
   public StopWatch() {
      alarm = new Thread(alarmRunnable);
      alarm.setName("StopWatch@" + hashCode() + ".alarm");
      alarm.start();
   }

   /**
    * 
    */
   protected void checkPaused() {
      if (paused) {
         throw new RuntimeException("StopWatch is paused!");
      }
   }

   /**
    * 
    */
   protected void checkStopped() {
      if (stopped) {
         throw new RuntimeException("StopWatch is not started!");
      }
   }

   /**
    * 
    */
   protected void checkUnpaused() {
      if (!paused) {
         throw new RuntimeException("StopWatch is not paused!");
      }
   }

   /**
    * 
    * @return
    */
   public synchronized boolean expired() {
      return timeLeft() < 0;
   }

   /**
    * 
    */
   public synchronized void pause() {
      pause = System.nanoTime();
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
      start = System.nanoTime();
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
      return unit.convert(System.nanoTime() - (paused ? start + (System.nanoTime() - pause) : start), NANOSECONDS);
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
      start += System.nanoTime() - pause;
   }
}
