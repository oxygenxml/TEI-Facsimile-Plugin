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
import com.oxygenxml.image.markup.controller.ImageScaleSupport;
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
  private List<Rectangle> originalAreas = new ArrayList<Rectangle>();
  /**
   * Currently active area. Either because the user invoked the contextual menu onto it
   * or because it was explicitly selected.
   */
  private Rectangle activeArea;
  /**
   * The context of the current resize operation.
   */
  private ResizeContext resizeContext;
  
  private ImageScaleSupport imageScaleSupport;
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
    dec.imageScaleSupport = component.getImageScaleSupport();
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
    for (Rectangle rectangle : originalAreas) {
      rectangle = scale(rectangle);
      if (clipBounds.intersects(rectangle.x, rectangle.y, rectangle.width + 1, rectangle.height + 1)) {
        g.setColor(Color.BLACK);
        if (activeArea != null && rectangle.equals(activeArea)) {
          g.setColor(Color.RED);	
        }

        g.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
      }
    }

    if (resizeContext != null) {
      g.setColor(Color.RED);
      Rectangle hintArea = getHintArea(scale(resizeContext.getResizePoint()));
      g.drawRect(hintArea.x, hintArea.y, hintArea.width, hintArea.height);
    }
  }


  private void updateRectangleArea(Point draggedPoint) {
    ResizeContext oldContext = resizeContext;

    if (oldContext != null) {
      resizeContext = oldContext.derive(draggedPoint);
      
      // Temporary rectangle. Clear.
      Rectangle toClear = oldContext.getRectangle();
      int indexOf = originalAreas.indexOf(toClear);
      if (indexOf != -1) {
        originalAreas.remove(indexOf);
      }
      
      toClear = scale(toClear);
      component.repaint(toClear.x, toClear.y, toClear.width + 1, toClear.height + 1);
      Rectangle hintArea = getHintArea(scale(oldContext.getResizePoint()));
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }

    if (resizeContext != null) {
      Rectangle newRect = resizeContext.getRectangle();
      originalAreas.add(newRect);

      newRect = scale(newRect);
      component.repaint(newRect.x, newRect.y, newRect.width + 1, newRect.height + 1);
      Rectangle hintArea = getHintArea(scale(resizeContext.getResizePoint()));
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
      activeArea = null;

      // Check if we are pressing on an existing
      Point resizePoint = original(e.getPoint());
      ResizeContext candidateArea = getHoverArea(resizePoint);
      if (candidateArea != null) {
        resizeContext = candidateArea;
        resizePoint = candidateArea.getResizePoint();
      } else {
        resizePoint = new Point(resizePoint.x + 1, resizePoint.y + 1);
        resizeContext = new ResizeContext(ResizeType.CORNER, resizePoint, new Rectangle(resizePoint.x, resizePoint.y, 0, 0));
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
        int indexOf = originalAreas.indexOf(toClear);
        if (indexOf != -1) {
          originalAreas.remove(indexOf);
        }
        
        toClear = scale(toClear);
        component.repaint(toClear.x, toClear.y, toClear.width + 1, toClear.height + 1);
      }
      Rectangle hintArea = getHintArea(scale(oldContext.getResizePoint()));
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }
  }


  @Override
  public void mouseDragged(MouseEvent e) {
    updateRectangleArea(original(e.getPoint()));
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    ResizeContext oldContext = resizeContext;

    resizeContext = getHoverArea(original(e.getPoint()));

    if (oldContext != null) {
      Rectangle hintArea = getHintArea(scale(oldContext.getResizePoint()));
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }

    if (resizeContext != null) {
      Rectangle hintArea = getHintArea(scale(resizeContext.getResizePoint()));
      component.repaint(hintArea.x, hintArea.y, hintArea.width + 1, hintArea.height + 1);
    }
  }


  public void clean() {
    originalAreas.clear();
    resizeContext = null;
    activeArea = null;
  }

  

  public void setAreas(List<Rectangle> areas2) {
    originalAreas.clear();
    originalAreas.addAll(areas2);
  }

  public List<Rectangle> getAreas() {
    return originalAreas;
  }

  public void setActive(Rectangle buildRectangle) {
    Rectangle oldActiveArea = activeArea;
    if (oldActiveArea != null) {
      component.repaint(oldActiveArea.x, oldActiveArea.y, oldActiveArea.width + 1, oldActiveArea.height + 1);
    }

    activeArea = buildRectangle != null ? scale(buildRectangle) : null;
    if (activeArea != null) {
      component.repaint(activeArea.x, activeArea.y, activeArea.width + 1, activeArea.height + 1);
    }

  }

  public void removeArea(Rectangle toProcess) {
    int indexOf = originalAreas.indexOf(toProcess);
    if (indexOf != -1) {
      originalAreas.remove(indexOf);

      Rectangle scaled = scale(toProcess);
      component.repaint(scaled.x, scaled.y, scaled.width + 1, scaled.height + 1);
    }
  }
  
  
  private Rectangle scale(Rectangle area) {
    // Scale the area
    int x = imageScaleSupport.applyScale(area.x);
    int y = imageScaleSupport.applyScale(area.y);
    int lx = imageScaleSupport.applyScale(area.x + area.width);
    int ly = imageScaleSupport.applyScale(area.y + area.height);
    
    return new Rectangle(x, y, lx - x, ly -y);
  }
  
  private Point scale(Point area) {
    // Scale the area
    int x = imageScaleSupport.applyScale(area.x);
    int y = imageScaleSupport.applyScale(area.y);
    
    return new Point(x, y);
  }
  
  private Rectangle original(Rectangle area) {
    // Scale the area
    int x = imageScaleSupport .getOriginal(area.x);
    int y = imageScaleSupport.getOriginal(area.y);
    int lx = imageScaleSupport.getOriginal(area.x + area.width);
    int ly = imageScaleSupport.getOriginal(area.y + area.height);
    
    return new Rectangle(x, y, lx - x, ly -y);
  }
  
  private Point original(Point area) {
    // Scale the area
    int x = imageScaleSupport.getOriginal(area.x);
    int y = imageScaleSupport.getOriginal(area.y);
    
    return new Point(x, y);
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
    for (Rectangle rectangle : originalAreas) {
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
      } 
      // TODO The side resizing is not yet supported.
//      else if (isMatch(new Point(rectangle.x + rectangle.width / 2, rectangle.y), p)) {
//        resizeType = ResizeType.SIDE;
//        resizePoint = new Point(rectangle.x + rectangle.width / 2, rectangle.y); 
//      } else if (isMatch(new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2), p)) {
//        resizeType = ResizeType.SIDE;
//        resizePoint = new Point(rectangle.x + rectangle.width, rectangle.y + rectangle.height / 2); 
//      } else if (isMatch(new Point(rectangle.x, rectangle.y + rectangle.height / 2), p)) {
//        resizeType = ResizeType.SIDE;
//        resizePoint = new Point(rectangle.x, rectangle.y + rectangle.height / 2); 
//      } else if (isMatch(new Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height), p)) {
//        resizeType = ResizeType.SIDE;
//        resizePoint = new Point(rectangle.x + rectangle.width / 2, rectangle.y + rectangle.height); 
//      }

      if (resizeType != null) {
        toRet = new ResizeContext(resizeType, resizePoint, rectangle);
        break;
      }
    }

    return toRet;
  }

  private boolean isMatch(Point referencePoint, Point point) {
    int original = imageScaleSupport.getOriginal(DELTA);
    
    return Math.abs(referencePoint.x - point.x) < original && Math.abs(referencePoint.y - point.y) < original;
  }

  private Rectangle getHintArea(Point dragPoint) {
    int original = 5;
    return new Rectangle(dragPoint.x - original, dragPoint.y - original, original * 2, original * 2);
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
    for (Rectangle rectangle : originalAreas) {
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
