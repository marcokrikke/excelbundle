/*
 * Copyright 2008 Rob Juurlink
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package senselogic.excelbundle;

import java.util.*;


/**
 * Extends java.util.Properties, but preserves the sequence of the inserted key-values
 *
 * @author Rob Juurlink
 */
public class CustomProperties extends Properties {

	private List<Object> sequence = new ArrayList<Object>();


	@Override
	public Object put(Object key, Object value) {

		Object lObject = super.put(key, value);

		if (!sequence.contains(key)) {
			sequence.add(key);
		}
		return lObject;
	}


	@Override
	public Object remove(Object key) {

		Object lObject = super.remove(key);

		sequence.remove(key);

		return lObject;
	}


	/**
	 * An entryset containing an iterator that respects the sequence
	 */
	@Override
	public Set<Map.Entry<Object, Object>> entrySet() {

		return new AbstractSet<Map.Entry<Object, Object>>() {

			public Iterator<Map.Entry<Object, Object>> iterator() {

				return new Iterator<Map.Entry<Object, Object>>() {

					// Start vooraan
					private int index = 0;

					public boolean hasNext() {
						return index < sequence.size();
					}

					public Map.Entry<Object, Object> next() {

						Object lKey = sequence.get(index);
						Object lValue = get(lKey);

						Map.Entry<Object, Object> lEntry = new CustomMapEntry(lKey, lValue);
						index++;
						return lEntry;
					}

					public void remove() {
						throw new Error("Not implemented");
					}
				};
			}

			public int size() {
				return sequence.size();
			}
		};
	}


	/**
	 * Simple implementation of Map.Entry
	 */
	private class CustomMapEntry implements Map.Entry<Object, Object> {

		private Object key;
		private Object value;

		public CustomMapEntry(Object pKey, Object pValue) {
			key = pKey;
			value = pValue;
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}

		public Object setValue(Object value) {
			throw new Error("Not implemented");
		}

		public String toString() {
			return key + ":" + value;
		}
	}


	/**
	 * Testing
	 */
	public static void main(String[] args) throws Exception {

		Properties lproperties = new CustomProperties();

		lproperties.put("eerste.key", "De eerste key");
		lproperties.put("tweede.key", "De tweede key");
		lproperties.put("derde.key", "De derde key");
		lproperties.put("vierde.key", "De vierde key");
		lproperties.put("vijfde.key", "De vijfde key");

		for (Map.Entry<Object, Object> lEntry : lproperties.entrySet()) {
			System.out.println(lEntry);
		}
	}
}

