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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;

/**
 * Preview accessory for images in {@link JFileChooser}
 * 
 * @author Oracle and/or its affiliates
 */
public class ImagePreview extends JComponent implements PropertyChangeListener {
	private static final long serialVersionUID = 7477299564536335362L;
	
	ImageIcon thumbnail = null;
	File file = null;
	
	public ImagePreview(JFileChooser fc) {
		setPreferredSize(new Dimension(100, 50));
		fc.addPropertyChangeListener(this);
	}
	
	public void loadImage() {
		if (file == null) {
			thumbnail = null;
			return;
		}
		
		// Don't use createImageIcon (which is a wrapper for getResource)
		// because the image we're trying to load is probably not one
		// of this program's own resources.
		ImageIcon tmpIcon = new ImageIcon(file.getPath());
		if (tmpIcon != null) {
			if (tmpIcon.getIconWidth() > 90) {
				thumbnail = new ImageIcon(tmpIcon.getImage().getScaledInstance(90, -1, Image.SCALE_DEFAULT));
			} else { // no need to miniaturize
				thumbnail = tmpIcon;
			}
		}
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		boolean update = false;
		String prop = e.getPropertyName();
		
		// If the directory changed, don't show an image.
		if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
			file = null;
			update = true;
			
			// If a file became selected, find out which one.
		} else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
			file = (File) e.getNewValue();
			update = true;
		}
		
		// Update the preview accordingly.
		if (update) {
			thumbnail = null;
			if (isShowing()) {
				loadImage();
				repaint();
			}
		}
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		if (thumbnail == null) {
			loadImage();
		}
		if (thumbnail != null) {
			int x = getWidth() / 2 - thumbnail.getIconWidth() / 2;
			int y = getHeight() / 2 - thumbnail.getIconHeight() / 2;
			
			if (y < 0) {
				y = 0;
			}
			
			if (x < 5) {
				x = 5;
			}
			thumbnail.paintIcon(this, g, x, y);
		}
	}
}
