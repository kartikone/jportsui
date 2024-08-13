package oz.zomg.jport.common;

import oz.zomg.jport.common.Interfacing_.Transformable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

//ENHANCED

/**
 * Report occurrences of keys as frequency counts.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @param <K> keys are of class type
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Histogram2<K> {
    static // initializer block
    {
    }

    final private Class<K> fKeysOfClassType;
    final private Map<K, Integer> fKey_to_IntegerMap;

    /**
     * @param ofClassType for reified arrays of keys
     */
    public Histogram2(final Class<K> ofClassType) {
        if (ofClassType == null) throw new NullPointerException();

        fKeysOfClassType = ofClassType;
        fKey_to_IntegerMap = new HashMap<K, Integer>();
        //... could have conditional assigned from Enum.class.isAssignableFrom(...) and new EnumMap<K, Integer>( kOfClassType ) but genererics noooo
    }

    /**
     * @param key increment the histogram for the occurring key
     * @return count of occurrences after incrementing
     */
    public int increment(final K key) {
        if (!fKey_to_IntegerMap.containsKey(key)) {   // special initial case
            fKey_to_IntegerMap.put(key, CachedInteger_.ONE);
            return 1;
        } else {
            final int count = 1 + fKey_to_IntegerMap.get(key).intValue();
            fKey_to_IntegerMap.put(key, CachedInteger_.valueOf(count));
            return count;
        }
    }

    /**
     * @return 'true' if the histogram is empty of any occurrences
     */
    public boolean isEmpty() {
        return fKey_to_IntegerMap.isEmpty();
    }

    /**
     * @return 'true' if there is only a unique / singular key in the map
     */
    public boolean isSingular() {
        return fKey_to_IntegerMap.size() == 1;
    }

    /**
     * @return array of keys that occurred at least once in the histogram in hash code order
     */
    public K[] getKeys() {
        final K[] keys = Util.createArray(fKeysOfClassType, fKey_to_IntegerMap.keySet());
        return keys;
    }

    /**
     * @return array of Key-count Entries in ascending count order
     */
    public Entry<K, Integer>[] getKeyFrequencyEntries() {
        final Entry<K, Integer>[] kvEntries = Util.createMapEntryArray(fKey_to_IntegerMap);

        // para-lambda for ascending Map.Entry Integer compares
        // non-static because of Generics
        final Comparator<Entry<K, Integer>> comparator = new Comparator<Entry<K, Integer>>() // anonymous class
        {
            @Override
            public int compare(Entry<K, Integer> o1, Entry<K, Integer> o2) {
                if (o1 == o2) return 0; // equal obj refs
                final int i1 = o1.getValue().intValue();
                final int i2 = o2.getValue().intValue();
                return (i1 < i2) ? -1
                        : (i1 > i2) ? 1
                        : 0;
            }
        };
        Arrays.sort(kvEntries, comparator);

        return kvEntries;
    }

    /**
     * @param minimumFreq use 0 for no min bound
     * @param maximumFreq use Integer.MAX_VALUE for no max bound
     * @return populate array with only those Entries which are bounded by min and max
     */
    public Entry<Integer, K>[] getFrequencyKeyEntries(final int minimumFreq, final int maximumFreq) {
        final Entry<Integer, K>[] entries = getFrequencyKeyEntries();

        int count = entries.length;
        for (int i = 0; i < entries.length; i++) {
            final int freq = entries[i].getKey();
            if (freq < minimumFreq || freq > maximumFreq) {
                entries[i] = null;
                count--;
            }
        }

        if (count != entries.length) {   // some reduction
            @SuppressWarnings("unchecked") final Entry<Integer, K>[] filterEntries = Util.createArray(Entry.class, count);

            int i = 0;
            for (final Entry<Integer, K> entry : entries) {
                if (entry != null) {
                    filterEntries[i] = entry;
                    i++;
                }
            }

            return filterEntries;
        } else {   // all passed
            return entries;
        }
    }

    /**
     * @return array of reversed count-key Entries in ascending frequencies order
     */
    public Entry<Integer, K>[] getFrequencyKeyEntries() {
        return getFrequencyKeyEntries(false);
    }

    /**
     * @param isDescendingOrder if 'false' in order of ascending frequencies else in descending order
     * @return array of reversed count-key Entries in sort order
     */
    public Entry<Integer, K>[] getFrequencyKeyEntries(final boolean isDescendingOrder) {
        @SuppressWarnings("unchecked") final Entry<Integer, K>[] vkEntries = Util.createArray(Entry.class, fKey_to_IntegerMap.size());

        if (vkEntries.length > 0) {
            int i = 0;
            for (final Entry<K, Integer> kvEntry : fKey_to_IntegerMap.entrySet()) {
                final Entry<Integer, K> vkEntry = new IdentityEntry<Integer, K>(kvEntry.getValue(), kvEntry.getKey());
                vkEntries[i] = vkEntry;
                i++;
            }

            // para-lambda for ascending Map.Entry Integer compares
            // non-static because of Generics
            final Comparator<Entry<Integer, K>> entryComparator = new Comparator<Entry<Integer, K>>() // anonymous class
            {
                @Override
                public int compare(Entry<Integer, K> o1, Entry<Integer, K> o2) {
                    if (o1 == o2) return 0; // equal obj refs
                    final int i1 = o1.getKey().intValue();
                    final int i2 = o2.getKey().intValue();
                    return (i1 < i2) ? -1
                            : (i1 > i2) ? 1
                            : 0;
                }
            };
            Arrays.sort(vkEntries, entryComparator);

            if (isDescendingOrder) {   // needs descending order
                Util.reverseOrderInPlace(vkEntries);
            }
        }
        return vkEntries;
    }

    /**
     * @return new-line separated keys-counts in ascending frequency order
     */
    public String report() {
        final StringBuilder sb = new StringBuilder();
        for (final Entry<Integer, K> vkEntry : getFrequencyKeyEntries()) {   // reverse order
            sb.append(vkEntry.getValue()).append('=').append(vkEntry.getKey()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Most simple static factory.
     *
     * @param <E>      will be inferred
     * @param elements instances to produce a histogram of
     * @return
     */
    static public <E> Histogram2<E> create(final E[] elements) {
        final Histogram2<E> histogram = new Histogram2<E>(Util.getElementalClass(elements));

        for (final E element : elements) {
            histogram.increment(element);
        }

        return histogram;
    }

    /**
     * Simple static factory.
     *
     * @param <E>         will be inferred
     * @param ofClassType for reified key array, required because of java generic type erasure
     * @param iterable    as an iterator, probably from a Collection
     * @return
     */
    static public <E> Histogram2<E> create(final Class<E> ofClassType, final Iterable<E> iterable) {
        final Histogram2<E> histogram = new Histogram2<E>(ofClassType);

        for (final E element : iterable) {
            histogram.increment(element);
        }

        return histogram;
    }

    /**
     * Transformational static factory method.
     *
     * @param <I>               inputs of class type will be inferred
     * @param <O>               outputs of class type will be inferred
     * @param transformer       lambda expression that converts an input to an output
     * @param outputOfClassType for reified key array, required because of java generic type erasure
     * @param inputs            as an array
     * @return
     */
    static public <I, O> Histogram2<O> create
    (final Transformable<I, O> transformer
            , final Class<O> outputOfClassType
            , final I[] inputs
    ) {
        final Histogram2<O> histogram = new Histogram2<O>(outputOfClassType);

        for (final I input : inputs) {
            final O output = transformer.transform(input);
            if (output != null) {
                histogram.increment(output);
            }
        }

        return histogram;
    }

    /**
     * Transformational static factory method.
     *
     * @param <I>               inputs of class type will be inferred
     * @param <O>               outputs of class type will be inferred
     * @param transformer       lambda expression that converts an input to an output
     * @param outputOfClassType for reified key array, required because of java generic type erasure
     * @param inputs            as an iterator, probably from a Collection
     * @return
     */
    static public <I, O> Histogram2<O> create
    (final Transformable<I, O> transformer
            , final Class<O> outputOfClassType
            , final Iterable<I> inputs
    ) {
        final Histogram2<O> histogram = new Histogram2<O>(outputOfClassType);

        for (final I input : inputs) {
            final O output = transformer.transform(input);
            if (output != null) {
                histogram.increment(output);
            }
        }

        return histogram;
    }


    // ================================================================================

    /**
     * Concretization of the Map.Entry interface where
     * comparison of the Key is an identity check.
     *
     * @author sgbaber
     */
    static private class IdentityEntry<K, V>
            implements Map.Entry<K, V> {
        static // initializer block
        {
        }

        final K fKey;
        final V fValue;

        private IdentityEntry(final K key, final V value) {
            if (key == null) throw new NullPointerException();

            fKey = key;
            fValue = value;
        }

        @Override
        public K getKey() {
            return fKey;
        }

        @Override
        public V getValue() {
            return fValue;
        }

        /**
         * Method required by interface.
         *
         * @param value
         * @return an exception
         */
        @Deprecated
        @Override
        public V setValue(V value) {
            throw new IllegalArgumentException();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof IdentityEntry<?, ?>) {
                return this.fKey.equals(obj);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return fKey.hashCode();
        }
    }
}
