package com.oxygenxml.image.markup.decorator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;

import com.oxygenxml.image.markup.AreaUpdatedListener;
import com.oxygenxml.image.markup.ImageViewerPanel;
import com.oxygenxml.image.markup.decorator.ResizeContext.ResizeType;

/**
 * Decorates a image with rectangles. Installs listeners for adding new rectangles as 
 * well as adding actions that manipulate these rectangles.
 * 
 * @author alex_jitianu
 */
public class RectangleImageDecorator implements ImageDecorator, MouseListener, MouseMotionListener {
  /**
   * The displacement we recognize as a match.
   */
  private static final int DELTA = 5;
  /**
   * The component to decorate.
   */
  private JComponent component;
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
   * The context of the current resize operation.
   */
  private ResizeContext resizeContext;
  /**
   * Listeners interested in area updates.
   */
  private List<AreaUpdatedListener> listeners = new ArrayList<AreaUpdatedListener>();

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

    if (resizeContext != null) {
      g.setColor(Color.RED);
      Rectangle hintArea = getHintArea(resizeContext.getResizePoint());
      g.drawRect(hintArea.x, hintArea.y, hintArea.width, hintArea.height);
    }
  }


  private void updateRectangleArea(Point draggedPoint) {
    ResizeContext oldContext = resizeContext;

    if (oldContext != null) {
      resizeContext = oldContext.derive(draggedPoint);
      
      // Temporary rectangle. Clear.
      Rectangle toClear = oldContext.getRectangle();
      areas.remove(toClear);
      component.repaint(toClear.x, toClear.y, toClear.width + 1, toClear.height + 1);
      Rectangle hintArea = getHintArea(oldContext.getResizePoint());
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }

    if (resizeContext != null) {
      Rectangle newRect = resizeContext.getRectangle();
      areas.add(newRect);

      component.repaint(newRect.x, newRect.y, newRect.width + 1, newRect.height + 1);
      Rectangle hintArea = getHintArea(resizeContext.getResizePoint());
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }
  }
  
  @Override
  public void mouseClicked(MouseEvent ev) {}

  @Override
  public void mouseEntered(MouseEvent arg0) {}

  @Override
  public void mouseExited(MouseEvent arg0) {}
  @Override
  public void mousePressed(MouseEvent e) {
    if (!e.isPopupTrigger()) {
      Point p = e.getPoint();
      activeArea = null;

      // Check if we are pressing on an existing
      Point resizePoint = e.getPoint();
      ResizeContext candidateArea = getHoverArea(e.getPoint());
      if (candidateArea != null) {
        resizeContext = candidateArea;
        resizePoint = candidateArea.getResizePoint();
      } else {
        resizePoint = new Point(p.x + 1, p.y + 1);
        resizeContext = new ResizeContext(ResizeType.CORNER, resizePoint, new Rectangle(p.x, p.y, 0, 0));
      }

      updateRectangleArea(resizePoint);
    }
  }

  @Override
  public void mouseReleased(MouseEvent ev) {
    ResizeContext oldContext = resizeContext;
    resizeContext = null;
    if (oldContext != null) {
      if (oldContext.getOriginalRectangle().width > 0) {
        // An actual rectangle was being resized. Not a new one.
        fireAreaUpdated(oldContext.getOriginalRectangle(), oldContext.getRectangle());
      } else if (oldContext.getRectangle().width > 1) {
        fireRectangleAdded(oldContext.getRectangle());
      }
    }
    
    if (oldContext != null) {
      if (oldContext.getRectangle().width <= 1) {
        Rectangle toClear = oldContext.getRectangle();
        areas.remove(toClear);
        component.repaint(toClear.x, toClear.y, toClear.width + 1, toClear.height + 1);
      }
      Rectangle hintArea = getHintArea(oldContext.getResizePoint());
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }
  }


  @Override
  public void mouseDragged(MouseEvent e) {
    updateRectangleArea(e.getPoint());
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    ResizeContext oldContext = resizeContext;

    resizeContext = getHoverArea(e.getPoint());

    if (oldContext != null) {
      Rectangle hintArea = getHintArea(oldContext.getResizePoint());
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }

    if (resizeContext != null) {
      Rectangle hintArea = getHintArea(resizeContext.getResizePoint());
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }
  }


  public void clean() {
    areas.clear();

    resizeContext = null;

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

  /**
   * Checks if the given point is over a resize area of one of the existing rectangles and returns that rectangle.
   * 
   * @param p Point.
   * 
   * @return The rectangle to resize.
   */
  private ResizeContext getHoverArea(Point p) {
    ResizeContext toRet = null;
    ResizeType resizeType = null;
    Point resizePoint = null;
    for (Rectangle rectangle : areas) {
      if (isMatch(new Point(rectangle.x, rectangle.y), p)) {
        resizeType = ResizeType.CORNER;
        resizePoint = new Point(rectangle.x, rectangle.y); 
      } else if (isMatch(new Point(rectangle.x + rectangle.width, rectangle.y), p)) {
        resizeType = ResizeType.CORNER;
        resizePoint = new Point(rectangle.x + rectangle.width, rectangle.y); 
      } else if (isMatch(new Point(rectangle.x, rectangle.y + rectangle.height), p)) {
        resizeType = ResizeType.CORNER;
        resizePoint = new Point(rectangle.x, rectangle.y + rectangle.height); 
      } else if (isMatch(new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height), p)) {
        resizeType = ResizeType.CORNER;
        resizePoint = new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height);
      } else if (isMatch(new Point(rectangle.x + rectangle.width / 2, rectangle.y), p)) {
        resizeType = ResizeType.SIDE;
        resizePoint = new Point(rectangle.x + rectangle.width / 2, rectangle.y); 
      } else if (isMatch(new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2), p)) {
        resizeType = ResizeType.SIDE;
        resizePoint = new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2); 
      } else if (isMatch(new Point(rectangle.x, rectangle.y + rectangle.height / 2), p)) {
        resizeType = ResizeType.SIDE;
        resizePoint = new Point(rectangle.x, rectangle.y + rectangle.height / 2); 
      } else if (isMatch(new Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height), p)) {
        resizeType = ResizeType.SIDE;
        resizePoint = new Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height); 
      }

      if (resizeType != null) {
        toRet = new ResizeContext(resizeType, resizePoint, rectangle);
        break;
      }
    }

    return toRet;
  }

  private boolean isMatch(Point referencePoint, Point point) {
    return Math.abs(referencePoint.x - point.x) < DELTA && Math.abs(referencePoint.y - point.y) < DELTA;
  }

  private Rectangle getHintArea(Point dragPoint) {
    return new Rectangle(dragPoint.x - 5, dragPoint.y - 5, 10, 10);
  }

  /**
   * Adds a new update listener.
   * 
   * @param areaUpdatedListener Area update listener.
   */
  public void addAreaUpdateListener(AreaUpdatedListener areaUpdatedListener) {
    listeners.add(areaUpdatedListener);
  }
  
  /**
   * Notify the registered listeners that an area was changed.
   * 
   * @param original Initial area.
   * @param updated Updated area.
   */
  private void fireAreaUpdated(Rectangle original, Rectangle updated) {
    for (Iterator<AreaUpdatedListener> iterator = listeners.iterator(); iterator.hasNext();) {
      AreaUpdatedListener listener = iterator.next();
      listener.rectangleUpdated(original, updated);
    }
  }
  
  /**
   * A new rectangle was added.
   * 
   * @param newArea The newly added rectangle.
   */
  void fireRectangleAdded(Rectangle newArea) {
    Rectangle candidate = null;
    int delta = Integer.MAX_VALUE;
    // Search for an already existing rectangle, closest to the new one.
    for (Rectangle rectangle : areas) {
      if (!rectangle.contains(newArea)) {
        int cDelta = newArea.y - rectangle.y;
        if (candidate == null || cDelta > 0 && cDelta < delta) {
          candidate = rectangle;
          delta = cDelta;
        }
      }
    }
    
    for (Iterator<AreaUpdatedListener> iterator = listeners.iterator(); iterator.hasNext();) {
      AreaUpdatedListener listener = iterator.next();
      listener.rectangleAdded(newArea, candidate);
    }
  }
}
