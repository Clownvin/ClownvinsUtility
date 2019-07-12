package com.clownvin.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @author Clownvin
 *
 * @param <T>
 */
public class CircularList<T> implements Deque<T>, List<T> {

   /**
    *
    */
   public static final float DEFAULT_RESIZE_PERCENT = 1.2F;
   
   /**
    *
    */
   public static final int MIN_CAPACITY = 10;

   // How much to automatically resize by
   protected float resizeModifier;
   
   // Incremented for each operation that changes the array
   protected volatile long mods = 0L;
   
   protected volatile int head = 0;
   protected volatile int tail = 1;
   protected volatile int size = 0;

   // The underlying array
   protected T[] array;


   /**
    *
    */
   public CircularList() {
      this(CircularList.MIN_CAPACITY);
   }


   /**
    *
    * @param resizeModifier
    */
   public CircularList(final float resizeModifier) {
      this(CircularList.MIN_CAPACITY, resizeModifier);
   }


   /**
    *
    * @param initialCapacity
    */
   public CircularList(final int initialCapacity) {
      this(initialCapacity, CircularList.DEFAULT_RESIZE_PERCENT);
   }


   /**
    *
    * @param initialCapacity
    * @param resizeModifier
    */
   @SuppressWarnings("unchecked")
   public CircularList(final int initialCapacity, final float resizeModifier) {
      array = (T[]) new Object[Math.max(initialCapacity, CircularList.MIN_CAPACITY)];
      this.resizeModifier = Math.max(resizeModifier, CircularList.DEFAULT_RESIZE_PERCENT);
   }


   // TODO TODO TODO Revise
   /**
    *
    * @param index
    * @param entry
    */
   @Override
   public void add(final int index, final T entry) {
      if (index == (size - 1)) {                     // Add to tail shortcut
         add(entry);
         return;
      }
      if (index == 0) {                        // Add to head shortcut
         addToFront(entry);
         return;
      }
      if (index > size) {                      // Add beyond size shortcut
         set(index, entry);
      }
      mods++;
      ensureCapacity(size + 1);
      final int x = translate(index);
      final int y = translate(index + 1);
      if (x < tail) {
         int count;
         if (head < tail) {
            count = array.length - y;
         } else {
            count = size - y;
         }
         System.arraycopy(array, x, array, y, count);
      } else {
         final T swap = array[array.length - 1];
         if ((array.length - 1) != x) {
            System.arraycopy(array, x, array, y, array.length - y);
         }
         System.arraycopy(array, 0, array, 1, tail);
         array[0] = swap;
      }
      // Finish any that fall through
      array[x] = entry;
      size++;
      tail = (tail + 1) % array.length;
   }

   /**
    *
    * @param entry
    * @return
    */
   @Override
   public boolean add(final T entry) {
      mods++;
      ensureCapacity(size + 1);
      array[tail++] = entry;
      tail %= array.length;
      size++;
      return true;
   }


   /**
    *
    * @param entries
    * @return
    */
   @Override
   public boolean addAll(final Collection<? extends T> entries) {
      ensureCapacity(size + entries.size());
      mods++;
      @SuppressWarnings("unchecked")
      final T[] entryArray = (T[]) entries.toArray();
      final int count = entryArray.length;
      //final int oldTail = tail;
      //
      if (((tail + count) <= array.length) || isWrapping()) {
         System.arraycopy(entryArray, 0, array, tail, count);
      } else {
         final int len = array.length - tail;
         System.arraycopy(entryArray, 0, array, tail, len);
         System.arraycopy(entryArray, len, array, 0, count - len);
      }
      tail = (tail + count) % array.length;
      size += count;
      return true;
   }

   @Override
   public boolean addAll(final int index, final Collection<? extends T> entries) {
      if (index == 0) {
         return addAllFirst(entries); // Add to head shortcut
      }
      if (index == size) {
         return addAll(entries); // Add to tail shortcut
      }
      int i = 0;
      for (final T entry : entries) {
         add(index + i++, entry);
      }
      return true;
   }

