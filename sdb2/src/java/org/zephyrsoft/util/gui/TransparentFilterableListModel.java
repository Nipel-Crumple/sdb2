/*
 * This file is part of the Song Database (SDB).
 *
 * SDB is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * SDB is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SDB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.zephyrsoft.util.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * A list model which transparently uses an underlying {@link List} and supports filtering the contents.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class TransparentFilterableListModel<T> extends TransparentListModel<T> {
	
	private static final long serialVersionUID = -4892272915483148933L;
	
	private final List<T> filteredList = new ArrayList<>();
	private ListFilter<T> filter;
	
	public TransparentFilterableListModel(List<T> underlyingList) {
		super(underlyingList);
		filteredList.addAll(super.getAllElements());
	}
	
	@Override
	public int getSize() {
		return filteredList.size();
	}
	
	@Override
	public T getElementAt(int index) {
		return filteredList.get(index);
	}
	
	@Override
	public List<T> getAllElements() {
		return filteredList;
	}
	
	public ListFilter<T> getFilter() {
		return filter;
	}
	
	public void setFilter(ListFilter<T> filter) {
		this.filter = filter;
		refilter();
	}
	
	public void refilter() {
		if (filter == null) {
			// everything is accepted
			filteredList.clear();
			filteredList.addAll(super.getAllElements());
		} else {
			filteredList.clear();
			for (T item : super.getAllElements()) {
				if (filter.isAccepted(item)) {
					filteredList.add(item);
				}
			}
		}
		fireContentsChanged(this, 0, getSize());
	}
	
}
