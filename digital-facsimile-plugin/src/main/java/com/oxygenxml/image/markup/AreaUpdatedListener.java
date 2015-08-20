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
  
  /**
   * A new rectangle was added.
   * 
   * @param newArea The new added rectangle.
   * @param closestArea An already existing rectangle that is closest to the new one.
   * It can be used as a reference for inserting the new one.
   */
  void rectangleAdded(Rectangle newArea, Rectangle closestArea);
}