   /**
    *
    * @param entries
    * @return
    */
   public boolean addAllFirst(final Collection<? extends T> entries) {
      ensureCapacity(size + entries.size());
      @SuppressWarnings("unchecked")
      final T[] entryArray = (T[]) entries.toArray();
      final int count = entryArray.length;
      final int oldHead = head;
      head = corner(head - count);
      mods++;
      if (head > oldHead) {
         System.arraycopy(entryArray, 0, array, 0, oldHead);
         System.arraycopy(entryArray, oldHead, array, head, array.length - head);
      } else {
         System.arraycopy(entryArray, 0, array, head, count);
      }
      size += count;
      return true;
   }

   /**
    *
    * @param entry
    */
   @Override
   public void addFirst(final T entry) {
      addToFront(entry);
   }

   /**
    *
    * @param entry
    */
   @Override
   public void addLast(final T entry) {
      add(entry);
   }

   /**
    *
    * @param entry
    * @return
    */
   public boolean addToFront(final T entry) {
      ensureCapacity(size + 1);
      array[head = corner(head - 1)] = entry;
      mods++;
      size++;
      return true;
   }

   /**
    *
    * @param collection
    * @param complement
    * @return
    */
   protected boolean batchRemove(final Collection<?> collection, final boolean complement) {
      int offset = 0;
      int actual;
      for (int i = 0; i < size; i++) {
         actual = translate(i);
         if (collection.contains(array[actual]) == complement) {
            array[translate(i - offset)] = array[actual];
         } else {
            offset++;
         }
      }
      for (int i = 0; i < offset; i++) {
         array[translate(size - i)] = null;
      }
      tail = corner(tail - offset);
      size -= offset;
      mods += offset;
      return offset != 0;
   }

   /**
    *
    * @param startModifications
    */
   protected void checkForConcurrentModification(final long startModifications) {
      if (startModifications != mods) {
         throw new ConcurrentModificationException("CycleQueue was modified by a thread while being accessed.");
      }
   }
   
   /**
    *
    */
   @SuppressWarnings("unchecked")
   @Override
   public void clear() {
      array = (T[]) new Object[array.length];
      size = head = 0;
      tail = 1;
      mods++;
   }

   /**
    *
    * @param entry
    * @return
    */
   @Override
   public boolean contains(final Object entry) {
      for (final T other : this) {
         if (entry.equals(other)) {
            return true;
         }
      }
      return false;
   }

   /**
    *
    * @param entries
    * @return
    */
   @Override
   public boolean containsAll(final Collection<?> entries) {
      for (final Object entry : entries) {
         if (!contains(entry)) {
            return false;
         }
      }
      return true;
   }

   /**
    *
    * @param index
    * @return
    */
   protected int corner(final int index) {
      return index < 0 ? index + array.length : index % array.length;
   }


   /**
    *
    * @return
    */
   @Override
   public Iterator<T> descendingIterator() {
      return new Iterator<>() {

         int        currIndex = size - 1;
         final long startMod  = mods;

         @Override
         public boolean hasNext() {
            checkForConcurrentModification(startMod);
            return currIndex > 0;
         }

         @Override
         public T next() {
            checkForConcurrentModification(startMod);
            return get(currIndex--);
         }

      };
   }


   /**
    *
    * @return
    */
   @Override
   public T element() {
      if (isEmpty()) {
         throw new IllegalStateException("CycleQueue is empty.");
      }
      return get(0);
   }

   /**
    *
    * @param capacity
    */
   protected void ensureCapacity(final int capacity) {
      if (capacity <= array.length) {
         return;
      }
      resize((int) (capacity * resizeModifier));
   }

   /**
    *
    * @return
    */
   public boolean full() {
      return size == array.length;
   }

   /**
    *
    * @param index
    * @return
    */
   @Override
   public T get(final int index) {
      return array[translate(index)];
   }

   /**
    *
    * @return
    */
   @Override
   public T getFirst() {
      return get(0);
   }

   /**
    *
    * @return
    */
   @Override
   public T getLast() {
      return get(size - 1);
   }

   protected int getTail() {
      return tail;
   }

   /**
    *
    * @param entry
    * @return
    */
   @Override
   public int indexOf(final Object entry) {
      if (entry == null) {
         for (int i = 0; i < size; i++) {
            if (get(i) == null) {
               return i;
            }
         }
      } else {
         for (int i = 0; i < size; i++) {
            if (entry.equals(get(i))) {
               return i;
            }
         }
      }
      return -1;
   }

