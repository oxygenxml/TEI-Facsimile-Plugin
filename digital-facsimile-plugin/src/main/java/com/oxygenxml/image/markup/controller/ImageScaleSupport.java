package com.oxygenxml.image.markup.controller;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

/**
 * Scaling support for the panel that presents the image. 
 */
public class ImageScaleSupport {
  /**
   * The scale factor.
   */
  private double scale = 1.0;
  /**
   * Listeners interest in scale change.
   */
  private List<ScaleListener> listeners = new ArrayList<ScaleListener>(1);
  
  public ImageScaleSupport(final JComponent panel) {
    panel.addMouseWheelListener(new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        if ((e.getModifiers() & InputEvent.CTRL_MASK) != 0) {
          double oldScale = scale;
          if (e.getWheelRotation() > 0) {
            scale = scale / 2;
          } else {
            scale = scale * 2;
          }
          
          fireScaleListener(oldScale, scale);
          
          panel.invalidate();
          panel.doLayout();
          panel.doLayout();
          e.consume();
        }
      }
    });
  }
  
  /**
   * Applies the scaling factor on the given coordinates.
   * 
   * @param p Coordinates.
   * 
   * @return Scaled coordinates.
   */
  public Point applyScale(Point p) {
    return new Point((int) (p.x * scale), (int) (p.y * scale));
  }
  
  /**
   * Subtracts the scaling factor in order to obtain the original/real coordinates.
   * 
   * @param p Coordinates.
   * 
   * @return Down scaled coordinates.
   */
  public Point getOriginal(Point p) {
    return new Point((int) (p.x / scale), (int) (p.y / scale));
  }
  
  /**
   * Applies the scaling factor on the given coordinate.
   * 
   * @param x Coordinate.
   * 
   * @return Scaled coordinate.
   */
  public int applyScale(int x) {
    return (int) (x * scale);
  }
  
  /**
   * Subtracts the scaling factor in order to obtain the original/real coordinate.
   * 
   * @param x Coordinate.
   * 
   * @return Down scaled coordinate.
   */
  public int getOriginal(int x) {
    return (int) (x / scale);
  }
  
  /**
   * @return The current applied scaled.
   */
  public double getScale() {
    return scale;
  }

  /**
   * Adds a listener interested in scaling events.
   * 
   * @param listener Scaling events listener.
   */
  public void addScaleListener(ScaleListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes a listener interested in scaling events.
   * 
   * @param listener Scaling events listener.
   */
  public void removeScaleListener(ScaleListener listener) {
    listeners.remove(listener);
  }
  
  /**
   * The applied scaling has changed. Notify the interested listeners.
   * 
   * @param oldScale Old active scale.
   * @param newScale New active scale.
   */
  private void fireScaleListener(double oldScale, double newScale) {
    for (ScaleListener scaleListener : listeners) {
      scaleListener.scaleEvent(oldScale, newScale);
    }
  }
}
