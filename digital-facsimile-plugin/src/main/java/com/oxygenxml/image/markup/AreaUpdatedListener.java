package com.oxygenxml.image.markup;

import java.awt.Rectangle;

/**
 * Receives notifications when an existing area is modified. 
 *  
 * @author alex_jitianu
 */
public interface AreaUpdatedListener {
  /**
   * An existing rectangle was modified.
   * 
   * @param originalArea Initial area.
   * @param newArea New area.
   */
  void rectangleUpdated(Rectangle originalArea, Rectangle newArea);
}