   /**
    *
    * @return
    */
   @Override
   public boolean isEmpty() {
      return size == 0;
   }

   protected boolean isWrapping() {
      return ((head >= tail) && (tail != 0)) || (full() && (head != 0));
   }

   /**
    *
    * @return
    */
   @Override
   public Iterator<T> iterator() {
      return new Iterator<>() {

         int currIndex = 0;

         final long startMod = mods;

         @Override
         public boolean hasNext() {
            checkForConcurrentModification(startMod);
            return currIndex != size;
         }

         @Override
         public T next() {
            checkForConcurrentModification(startMod);
            return get(currIndex++);
         }

      };
   }

   /**
    *
    * @param object
    * @return
    */
   @Override
   public int lastIndexOf(final Object object) {
      if (object == null) {
         for (int i = size - 1; i >= 0; i--) {
            if (get(i) == null) {
               return i;
            }
         }
      } else {
         for (int i = size - 1; i >= 0; i--) {
            if (object.equals(get(i))) {
               return i;
            }
         }
      }
      return -1;
   }

   /**
    *
    * @return
    */
   @Override
   public ListIterator<T> listIterator() {
      return listIterator(0);
   }
   
   /**
    *
    * @param start
    * @return
    */
   @Override
   public ListIterator<T> listIterator(final int start) {
      return new ListIterator<>() {

         int index = start - 1;

         @Override
         public void add(final T entry) {
            CircularList.this.add(entry);
         }

         @Override
         public boolean hasNext() {
            return index != size;
         }

         @Override
         public boolean hasPrevious() {
            return index > 0;
         }

         @Override
         public T next() {
            return get(++index);
         }

         @Override
         public int nextIndex() {
            return index + 1;
         }

         @Override
         public T previous() {
            return get(--index);
         }

         @Override
         public int previousIndex() {
            return index - 1;
         }

         @Override
         public void remove() {
            CircularList.this.remove(index);
         }

         @Override
         public void set(final T entry) {
            CircularList.this.set(index, entry);
         }

      };
   }

   /**
    *
    * @param index
    */
   protected void lowerRangeCheck(final int index) {
      if (index < 0) {
         throw new ArrayIndexOutOfBoundsException("Index must be cannot be less than 0");
      }
   }

   @Override
   public boolean offer(final T e) {
      return add(e);
   }

   @Override
   public boolean offerFirst(final T e) {
      return addToFront(e);
   }

   @Override
   public boolean offerLast(final T e) {
      return add(e);
   }

   @Override
   public T peek() {
      if (isEmpty()) {
         return null;
      }
      return element();
   }

   @Override
   public T peekFirst() {
      if (isEmpty()) {
         return null;
      }
      return getFirst();
   }

   @Override
   public T peekLast() {
      if (isEmpty()) {
         return null;
      }
      return getLast();
   }

   @Override
   public T poll() {
      if (isEmpty()) {
         return null;
      }
      return remove();
   }

   @Override
   public T pollFirst() {
      if (isEmpty()) {
         return null;
      }
      return remove();
   }

   @Override
   public T pollLast() {
      if (isEmpty()) {
         return null;
      }
      return removeFromRear();
   }

   @Override
   public T pop() {
      return removeFromRear();
   }

   @Override
   public void push(final T e) {
      add(e);
   }

   /**
    *
    * @param index
    */
   protected void rangeCheck(final int index) {
      if ((index < 0) || (index >= size)) {
         throw new ArrayIndexOutOfBoundsException(index + " is not within the range: [0, " + size + ")");
      }
   }

   @Override
   public T remove() {
      if (isEmpty()) {
         throw new IllegalStateException("CycleQueue is currently empty.");
      }
      final T returnVal = array[head];
      array[head++] = null;
      mods++;
      head %= array.length;
      size--;
      return returnVal;
   }

