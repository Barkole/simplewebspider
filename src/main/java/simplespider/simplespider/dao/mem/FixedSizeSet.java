package simplespider.simplespider.dao.mem;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

class FixedSizeSet<E> implements SimpleSet<E> {
    private final int           maxSize;
    private final Set<E>        set = new LinkedHashSet<E>();
    private final Random        rnd = new Random();

    // use ReentrantLock instead of synchronized for scalability
    private final ReentrantLock lock;

    public FixedSizeSet(final int maxSize) {
        this.maxSize = maxSize;
        this.lock = new ReentrantLock(false);
    }

    @Override
    public E remove() {
        lock.lock();
        try {
            final int size = set.size();
            if (size == 0) {
                return null;
            }

            final int toBeDeleted = rnd.nextInt(size);
            int counter = 0;
            for (final Iterator<E> iterator = set.iterator(); iterator.hasNext(); counter++) {
                final E element = iterator.next();
                if (counter == toBeDeleted) {
                    iterator.remove();
                    return element;
                }
            }

            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean put(final E e) {
        lock.lock();
        try {
            if (set.size() >= maxSize) {
                remove();
            }
            return set.add(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void addAll(final Collection<? extends E> c) {
        lock.lock();
        try {
            for (final E e : c) {
                put(e);
            }
        } finally {
            lock.unlock();
        }
    }

}
