package com.oxygenxml.image.markup.controller;

/**
 * Listener interested in notifications when the scale changes.
 */
public interface ScaleListener {
  /**
   * The applied scaled has changed.
   * 
   * @param oldScale Old scale.
   * @param newScale New scale.
   */
  void scaleEvent(double oldScale, double newScale);
}