   @Override
   public T remove(final int index) {
      // Remove head shortcut
      if (index == 0) {
         return remove();
      }
      // Remove tail shortcut
      if (index == (size - 1)) {
         return removeFromRear();
      }
      rangeCheck(index);
      final int x = translate(index);
      final int y = translate(index + 1);
      final T val = array[x];
      if (((head < tail) || (tail == 0)) || (x < head)) {
         // Shift array[x + 1, tail] left
         System.arraycopy(array, y, array, x, size - index - 1);
      } else {
         // Store array[0] in swap
         // Shift array[1, tail] left
         // Shift array[x + 1, array.length] left
         // Store swap in array[array.length]
         final T swap = array[0];
         System.arraycopy(array, 1, array, 0, tail - 1);
         System.arraycopy(array, y, array, x, array.length - x - 1);
         array[array.length - 1] = swap;
      }
      tail = corner(tail - 1);
      array[tail] = null;
      size--;
      mods++;
      return val;
   }

   @Override
   public boolean remove(final Object o) {
      final int indexOf = indexOf(o);
      if (indexOf == -1) {
         return false;
      }
      remove(indexOf);
      return true;
   }

   @Override
   public boolean removeAll(final Collection<?> c) {
      return batchRemove(c, false);
   }

   @Override
   public T removeFirst() {
      return remove();
   }

   @Override
   public boolean removeFirstOccurrence(final Object o) {
      return remove(o);
   }

   public T removeFromRear() {
      if (isEmpty()) {
         throw new IllegalStateException("CycleQueue is currently empty.");
      }
      int index;
      if (head < tail) { // Shortcut
         index = tail--;
      } else {
         index = tail = corner(tail - 1);
      }
      final T val = array[index];
      array[tail] = null;
      mods++;
      size--;
      return val;
   }
   
   @Override
   public T removeLast() {
      return removeFromRear();
   }

   @Override
   public boolean removeLastOccurrence(final Object o) {
      final int indexOf = lastIndexOf(o);
      if (indexOf == -1) {
         return false;
      }
      remove(indexOf);
      return true;
   }
   
   //   protected int getHead() {
   //      return head;
   //   }
   //   
   //   protected int getCapacity() {
   //      return array.length;
   //   }
   //   
   //   protected int getHeadSize() {
   //      return getCapacity() - getHead();
   //   }

   @SuppressWarnings("unchecked")
   public void resize(final int newSize) {
      final T[] array = (T[]) new Object[Math.max(newSize, CircularList.MIN_CAPACITY)];
      if (!isEmpty()) {
         if (isWrapping()) {
            final int preWrap = this.array.length - head;
            System.arraycopy(this.array, head, array, 0, preWrap);
            System.arraycopy(this.array, 0, array, preWrap, tail);
         } else {
            System.arraycopy(this.array, head, array, 0, size);
         }
      }
      this.array = array;
      head = 0;
      tail = size;
      mods++;
   }

   @Override
   public boolean retainAll(final Collection<?> c) {
      return batchRemove(c, true);
   }

   @SuppressWarnings("unchecked")
   public void reverse() {
      for (int i = 0; i < (size / 2); i++) {
         final Object o = get(i);
         set(i, get(size - (i + 1)));
         set(size - (i + 1), (T) o);
      }
   }

   @Override
   public T set(final int index, final T element) {
      lowerRangeCheck(index);
      if (index > array.length) {
         resize((int) ((index + 1) * resizeModifier));
      }
      if (index >= size) {
         size = index + 1;
      }
      array[translate(index)] = element;
      mods++;
      return element;
   }

   @Override
   public int size() {
      return size;
   }

   @Override
   public List<T> subList(final int fromIndex, final int toIndex) {
      final List<T> sub = new CircularList<>(toIndex - fromIndex);
      for (int i = fromIndex; i < toIndex; i++) {
         sub.add(get(i));
      }
      return sub;
   }

   @Override
   public Object[] toArray() {
      final Object[] array = new Object[size];
      for (int i = 0; i < size; i++) {
         array[i] = get(i);
      }
      return array;
   }

   @Override
   @SuppressWarnings({
         "hiding", "unchecked" // Safe to assume that array will ONLY contain types implementing T
   })
   public <T> T[] toArray(final T[] array) {
      for (int i = 0; i < array.length; i++) {
         array[i] = (T) get(i);
      }
      return array;
   }

   @Override
   public String toString() {
      return "{ CircularList: { size: " + size + ", head: " + head + ", tail: " + tail + ", capacity: " + array.length + ", mods: " + mods + "}}";
   }

   protected int translate(final int index) {
      return (head + index) % array.length;
   }
}
