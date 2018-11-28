package com.git.clownvin.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
/**
 * Queue with singly-linked list under the hood.
 */
public final class LinkedQueue<T> implements Queue<T> {
	private final class Link {
		private Link next;
		private final T t;
		
		private Link(T t) {
			this.t = t;
		}
	}
	
	private volatile Link head = null;
	private volatile Link tail = null;
	private volatile int size = 0;
	private volatile long mods = 0L;

	@Override
	public synchronized int size() {
		return size;
	}

	@Override
	public synchronized boolean isEmpty() {
		return size == 0;
	}

	@Override
	public synchronized boolean contains(Object o) {
		for (T t : this) {
			if (!t.equals(o))
				continue;
			return true;
		}
		return false;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			Link base = null;
			final long startMod = mods;

			@Override
			public boolean hasNext() {
				if (mods != startMod)
					throw new ConcurrentModificationException();
				return (base == null && !isEmpty()) || base.next != null; //If base == null, just started. I'd hope there's next.
			}

			@Override
			public T next() {
				if (mods != startMod)
					throw new ConcurrentModificationException();
				if (base == null)
					base = LinkedQueue.this.head;
				else
					base = base.next;
				return base.t;
			}

		};
	}

	@Override
	public synchronized Object[] toArray() {
		Object[] array = new Object[size()];
		Link current = head;
		int i = 0;
		while (current != tail) {
			array[i++] = current.t;
			current = current.next;
		}
		array[i] = current.t;
		return array;
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <V> V[] toArray(V[] array) {
		if (array.length < size())
			throw new IllegalArgumentException("Array argument length must be atleast as long as queue size.");
		Link current = head;
		int i = 0;
		while (current != tail) {
			array[i++] = (V) current.t;
			current = current.next;
		}
		array[i] = (V) current.t;
		return array;
	}

	@Override
	public synchronized boolean add(T t) {
		long startMod = ++mods;
		Link link = new Link(t);
		if (tail == null || head == null)
			head = tail = link;
		else
			tail = tail.next = link;
		size++;
		if (mods != startMod)
			throw new ConcurrentModificationException(startMod+", "+mods);
		return true;
	}

	@Override
	public synchronized boolean remove(Object o) {
		if (head == null)
			return true;
		Link last = new Link(null);
		Link current = head;
		Link temp = null;
		while (current != tail) {
			if (!current.t.equals(o)) {
				last = current;
				current = current.next;
				continue;
			}
			if (current == head) {
				current = head = current.next;
				continue;
			}
			temp = current;
			last.next = current = current.next;
			last = temp;
		}
		if (current.t.equals(o)) {
			tail = last;
			last.next = null;
		}
		return true;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object t : c) {
			if(!contains(t)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T t : c) {
			if(add(t))
				continue;
			return false;
		}
		return true;
	}

	@Override
	public synchronized boolean removeAll(Collection<?> c) {
		for (Object t : c) {
			if(remove(t))
				continue;
			return false;
		}
		return true;
	}

	//TODO TODO TODO TODO Untested
	@Override
	public synchronized boolean retainAll(Collection<?> c) {
		Link current = head;
		outer: while (current != tail) {
			for (Object t : c) {
				if (current.t == t) {
					current = current.next;
					continue outer;
				}
			}
			Link next = current.next;
			remove(current.t);
			current = next;
		}
		return true;
	}

	@Override
	public synchronized void clear() {
		long startMod = ++mods;
		head = null; //I'm pretty sure this is the only reference to all the other nodes? preeeeetty sure doing this removes ref
		tail = null;
		if (mods != startMod)
			throw new ConcurrentModificationException();
	}

	@Override
	public boolean offer(T t) {
		return add(t);
	}

	@Override
	public synchronized T remove() {
		long startMod = ++mods;
		T t;
		if (isEmpty())
			throw new NoSuchElementException("Queue underflow");
		t = head.t;
		head = head.next;
		size--;
		if (mods != startMod)
			throw new ConcurrentModificationException();
		return t;
	}

	@Override
	public T poll() {
		return remove();
	}

	@Override
	public synchronized T element() {
		long startMod = ++mods;
		T t;
		if (isEmpty())
			throw new NoSuchElementException("Queue underflow");
		t = head.t;
		if (mods != startMod)
			throw new ConcurrentModificationException();
		return t;
	}

	@Override
	public T peek() {
		return element();
	}
	
	@Override
	public synchronized String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		Link current = head;
		while (current != tail) {
			builder.append(current.t+", ");
			current = current.next;
		}
		builder.append(current.t+"]");
		return builder.toString();
	}
}
