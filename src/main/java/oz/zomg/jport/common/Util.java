package oz.zomg.jport.common;

import oz.zomg.jport.common.Interfacing_.Targetable;

import java.io.*;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;


/**
 * General utilities.
 * Some are a subset of original source.
 *
 * @author <SMALL>Copyright 2012 by Stephen Baber
 * &nbsp; <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * <img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by-sa/3.0/80x15.png" /></a><br />
 * This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/3.0/deed.en_US">
 * Creative Commons Attribution-ShareAlike 3.0 Unported License</a>.</SMALL>
 */
public class Util {
    static final public byte[] NO_BYTES = new byte[0];

    static final public int INVALID_INDEX = -1;

    private Util() {
    }

    static public boolean isOnMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    static public boolean isOnWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("win");
    }

    /**
     * Sleep the current Thread and ignore the checked Exception.
     *
     * @param millisec up to, but no more than, this duration to sleep
     * @return 'false' if was interrupted (the interrupt flag is also restored)
     */
    static public boolean sleep(final int millisec) {
        try {
            Thread.sleep(millisec);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); // restore the flag
            return false;
        }
    }

    /**
     * Linear Search an array for an identity with '==' instead of
     * using Arrays.binarySearch() when the array can not be sort ordered
     * or when less than approx. 10 elements avoiding the overhead of
     * searching a sparsely populated IdentityHashMap.
     *
     * @param <E>            will be inferred
     * @param searchReferent to locate in the array.  Can be 'null'
     * @param inArray        will -not- be altered!  Note: to avoid some compiler warnings in client code, the type needs to be 'Object[]' not {...}
     * @return index -1 if not found in the [], otherwise a ZERO-based index of the -FIRST- occurrence
     */
    static public <E> int indexOfIdentity(final E searchReferent, final E[] inArray) {
        switch (inArray.length) {
            case 0:
                return INVALID_INDEX;

            case 1:
                return (searchReferent == inArray[0]) ? 0 : INVALID_INDEX; // 0=found index

            default:
                int i = 0;
                for (final E obj : inArray) {   // linear search faster than indentity hashing until more than ~10 elements
                    if (searchReferent == obj) return i; // found
                    i += 1;
                }
                return INVALID_INDEX;
        }
    }

    //ENHANCE

    /**
     * Linear identity search specialization for Weak, Soft, or Phantom Reference arrays.
     *
     * @param <E>            will be inferred
     * @param searchReferent to locate in the array.  Can be 'null'
     * @param inRefArray     will -not- be altered!
     * @return
     */
    static private <E> int indexOfIdentity(final E searchReferent, final Reference<E>[] inRefArray) {
        switch (inRefArray.length) {
            case 0:
                return INVALID_INDEX;

            case 1:
                return (searchReferent == inRefArray[0].get()) ? 0 : INVALID_INDEX; // 0=found index

            default:
                int i = 0;
                for (final Reference<E> ref : inRefArray) {   // linear search faster than indentity hashing until more than ~10 elements
                    if (searchReferent == ref.get()) return i; // found
                    i += 1;
                }
                return INVALID_INDEX;
        }
    }

    /**
     * Linear Search an array for an equality with .equals() instead of
     * using Arrays.binarySearch() when the array can not be sort ordered.
     *
     * @param <E>     will be inferred
     * @param obj     to locate in the array. Can be 'null'
     * @param inArray will -not- be altered!  Note: to avoid compiler warnings in client code, the type needs to be 'Object[]' not {...}
     * @return index -1 if not found in the [], otherwise a ZERO-based index of the -FIRST- occurrence
     */
    static public <E> int indexOf(final E obj, final E[] inArray) {
        if (obj == null) return indexOfNull(inArray);

        switch (inArray.length) {
            case 0:
                return INVALID_INDEX;

            case 1:
                return (obj.equals(inArray[0])) ? 0 : INVALID_INDEX; // 0=found index

            default:
                int i = 0;
                for (final E element : inArray) {   // linear search faster than hashing until more than ~20 elements
                    if (obj.equals(element)) return i; // found
                    i += 1;
                }
                return INVALID_INDEX;
        }
    }

    /**
     * Where is null?
     *
     * @param <E>     will be inferred
     * @param inArray will -not- be altered!
     * @return -1 if there is no 'null' reference in array else the ZERO-based index of the -FIRST- 'null' occurrence
     */
    static public <E> int indexOfNull(final E[] inArray) {
        switch (inArray.length) {
            case 0:
                return INVALID_INDEX;

            case 1:
                return (inArray[0] == null) ? 0 : INVALID_INDEX; // 0=found index

            default:
                int i = 0;
                for (final E element : inArray) {
                    if (element == null) return i; // found
                    i += 1;
                }
                return INVALID_INDEX;
        }
    }

    /**
     * Reverse the array index order of the object refs in place.
     *
     * @param objs within same array is reverse ordered, no new allocation will occur
     */
    static public void reverseOrderInPlace(final Object[] objs) {
        final int length = objs.length;
        if (length <= 1) return; // 0 and 1 are guaranteed palindromes

        for (int top = 0, end = length - 1; top < end; top++, end--) {   // exchange the first and last
            final Object element = objs[top];
            objs[top] = objs[end];
            objs[end] = element;
        }
    }

    //BUG FIX middle element missing, doesn't effect in-place because middle remains put

    /**
     * Reverse index order the object references.
     * Employs optimizations that may result in the originating source [].
     *
     * @param <E>      will be inferred
     * @param elements will -not- be altered!  Zero lengths are okay.
     * @return reversal not an in-place operation, i.e. a new reified [] is allocated if length > 1
     */
    static public <E> E[] reverseOrder(final E[] elements) {
        final int length = elements.length;
        switch (length) {
            case 0: // length 0 and 1 are guaranteed palindromes
            case 1:
                return elements;

            default: {
                @SuppressWarnings("unchecked") final E[] revs = (E[]) Array.newInstance(getElementalClass(elements), length);
                for (int start = 0, end = length - 1; start < length; start++, end--) {
                    revs[start] = elements[end];
                }
                return revs;
            }

        }
    }

    /**
     * From a reified array, extract the []'s elemental class type.
     * Can be a primitive type such as byte.class, an array such as int[] when 2D int[][],
     * or object references like Byte.class or SomeClass.class.
     *
     * @param <E>        will be inferred
     * @param typedArray
     * @return the particular Class type of all elements in the reified array
     */
    @SuppressWarnings("unchecked")
    static public <E> Class<E> getElementalClass(final E[] typedArray) {
        return (Class<E>) (typedArray.getClass().getComponentType()); // BTW- not an java.awt.Component
    }

    /**
     * A single element array of the desired super type.
     * Special cases Object[] as seen in the JDK source.
     * Avoids generic array creation compiler errors of <CODE>new T[]{ obj }</CODE> has
     * to be <CODE>(T[])new Object[]{ obj }</CODE> and "unchecked" warnings suppressed.
     *
     * @param <S>                         super type of T
     * @param <T>                         will be inferred
     * @param withElementOfSuperClassType can be more base than the derived element's type
     * @param fromSingleElement
     * @return single element one-dimensional array of a reified type
     */
    static private <S, T extends S> S[] createArrayWrapper(final Class<S> withElementOfSuperClassType, final T fromSingleElement) {
        @SuppressWarnings("unchecked") final S[] array = (withElementOfSuperClassType != Object.class)
                ? (S[]) Array.newInstance(withElementOfSuperClassType, 1)
                : (S[]) new Object[1];
        array[0] = fromSingleElement;
        return array;
    }

    /**
     * @param <T>
     * @param withElementsOfClassType
     * @param length
     * @return
     */
    @SuppressWarnings("unchecked")
    static public <T> T[] createArray(final Class<T> withElementsOfClassType, final int length) {
        return (length == 0)
                ? EmptyArrayFactory_.get(withElementsOfClassType)
                : (withElementsOfClassType != Object.class)
                ? (T[]) Array.newInstance(withElementsOfClassType, length)
                : (T[]) new Object[length]; // optimization present in java.lang.Arrays.copyOf().  Perhaps makes System.arrayCopy() work faster?
    }

    /**
     * Programmatically create a reified array from a Generic Collection.
     * Special cased to cache EMPTY[0]{} arrays by Class type via EmptyArrayFactory.
     * This could be a feature of certain JVMs but we guarantee it here.
     * <p>
     * Implement .toArray() for Set<T> or List<T> without all the drama of an empty allocation with new <T>[0] or new <T>[n]
     *
     * @param <S>                         super type of T
     * @param <T>                         will be inferred
     * @param withElementOfSuperClassType required for Array.newInstance() as Java Type Erasure means Generics are just Object refs, sort of
     * @param theEntireCollection         is a List, Set, or Queue (but not an Iterable as no .size() method)
     * @return new array with a copy of the collection's elements
     */
    @SuppressWarnings("unchecked")
    static public <S, T extends S> S[] createArray(final Class<S> withElementOfSuperClassType, final Collection<T> theEntireCollection) {
        final int length = theEntireCollection.size();
        if (length == 0) { // caches an empty immutable array to avoid a common allocation scenario
            return EmptyArrayFactory_.get(withElementOfSuperClassType);

            //... case 1 : not available because no easy way to get a single element out without an Iterator allocation
        }
        if (withElementOfSuperClassType != Object.class) {
            final S[] elements = (S[]) Array.newInstance(withElementOfSuperClassType, length);
            theEntireCollection.toArray(elements);
            return elements;
        } else {   // plain-old type-erasured Object[]
            return (S[]) theEntireCollection.toArray();
        }
    }

    //ENHANCE

    /**
     * Convenience factory.
     *
     * @param <K>     keys of class type
     * @param <V>     values of class type
     * @param fromMap
     * @return
     */
    @SuppressWarnings("unchecked")
    static public <K, V> Map.Entry<K, V>[] createMapEntryArray(final Map<K, V> fromMap) {
        final int size = fromMap.size();
        if (size == 0) {
            return EmptyArrayFactory_.get(Map.Entry.class);
        } else if (size == 1) {
            return new Map.Entry[]{fromMap.entrySet().iterator().next()};
        }
        final Map.Entry<K, V>[] entries = new Map.Entry[size]; // nulls
        return fromMap.entrySet().toArray(entries);
    }

    //ENHANCE -> ObjUtil

    /**
     * Reflection based, non-static field values dump for debug.
     * Handles arrays also.
     *
     * @param instance
     * @param needStaticOnly 'true' for static fields only, 'false' for non-static fields only
     * @return is [CR] separated
     */
    static public String dumpFields(final Object instance, final boolean needStaticOnly) {
        final StringBuilder sb = new StringBuilder();

        Class<?> ofClassType = instance.getClass();
        do {   // 'do-while' allows at least "Object" if a POJO
            final Field[] fields = ofClassType.getDeclaredFields();
            // did not want static fields
            // else is a static field like NONE or EMPTY
            Arrays.stream(fields).filter(field -> Modifier.isStatic(field.getModifiers()) == needStaticOnly).forEach(field -> {
                try {
                    field.setAccessible(true); // otherwise throws IllegalAccessException
                    final Object value = field.get(instance);
                    if (value != null) {
                        final String valueString = (!value.getClass().isArray())
                                ? value.toString()
                                : Arrays.toString((Object[]) value);

                        if (!valueString.isEmpty()
                                && !"[]".equals(valueString)
                                && !"0".equals(valueString)
                                && !"0.0".equals(valueString)
                        ) {   // not empty or "[]" or "0" or "0.0" or 'null'
                            final String fieldName = field.getName();
                            sb.append(fieldName).append("\t ").append(valueString).append('\n');
                        }
                        // else ignore default fields
                    }
                    // else ignore uninitialized fields
                } catch (IllegalArgumentException | IllegalAccessException ignored) {
                }
            });

            ofClassType = ofClassType.getSuperclass();
        }
        while (!Object.class.equals(ofClassType) && ofClassType != null);

        return sb.toString();
    }

    //ENHANCE

    /**
     * <CODE><PRE>
     * // note: pre jdk8 lambda expression syntax will make for lame-duhs
     * Util.withEach( new Targetable<Component>() { @Override public void target( Component obj ) { obj.setFocusable( false ); } }
     * , ab_Sync
     * , ab_MarkOutdated
     * , ab_ApplyMarks
     * , ab_MoreCommand
     * );
     * <p>
     * // better then above anonymous class creation but still irreducibly awkward
     * for( final Component component : new Component[]
     * { ab_Sync
     * , ab_MarkOutdated
     * , ab_ApplyMarks
     * , ab_MoreCommand
     * }
     * ) { component.setFocusable( false ); }
     * </PRE></CODE>
     *
     * @param <T>      targetee of class type
     * @param targetor lambda expression
     * @param objs
     */
    @SafeVarargs
    static private <T> void withEach(final Targetable<T> targetor, final T... objs) {
        Arrays.stream(objs).forEach(targetor::target);
    }

    //ENHANCE

    /**
     * @param <T>      targetee of class type
     * @param targetor lambda expression
     * @param iterator collection
     */
    static private <T> void withEach(final Targetable<T> targetor, final Iterable<T> iterator) {
        for (final T obj : iterator) {
            targetor.target(obj);
        }
    }

    //ENHANCE CollectionUtil

    /**
     * Inverses a mapping of unique keys to values into unique values to sets of keys.
     *
     * @param <K>                          will be swapped to a Value class type
     * @param <V>                          will be swapped to a Key class type
     * @param inverseMapNeedsOrderedKeys   'true' requires Comparable elements
     * @param inverseMapNeedsOrderedValues 'true' requires Comparable elements
     * @param fromKeyValueMap              map to be inverted
     *                                     //* @param valuesIterate the value is Iterable Needed because type erasure of <CODE>Map<K,V></CODE> clashes with <CODE>Map<K,? extends Collection<V>></CODE>
     * @return an inverse mapping where Values are now mapped to potentially multiple Keys
     */
    static public <K, V>
    Map<V, Set<K>> createInverseMultiMapping
    (final boolean inverseMapNeedsOrderedKeys
            , final boolean inverseMapNeedsOrderedValues
            , final Map<K, V> fromKeyValueMap
//...            , final boolean valuesIterate
    ) {
        final Map<V, Set<K>> invMap = (inverseMapNeedsOrderedKeys)
                ? new TreeMap<>()
                : new HashMap<>();

        // invert keys - values
        // alias
        // alias
        fromKeyValueMap.forEach((invValue, invKey) -> {
            if (invKey != null) {   // values maybe 'null' but keys can not be
                if (!invMap.containsKey(invKey)) {   // a singleton element is always ordered
                    final Set<K> set = Collections.singleton(invValue);
                    invMap.put(invKey, set);
                } else {   // seen the inverse key before
                    final Set<K> set = invMap.get(invKey);
                    if (set.size() > 1) {   // set already bigger
                        set.add(invValue);
                    } else {   // copy to a bigger, non-singleton set
                        final Set<K> biggerSet = (inverseMapNeedsOrderedValues)
                                ? new TreeSet<>(set)
                                : new HashSet<>(set);
                        biggerSet.add(invValue);
                        invMap.put(invKey, biggerSet);
                    }
                }
            }
        });

        // replacing 'null' value Sets with Collections.emptySet() does not have to be done because 'null' keys are prohibited
        return invMap;
    }

    //ENHANCE CollectionsUtil

    /**
     * A specialized Inverse Mapping that takes Enum keyed Collections
     * of Values into Value Keyed associated Enum Sets.
     *
     * @param <K>                          will be swapped to a Value class type
     * @param <V>                          will be swapped to a Key class type
     * @param valuesOfClassType
     * @param fromEnumKey_to_MultiValueMap map to be inverted
     * @return an inverse mapping where Values are now mapped to potentially multiple Keys
     */
    static private <K extends Enum<K>, V>
    Map<V, EnumSet<K>> createInverseMultiMapping
    (final Class<V> valuesOfClassType
            , final Map<K, ? extends Collection<V>> fromEnumKey_to_MultiValueMap
    ) {
        final boolean isValueComparable = Comparable.class.isAssignableFrom(valuesOfClassType);
        final Map<V, EnumSet<K>> invMap = (isValueComparable)
                ? new TreeMap<>()
                : new HashMap<>();

        // invert keys - values
        // alias
        // alias
        fromEnumKey_to_MultiValueMap.forEach((invValue, invKeyCollection) -> {
            if (invKeyCollection != null) {   // values maybe 'null' but keys can not be
                for (final V invKey : invKeyCollection) {
                    if (invKey != null) {
                        if (!invMap.containsKey(invKey)) {   // a singleton element is always ordered
                            final EnumSet<K> set = EnumSet.of(invValue);
                            invMap.put(invKey, set);
                        } else {   // seen the inverse key before
                            final EnumSet<K> set = invMap.get(invKey);
                            set.add(invValue);
                        }
                    }
                }
            }
        });

        // replacing 'null' value Sets with Collections.emptySet() does not have to be done because 'null' keys are prohibited
        return invMap;
    }

    /**
     * Performs a complete read of an existing File.
     * Bringing in very large files may require a large starting JVM heap.
     *
     * @param filePath whose contents are less than 2 gigabytes in logical size else IllegalArgumentException
     * @return all content bytes for the file
     * @throws IOException is typically FileNotFoundException
     */
    static public byte[] retrieveFileBytes(final File filePath) throws IOException {
        final long size = filePath.length(); // file should not truncate before operation complete
        if (size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("File contents too large to fit into a Java6 array " + filePath.getAbsolutePath()); // Doh!

        final byte[] bytes = new byte[(int) size];
        final FileInputStream fis = new FileInputStream(filePath);
        final DataInputStream dis = new DataInputStream(fis);

        dis.readFully(bytes);

        dis.close();
        fis.close();
        return bytes;
    }

    /**
     * Fully drain available stream to a byte[].
     *
     * @param localInputStream is not closed here
     * @return populated by InputStream read available
     * @throws IOException
     */
    static public byte[] readFullyBytes(final InputStream localInputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(0); // 0 as most likely will be copying 100% in one shot if stream is local
        final DataInputStream dis = new DataInputStream(localInputStream); // decorated as InputStream does not have a .readFully()

        byte[] inBytes = NO_BYTES; // establish length=0
        int avail;
        while ((avail = localInputStream.available()) > 0) // without blocking, check if any unread bytes remain
        {
            if (inBytes.length != avail) inBytes = new byte[avail];
            dis.readFully(inBytes); // decorated as InputStream does not have a .readFully()
            baos.write(inBytes); // copy bytes
        }

        dis.close();
        final byte[] outBytes = baos.toByteArray();
        baos.close();
        return outBytes;
    }

    //ENHANCE

    /**
     * Read complete byte[] content of a .JAR resource or file entity on the Class Path.
     * i.e. path is in distro jar or the build class path.
     * Note: ClassLoader understands that the beginning slash path == `pwd` of
     * the .JAR not the root of the file system.
     *
     * @param internallyJarredResourceName should be prefixed with '/' to load from this jar / class files
     * @return
     * @throws IOException
     */
    static public byte[] retrieveResourceBytes(final String internallyJarredResourceName) throws IOException {
        final InputStream resourceInputStream = Util.class.getResourceAsStream(internallyJarredResourceName);
        if (resourceInputStream == null)
            throw new FileNotFoundException("ClassLoader can not find " + internallyJarredResourceName);
        final byte[] bytes = readFullyBytes(resourceInputStream);
        resourceInputStream.close();
        return bytes;
    }

    //ENHANCE -> IoFileUtil parameter

    /**
     * Just close the file, connection, stream, etc. without IOException.
     *
     * @param closeable
     * @return success 'true' if closed without IOException thrown
     */
    static public boolean close(final Closeable closeable) {
        if (closeable == null) return false; // nothing to do

        try {
            closeable.close();
            return true;
        } catch (IOException ex) {   // not expected
            return false;
        }
    }
}
