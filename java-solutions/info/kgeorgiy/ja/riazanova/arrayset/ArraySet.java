package info.kgeorgiy.ja.riazanova.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements SortedSet<E> {
    private final List<E> elements;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        // :NOTE: not copy from other constructor
        this(Collections.emptyList(), null);
    }

    public ArraySet(Collection<? extends E> elements) {
        this(elements, null);
    }

    public ArraySet(Comparator<? super E> comparator) {
        this(Collections.emptyList(), comparator);
    }

    public ArraySet(ArraySet<E> set) {
        // :NOTE: new ArrayList
        this.elements = List.copyOf(set);
        this.comparator = set.comparator();
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        Set<E> set = new TreeSet<>(comparator);
        set.addAll(collection);
        this.elements = new ArrayList<>(set);
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        if (comparator == null) {
            List<E> elems = Arrays.asList(fromElement, toElement);
            elems.sort(null);

            if (elems.get(0) != fromElement) {
                throw new IllegalArgumentException("fromElement > toElement..");
            }
        } else {
            if (comparator.compare(fromElement, toElement) > 0) {
                throw new IllegalArgumentException("fromElement > toElement..");
            }
        }

        int from = find(fromElement);
        int to = find(toElement);

        return new ArraySet<>(elements.subList(from, to), comparator);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        int to = find(toElement);
        return new ArraySet<>(elements.subList(0, to), comparator);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        int from = find(fromElement);
        return new ArraySet<>(elements.subList(from, elements.size()), comparator);
    }

    @Override
    public E first() {
        throwIfEmpty();
        return elements.get(0);
    }

    @Override
    public E last() {
        throwIfEmpty();
        return elements.get(elements.size() - 1);
    }

    private void throwIfEmpty() {
        if (isEmpty()) {
            throw new NoSuchElementException("arrayset is empty..");
        }
    }

    private int find(E e) {
        int index = Collections.binarySearch(elements, e, comparator);

        if (index >= 0) {
            return index;
        } else {
            return -1 - index;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object e) {
        return Collections.binarySearch(elements, (E) e, comparator) >= 0;
    }
}
