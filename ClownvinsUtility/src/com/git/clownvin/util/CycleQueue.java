package com.git.clownvin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Queue;

//TODO TODO TODO TODO TODO Consider implementing resizing functionality, similar to ArrayList.

public final class CycleQueue<T> implements Queue<T> {
	private final Object[] array;
	private volatile int takeIndex = 0;
	private volatile int placeIndex = 0;
	private volatile int size = 0;
	private volatile long modifications = 0;

	private final boolean deleteWhenFull;

	public CycleQueue(final int totalAmount) {
		this(totalAmount, false);
	}

	public CycleQueue(final int totalAmount, final boolean deleteWhenFull) {
		array = new Object[totalAmount];
		this.deleteWhenFull = deleteWhenFull;
	}

	@SuppressWarnings("unchecked")
	public void reverse() {
		ensureCorners();
		for (int i = 0; i < size / 2; i++) {
			Object o = get(i);
			set(i, get(size - (i + 1)));
			set(size - (i + 1), (T) o);
		}
	}

	@Override
	public synchronized boolean add(T e) {
		if (size == array.length && deleteWhenFull) {
			remove();
		}
		ensureCorners();
		if (size == array.length) {
			throw new IllegalStateException("CycleQueue is currently full.");
		}
		array[placeIndex++] = e;
		++size;
		modifications++;
		return true;
	}

	public boolean full() {
		return size == array.length;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T o : c) {
			add(o);
		}
		return true;
	}

	@Override
	public synchronized void clear() {
		for (int i = 0; i < array.length; i++) {
			array[i] = null;
		}
		size = 0;
		takeIndex = 0;
		placeIndex = 0;
		modifications++;
	}

	@Override
	public synchronized boolean contains(Object o) {
		for (int i = takeIndex, j = 0; j < size; i++, j++) {
			i %= array.length;
			if (array[i].equals(o)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	public int getCapacity() {
		return array.length;
	}

	@Override
	public T element() {
		if (isEmpty()) {
			throw new IllegalStateException("CycleQueue is empty.");
		}
		return get(0);
	}

	@SuppressWarnings("unchecked")
	public synchronized T get(int index) {
		index = (index + takeIndex) % array.length;
		return (T) array[index];
	}

	private synchronized void set(int index, T object) {
		index = (index + takeIndex) % array.length;
		array[index] = object;
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			int currIndex = 0;
			final long startMod = modifications;

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

	@Override
	public synchronized boolean offer(T e) {
		if (size == array.length) {
			return false;
		}
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
	public T poll() {
		if (isEmpty()) {
			return null;
		}
		return remove();
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized T remove() {
		ensureCorners();
		if (size == 0) {
			throw new IllegalStateException("CycleQueue is currently empty.");
		}
		T returnVal = (T) array[takeIndex];
		array[takeIndex++] = null;
		--size;
		modifications++;
		return returnVal;
	}

	@Override
	public synchronized boolean remove(Object o) {
		boolean pushingDown = false;
		int times = 0;
		int i = takeIndex;
		for (int j = 0; j < size; i++, j++) {
			i %= array.length;
			if (array[i].equals(o)) {
				pushingDown = true;
				times++;
				continue;
			}
			if (pushingDown) {
				int index = i - times;
				if (index < 0) {
					index += array.length;
				}
				array[index] = array[i];
			}
		}
		for (int j = 0; j < times; j++) {
			array[i - j] = null;
		}
		size -= times;
		modifications++;
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for (Object o : c) {
			remove(o);
		}
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		Collection<Object> list = new ArrayList<>();
		for (Object o : c) {
			if (contains(o)) {
				continue;
			}
			list.add(o);
		}
		return removeAll(list);
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public synchronized Object[] toArray() {
		Object[] object = new Object[size];
		for (int i = takeIndex, j = 0; j < size; i++, j++) {
			i %= array.length;
			object[j] = array[i];
		}
		return object;
	}

	@Override
	@SuppressWarnings({ "hiding", "unchecked" }) // Safe to assume that array will ONLY contain types implementing T
	public synchronized <T> T[] toArray(T[] a) {
		for (int i = takeIndex, j = 0; j < size; i++, j++) {
			i %= array.length;
			a[j] = (T) array[i];
		}
		return a;
	}

	private void checkForConcurrentModification(final long startModifications) {
		if (startModifications != modifications) {
			throw new ConcurrentModificationException("CycleQueue was modified by a thread while being accessed.");
		}
	}

	private synchronized void ensureCorners() {
		takeIndex %= array.length;
		placeIndex %= array.length;
	}
}