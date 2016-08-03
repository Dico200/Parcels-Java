 /*
  * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
  * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
  *  * This code is free software; you can redistribute it and/or modify it
  * under the terms of the GNU General Public License version 2 only, as
  * published by the Free Software Foundation.  Oracle designates this
  * particular file as subject to the "Classpath" exception as provided
  * by Oracle in the LICENSE file that accompanied this code.
  *
  * This code is distributed in the hope that it will be useful, but WITHOUT
  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
  * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
  * version 2 for more details (a copy is included in the LICENSE file that
  * accompanied this code).
  *
  * You should have received a copy of the GNU General Public License version
  * 2 along with this work; if not, write to the Free Software Foundation,
  * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
  *
  * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
  * or visit www.oracle.com if you need additional information or have any
  * questions.
  */
package com.redstoner.utils;

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class Optional<T> implements Serializable {
	private static final long serialVersionUID = -6967900704910277798L;
	
	//Slight differences with java.util.Optional: ifPresentOrElse and ifNotPresent.

	private static final Optional<?> EMPTY = new Optional<>(null);
	
	@SuppressWarnings("unchecked")
	public static <T> Optional<T> empty() {
		return (Optional<T>) EMPTY;
	}
	
	public static <T> Optional<T> of(T value) {
		if (value == null)
			throw new NullPointerException("value cannot be null");
		return new Optional<>(value);
	}
	
	public static <T> Optional<T> ofNullable(T value) {
		return value == null ? empty() : new Optional<>(value);
	}
	
	private T value;
	
	private Optional(T value) {
		this.value = value;
	}
	
	public T get() {
		if (value == null)
			throw new NoSuchElementException("value not present");
		return value;
	}
	
	public boolean isPresent() {
		return value != null;
	}
	
	public Optional<T> ifPresent(Consumer<? super T> present) {
		if (isPresent())
			present.accept(value);
		return this;
	}
	
	public Optional<T> ifPresent(Runnable toRun) {
		if (isPresent())
			toRun.run();
		return this;
	}
	
	public Optional<T> ifPresentOrElse(Consumer<? super T> present, Runnable orElse) {
		if (isPresent())
			present.accept(value);
		else
			orElse.run();
		return this;
	}
	
	public Optional<T> ifNotPresent(Runnable orElse) {
		if (!isPresent())
			orElse.run();
		return this;
	}
	
	public T orElse(T other) {
		return isPresent()? value : other;
	}
	
	public T orElseGet(Optional<? extends T> other) {
		return isPresent()? value : other.get();
	}
	
	public <U extends Throwable> T orElseThrow(U throwable) throws U {
		if (isPresent())
			return value;
		throw throwable;
	}
	
	public Optional<T> filter(Predicate<? super T> tester) {
		if (tester == null)
			throw new NullPointerException();
		if (isPresent())
			return tester.test(value)? this : empty();
		return this;
	}
	
	public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
		if (mapper == null)
			throw new NullPointerException();
		if (isPresent())
			return Optional.ofNullable(mapper.apply(value));
		return empty();
	}
	
	public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
		if (mapper == null)
			throw new NullPointerException();
		if (isPresent()) {
			Optional<U> result = mapper.apply(value);
			if (result == null)
				throw new NullPointerException();
			return result;
		}
		return empty();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Optional))
			return isPresent() ? get().equals(obj) : false;
		Optional<?> other = (Optional<?>) obj;
		if (isPresent() && other.isPresent())
			return get().equals(other.get());
		if (!isPresent() && !other.isPresent())
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		if (isPresent())
			return String.format("rOptional(%s)", value);
		return "rOptional.EMPTY";
	}

}
