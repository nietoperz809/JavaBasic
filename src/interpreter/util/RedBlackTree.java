/*
 * @(#)RedBlackTree.java	1.1 95/09/14
 *
 * Copyright (c) 1996 Chuck McManis, All Rights Reserved.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * CHUCK MCMANIS MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY
 * OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. CHUCK MCMANIS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING,
 * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */
package interpreter.util;

import java.util.*;

public class RedBlackTree <K,V> extends TreeMap<K,V>
{
    /**
     * Return an enumeration of the trees objects.
     * @return 
     */
    public Enumeration<Map.Entry<K, V>> elements()
    {
        return Collections.enumeration(entrySet());
    }

    /**
     * Return the successor object to the named object.
     * @param key
     * @return 
     */
    public V next(K key)
    {
        Map.Entry<K, V> e = higherEntry(key);
        if (e == null)
            return null;
        return e.getValue();
    }
}


