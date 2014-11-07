package com.oxygenxml.image.markup.decorator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import com.oxygenxml.image.markup.ImageViewerPanel;

/**
 * Decorates a image with rectangles. Installs listeners for adding new rectangles as 
 * well as adding actions that manipulate these rectangles.
 * 
 * @author alex_jitianu
 */
public class RectangleImageDecorator implements ImageDecorator, MouseListener, MouseMotionListener {
	/**
	 * The component to decorate.
	 */
	private JComponent component;
	/**
	 * Upper left point of the rectangle currently being constructed..
	 */
	private Point p1;
	/**
	 * Lower right point of the rectangle currently being constructed..
	 */
	private Point p2;
	/**
	 * All the rectangles.
	 */
	private List<Rectangle> areas = new ArrayList<Rectangle>();
	/**
	 * Currently active area. Either because the user invoked the contextual menu onto it
	 * or because it was explicitly selected.
	 */
	private Rectangle activeArea;
	
	/**
	 * Installs a decorator on the given component.
	 * 
	 * @param component Component to install.
	 * 
	 * @return The installed decorator.
	 */
	public static RectangleImageDecorator install(ImageViewerPanel component) {
		RectangleImageDecorator dec = new RectangleImageDecorator();
		dec.component = component;
		component.addMouseListener(dec);
		component.addMouseMotionListener(dec);
		
		component.setDecorator(dec);
		
		return dec;
	}

	/**
	 * Decorates the component
	 * 
	 * @param g Graphics to paint into.
	 */
	public void paint(Graphics g) {
		Rectangle clipBounds = g.getClipBounds();
		for (Rectangle rectangle : areas) {
			if (clipBounds.intersects(rectangle)) {
				g.setColor(Color.BLACK);
				if (activeArea != null && rectangle.equals(activeArea)) {
					g.setColor(Color.RED);	
				}
				
				g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
			}
		}
	}
	
	
	private void repaintComponent(Point point1, Point point2) {
		Point oldP1 = p1;
		Point oldP2 = p2;
		
		p1 = point1;
		p2 = point2;
		
		if (oldP1 != null && oldP2 != null) {
			// Temporary rectangle. Clear.
			Rectangle toClear = createRect(oldP1, oldP2);
			areas.remove(toClear);
			component.repaint(toClear.x, toClear.y, toClear.width + 1, toClear.height + 1);
		}
		
		if (p1 != null && p2 != null) {
			Rectangle newRect = createRect(p1, p2);
			areas.add(newRect);
			component.repaint(newRect.x, newRect.y, newRect.width + 1, newRect.height + 1);
		}
	}

	private Rectangle createRect(Point p1, Point p2) {
		return new Rectangle(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y));
	}


	@Override
	public void mouseClicked(MouseEvent ev) {}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {
		if (!arg0.isPopupTrigger()) {
			activeArea = null;
			repaintComponent(arg0.getPoint(), null);
		}
	}

	@Override
	public void mouseReleased(MouseEvent ev) {
		p1 = null;
		p2 = null;
	}


	@Override
	public void mouseDragged(MouseEvent arg0) {
		// TODO To allow resizing the rectangle from any point we should change a bit.
		//1. On mouse pressed remember the rectangle and the affected point. It can be one of the corners or the middle of a rectangle side.
		//2. On mouse drag translate the points had the same AXIS (X or Y) with the dragged point.
		// TODO The creation of a new rectangle is  pretty much the same thing as the previous procedure.
		// A special care should be taken for the case when the rectangle has zero width (when it started)
		
		repaintComponent(p1, arg0.getPoint());
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {}


	public void clean() {
		areas.clear();
		p1 = null;
		p2 = null;
		activeArea = null;
	}


	public void setAreas(List<Rectangle> areas2) {
		areas.clear();
		areas.addAll(areas2);
	}
	
	public List<Rectangle> getAreas() {
		return areas;
	}

	public void setActive(Rectangle buildRectangle) {
		Rectangle oldActiveArea = activeArea;
		if (oldActiveArea != null) {
			component.repaint(oldActiveArea.x, oldActiveArea.y, oldActiveArea.width + 1, oldActiveArea.height + 1);
		}
		
		activeArea = buildRectangle;
		if (activeArea != null) {
			component.repaint(activeArea.x, activeArea.y, activeArea.width + 1, activeArea.height + 1);
		}
		
	}

	public void removeArea(Rectangle toProcess) {
		areas.remove(toProcess);
		
		component.repaint(toProcess.x, toProcess.y, toProcess.width + 1, toProcess.height + 1);
	}
}
