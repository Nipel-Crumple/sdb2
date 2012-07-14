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
package org.zephyrsoft.sdb2.gui.renderer;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zephyrsoft.sdb2.model.LanguageEnum;

/**
 * A {@link ListCellRenderer} for {@link LanguageEnum} values.
 * 
 * @author Mathis Dirksen-Thedens
 */
public class LanguageCellRenderer implements ListCellRenderer<LanguageEnum> {
	
	private static final Logger LOG = LoggerFactory.getLogger(LanguageCellRenderer.class);
	
	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
	
	@Override
	public Component getListCellRendererComponent(JList<? extends LanguageEnum> list, LanguageEnum value, int index,
		boolean isSelected, boolean cellHasFocus) {
		JLabel ret =
			(JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value == null) {
			ret.setText("");
		} else if (value == LanguageEnum.GERMAN) {
			ret.setText("German");
		} else if (value == LanguageEnum.ENGLISH) {
			ret.setText("English");
		} else if (value == LanguageEnum.MIXED) {
			ret.setText("Mixed");
		} else {
			LOG.warn("unknown language enum value: {}", value.toString());
			ret.setText(value.name());
		}
		return ret;
	}
}
