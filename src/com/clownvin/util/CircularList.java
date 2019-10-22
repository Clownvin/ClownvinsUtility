package com.clownvin.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @param <T>
 */
public class CircularList<T> extends AbstractList<T> implements List<T> {

  /**
   *
   */
  public static final float DEFAULT_RESIZE_PERCENT = 1.2F;

  /**
   *
   */
  public static final int MIN_CAPACITY = 10;

  // How much to automatically resize by
  protected final float resizeModifier;

  protected volatile int head = 0;
  protected volatile int tail = 0;
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
  
  protected void rangeCheck(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(index);
    }
  }
  
  protected int normalizeIndex(int index) {
    if (index < 0) {
      index += size;
    }
    return index;
  }

  @SuppressWarnings("unchecked")
  protected void ensureCapacity(int index) {
    if (array.length > index) {
      return;
    }
    T[] array = (T[]) new Object[(int) (this.array.length * resizeModifier)];
    if (head >= tail && tail != 0) {
      System.arraycopy(this.array, head, array, 0, this.array.length - head);
      System.arraycopy(this.array, 0, array, this.array.length - head, tail);
    } else {
      System.arraycopy(this.array, 0, array, 0, this.array.length);
    }
    head = 0;
    tail = size;
    this.array = array;
  }
  
  private void shiftHeadLeft(int index) {
    System.arraycopy(array, head, array, head - 1, index - head);
    head = cornerLeft(head - 1);
  }
  
  private void shiftHeadRight(int index) {
    System.arraycopy(array, head, array, head + 1, index - head);
    head = cornerRight(head + 1);
  }
  
  private void shiftTailLeft(int index) {
    System.arraycopy(array, index + 1, array, index, tail - index);
    tail = cornerLeft(tail - 1);
  }
  
  private void shiftTailRight(int index) {
    System.arraycopy(array, index, array, index + 1, tail - index);
    tail = cornerRight(tail + 1);
  }
  
  protected int cornerLeft(int index) {
    return (index + array.length) % array.length;
  }
  
  protected int cornerRight(int index) {
    return index % array.length;
  }
  
  protected int translate(int index) {
    return (index + head) % array.length;
  }
  
  protected int convert(int index) {
    index = normalizeIndex(index);
    rangeCheck(index);
    return translate(index);
  }

  @Override
  public boolean add(T val) {
    ensureCapacity(size);
    array[tail++] = val;
    tail %= this.array.length;
    size++;
    return true;
  }

  @Override
  public void add(int index, T val) {
    ensureCapacity(size);
    if (index >= size) {
      add(val);
    } else if (index == 0) {
      addHead(val);
    } else {
      addMiddle(index, val);
    }
  }
  
  private void addHead(T val) {
    head = cornerLeft(head - 1);
    array[head] = val;
    size++;
  }
  
  private void addMiddleTail(int index, T val) {
    shiftTailRight(index);
    array[index] = val;
    size++;
  }
  
  private void addMiddleHead(int index, T val) {
    shiftHeadLeft(index);
    array[index - 1] = val;
    size++;
  }
  
  public void addMiddle(int index, T val) {
    index = convert(index);
    if (index < tail) {
      addMiddleTail(index, val);
    } else {
      addMiddleHead(index, val);
    }
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    ensureCapacity(size + collection.size() - 1);
    for (T val : collection) {
      array[tail++] = val;
      tail = cornerRight(tail);
    }
    size += collection.size();
    return true;
  }

  //TODO could be optimized greatly.
  @Override
  public boolean addAll(int index, Collection<? extends T> collection) {
    for (T val : collection) {
      add(index++, val);
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    array = (T[]) new Object[array.length];
    size = 0;
    tail = 0;
    head = 0;
  }

  @Override
  public boolean contains(Object val) {
    for (int i = head, j = 0; j < size; i = (i + 1) % array.length) {
      if (array[i].equals(val)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    for (Object val : collection) {
      if (!contains(val)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public T get(int index) {
    return array[translate(normalizeIndex(index))];
  }

  @Override
  public int indexOf(Object val) {
    for (int i = 0; i < size; i++) {
      if (get(i).equals(val)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int lastIndexOf(Object val) {
    for (int i = size; i >= 0; i--) {
      if (get(i).equals(val)) {
        return i;
      }
    }
    return -1;
  }
  
  @Override
  public boolean equals(Object other) {
    if (!(other instanceof CircularList)) {
      return false;
    }
    CircularList<?> otherList = (CircularList<?>) other;
    for (int i = 0; i < size; i++) {
      if (!get(i).equals(otherList.get(i))) {
        return false;
      }
    }
    return true;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append('[');
    for (int i = 0; i < size; i++) {
      builder.append(get(i).toString());
      if (i < size - 1) {
        builder.append(", ");
      }
    }
    builder.append(']');
    return builder.toString();
  }

  @Override
  public boolean remove(Object val) {
    int index = indexOf(val);
    if (index == -1) {
      return false;
    }
    remove(index);
    return true;
  }

  @Override
  public T remove(int index) {
    if (index == 0) {
      return removeHead();
    } else if (index == size - 1) {
      return removeTail();
    }
    return removeMiddle(index);
  }
  
  private T removeHead() {
    T toReturn = array[head++];
    head = cornerRight(head);
    size--;
    return toReturn;
  }
  
  private T removeTail() {
    tail = cornerLeft(tail - 1);
    size--;
    return array[tail];
  }
  
  private T removeMiddle(int index) {
    index = convert(index);
    T toReturn = array[index];
    shiftArray(index);
    size--;
    return toReturn;
  }
  
  private void shiftArray(int index) {
    if (index < tail) {
      shiftTailLeft(index);
    } else {
      shiftHeadRight(index);
    }
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    for (Object val : collection) {
      for (int i = 0; i < size; i++) {
        if (get(i).equals(val)) {
          remove(i--);
        }
      }
    }
    return true;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    for (int i = 0; i < size; i++) {
      if (!collection.contains(get(i))) {
        remove(i--);
      }
    }
    return true;
  }

  @Override
  public T set(int index, T val) {
    index = convert(index);
    T toReturn = array[index];
    array[index] = val;
    return toReturn;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public List<T> subList(int arg0, int arg1) {
    CircularList<T> subList = new CircularList<>(array.length);
    subList.addAll(this);
    return subList;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object[] toArray() {
    T[] array = (T[]) new Object[size];
    for (int i = 0; i < size(); i++) {
      array[i] = get(i);
    }
    return array;
  }

  @SuppressWarnings({ "unchecked", "hiding" })
  @Override
  public <T> T[] toArray(T[] array) {
    for (int i = 0; i < size() && i < array.length; i++) {
      array[i] = (T) get(i);
    }
    return array;
  }

}
