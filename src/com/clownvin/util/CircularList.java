package com.clownvin.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * @param <T>
 */
public class CircularList<T> implements List<T> {
  
  protected class CircularIterator implements Iterator<T> {
    int index;
    
    protected CircularIterator() {
      this(0);
    }
    
    protected CircularIterator(int start) {
      index = start;
    }

    @Override
    public boolean hasNext() {
      return index < size;
    }

    @Override
    public T next() {
      return get(index++);
    }
    
  };
  
  protected class CircularListIterator extends CircularIterator implements ListIterator<T> {
    
    protected CircularListIterator() {
      this(0);
    }
    
    protected CircularListIterator(int start) {
      super(start);
    }

    @Override
    public void add(T val) {
      CircularList.this.add(index++, val);
    }

    @Override
    public boolean hasPrevious() {
      return index > 0;
    }

    @Override
    public int nextIndex() {
      return index;
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
    public void set(T val) {
      CircularList.this.set(index, val);
    }
    
  }

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
      return;
    } else if (index == 0) {
      head = --head < 0 ? array.length - 1 : head;
      array[head] = val;
    } else {
      index = normalizeIndex(index);
      rangeCheck(index);
      index = (index + head) % array.length;
      if (index < tail) {
        System.arraycopy(array, index, array, index + 1, tail - index);
        tail = ++tail % array.length;
        array[index] = val;
      } else {
        System.arraycopy(array, head, array, head - 1, index - head);
        head = --head < 0 ? array.length - 1 : head;
        array[index - 1] = val;
      }
    }
    size++;
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    ensureCapacity(size + collection.size() - 1);
    for (T val : collection) {
      array[tail++] = val;
      tail %= array.length;
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
    return array[(head + index) % array.length];
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
  public Iterator<T> iterator() {
    return new CircularIterator();
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
  public ListIterator<T> listIterator() {
    return new CircularListIterator();
  }

  @Override
  public ListIterator<T> listIterator(int start) {
    return new CircularListIterator(start);
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
    T toReturn = null;
    if (index == 0) {
      toReturn = array[head++];
      head %= array.length;
    } else if (index == size - 1) {
      tail = --tail < 0 ? array.length - 1 : tail;
      toReturn = array[tail];
    } else {
      index = normalizeIndex(index);
      rangeCheck(index);
      index = (head + index) % array.length;
      toReturn = array[index];
      if (index < tail) {
        System.arraycopy(array, index + 1, array, index, tail - index);
        tail = --tail < 0 ? array.length - 1 : tail;
      } else {
        System.arraycopy(array, head, array, head + 1, index - head);
        head = ++head % array.length;
      }
    }
    size--;
    return toReturn;
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
    index = normalizeIndex(index);
    rangeCheck(index);
    index = (head + index) % array.length;
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
    // TODO Auto-generated method stub
    return null;
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
