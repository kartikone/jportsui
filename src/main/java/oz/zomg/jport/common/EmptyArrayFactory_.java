package oz.zomg.jport.common;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Aggregates pre-allocated empty arrays.  One dimensional arrays are type strict and length immutable.
 * This common allocation scenario happens surprisingly often and is not built into the
 * JVM/ClassLoader as of Sun/IcedTea Java 1.6.23.
 * <p>
 * Though this could be a feature of certain or future JVMs, we have put it into guaranteed effect here.
 * <H3><I><FONT color="#770000">Subset of original source.</FONT></I></H3>
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class EmptyArrayFactory_ {
    static final public Object[] NO_OBJECTS = new Object[0];

    /**
     * Class as a Map key is a good candidate for using an IdentityHashMap but the collection will
     * be modified by external threads, so using ConcurrentHashMap to be thread safe and avoiding
     * synchronization blocks since typically this is an often-read but rarely-mutated Collection.
     *
     * @see java.util.IdentityHashMap
     * @see java.util.concurrent.ConcurrentHashMap
     */
    static final private Map<Class<?>, Object> _CLASS_TYPE_TO_EMPTY_ARRAY_MAP = new ConcurrentHashMap<Class<?>, Object>(); // Value has to be Object though array is, in fact, reified.

    static // initializer block
    {
        _CLASS_TYPE_TO_EMPTY_ARRAY_MAP.put(Object.class, NO_OBJECTS);
    }

    private EmptyArrayFactory_() {
    } // factory utility

    /**
     * Static cached factory of empty arrays.
     * Retrieves an empty one-dimensional reified array reference of the correct
     * Type or lazy instantiates an immutable []{} if not presently available.
     * One dimensional []s are type strict and length immutable.
     * A zero length array is fully immutable and can not be altered once created.
     * <p>
     * Performance Note: This call is approximately 3x..4x slower than a normal empty array creation
     * when the scope is limited to the stack -HOWEVER- it is 2x..3x faster than allocating when scope
     * duration requires heap space by putting ZERO additional loading on the heap Garbage Collector.
     *
     * @param <T>                         compile time checked cast
     * @param arrayElementsAreOfClassType as "Thing.class", do not use "Thing[].class" unless you want a 2D array "Thing[][]"
     * @return reified array as TheClassType[0] {} where length=0
     */
    @SuppressWarnings("unchecked")
    static public <T> T[] get(final Class<T> arrayElementsAreOfClassType) {
        T[] emptyArray = (T[]) _CLASS_TYPE_TO_EMPTY_ARRAY_MAP.get(arrayElementsAreOfClassType);
        if (emptyArray == null) // not paying the 'synchonize' tax because, worse case, we would occasionally replace the value with a fresh []{}
        {   // null indicates []{} was not cached
            emptyArray = (T[]) Array.newInstance(arrayElementsAreOfClassType, 0); // reified but method gives an Object, not T[] or even Object[], doh!
            _CLASS_TYPE_TO_EMPTY_ARRAY_MAP.put(arrayElementsAreOfClassType, emptyArray);
        }
        return emptyArray;
    }
}
