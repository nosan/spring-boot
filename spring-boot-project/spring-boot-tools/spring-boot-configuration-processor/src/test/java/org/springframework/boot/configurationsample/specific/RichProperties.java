/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationsample.specific;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.boot.configurationsample.ConfigurationProperties;
import org.springframework.boot.configurationsample.NestedConfigurationProperty;

/**
 * Demonstrates that metadata generates for rich {@link Map} and {@link Collection} types.
 *
 * @param <T> any type
 * @author Dmytro Nosan
 */
@SuppressWarnings("rawtypes")
@ConfigurationProperties(prefix = "config")
public class RichProperties<T> {

	private final CustomList customList = new CustomList();

	private final CustomMap customMap = new CustomMap();

	@NestedConfigurationProperty
	private final List rawList = new ArrayList<>();

	@NestedConfigurationProperty
	private final List<?> listOfWildcard = new ArrayList<>();

	@NestedConfigurationProperty
	private final List<List<Person>> listOfList = new ArrayList<>();

	@NestedConfigurationProperty
	private final List<Map<String, Person>> listOfMap = new ArrayList<>();

	@NestedConfigurationProperty
	private final List<Person> list = new ArrayList<>();

	@NestedConfigurationProperty
	private final List<T> listOfUnresolvedGeneric = new ArrayList<>();

	@NestedConfigurationProperty
	private final Map rawMap = new LinkedHashMap<>();

	@NestedConfigurationProperty
	private final Map<String, ?> mapOfWildcard = new LinkedHashMap<>();

	@NestedConfigurationProperty
	private final Map<String, List<Person>> mapOfList = new LinkedHashMap<>();

	@NestedConfigurationProperty
	private final Map<String, Map<String, Person>> mapOfMap = new LinkedHashMap<>();

	@NestedConfigurationProperty
	private final Map<String, Person> map = new LinkedHashMap<>();

	@NestedConfigurationProperty
	private final Map<String, T> mapOfUnresolvedGeneric = new LinkedHashMap<>();

	public Map<String, Person> getMap() {
		return this.map;
	}

	public Map getRawMap() {
		return this.rawMap;
	}

	public List<Person> getList() {
		return this.list;
	}

	public List getRawList() {
		return this.rawList;
	}

	public List<T> getListOfUnresolvedGeneric() {
		return this.listOfUnresolvedGeneric;
	}

	public List<?> getListOfWildcard() {
		return this.listOfWildcard;
	}

	public List<List<Person>> getListOfList() {
		return this.listOfList;
	}

	public List<Map<String, Person>> getListOfMap() {
		return this.listOfMap;
	}

	public Map<String, T> getMapOfUnresolvedGeneric() {
		return this.mapOfUnresolvedGeneric;
	}

	public Map<String, ?> getMapOfWildcard() {
		return this.mapOfWildcard;
	}

	public Map<String, Map<String, Person>> getMapOfMap() {
		return this.mapOfMap;
	}

	public Map<String, List<Person>> getMapOfList() {
		return this.mapOfList;
	}

	public CustomMap getCustomMap() {
		return this.customMap;
	}

	public CustomList getCustomList() {
		return this.customList;
	}

	public static class Person {

		private String name;

		@NestedConfigurationProperty
		private final List<Address> addresses = new ArrayList<>();

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<Address> getAddresses() {
			return this.addresses;
		}

	}

	public static class Address {

		private String street;

		public String getStreet() {
			return this.street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

	}

	public static class CustomMap extends LinkedHashMap<String, Person> {

	}

	public static class CustomList extends LinkedHashSet<Person> {

	}

}
