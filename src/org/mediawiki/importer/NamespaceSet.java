/*
 * MediaWiki import/export processing tools
 * Copyright 2005 by Brion Vibber
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * $Id$
 */

package org.mediawiki.importer;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

public class NamespaceSet {
	Map<String, Namespace> byname;
	Map<Integer, Namespace> bynumber;
	
	public NamespaceSet() {
		byname = new HashMap<String, Namespace>();
		bynumber = new LinkedHashMap<Integer, Namespace>();
	}
	
	public void add(int index, String prefix, String nscase) {
		add(new Integer(index), prefix, nscase);
	}
	
	public void add(Integer index, String prefix, String nscase) {
		byname.put(prefix, new Namespace(index, prefix, nscase));
		bynumber.put(index, new Namespace(index, prefix, nscase));
	}
	
	public boolean hasPrefix(String prefix) {
		return byname.containsKey(prefix);
	}
	
	public boolean hasIndex(Integer index) {
		return bynumber.containsKey(index);
	}
	
	public String getPrefix(Integer index) {
		return bynumber.get(index).Prefix;
	}
	
	public Integer getIndex(String prefix) {
		return byname.get(prefix).Index;
	}
	
	public String getColonPrefix(Integer index) {
		String prefix = getPrefix(index);
		if (index.intValue() != 0)
			return prefix.concat(":");
		return prefix;
	}
	
	public Iterator<Map.Entry<Integer, Namespace>>  orderedEntries() {
		return bynumber.entrySet().iterator();
	}
}
