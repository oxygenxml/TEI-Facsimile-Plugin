package com.oxygenxml.image.markup.decorator;

import java.awt.Point;
import java.awt.Rectangle;

public class ResizeContext {
  /**
   * Tells if the resize is done by a corner or by a side.
   */
  private ResizeType resizeType;
  /**
   * The actual point that is being dragged to resize.
   */
  private Point resizePoint;
  /**
   * The actual resized rectangle being resized.
   */
  private  Rectangle rectangle;
  /**
   * The original rectangle being resized.
   */
  private Rectangle originalRectangle;

  /**
   * Creates a new resizing context.
   * @param resizeType
   * @param resizePoint
   * @param rectangle
   */
  public ResizeContext(
      ResizeType resizeType, 
      Point resizePoint,
      Rectangle rectangle) {
    this(resizeType, resizePoint, rectangle, rectangle);
  }

  private ResizeContext(
      ResizeType resizeType, 
      Point resizePoint,
      Rectangle rectangle,
      Rectangle original) {
    this.resizeType = resizeType;
    this.resizePoint = resizePoint;
    this.rectangle = rectangle;
    this.originalRectangle = original;
  }

  public static enum ResizeType {
    CORNER, SIDE
  }

  public ResizeContext derive(Point newDragged) {
    ResizeContext derived = null;

    if (resizeType == ResizeType.CORNER) {
      Point opposingPoint = new Point();

      if (resizePoint.x == rectangle.x) {
        opposingPoint.x = rectangle.x + rectangle.width;
      } else {
        opposingPoint.x = rectangle.x;
      }

      if (resizePoint.y == rectangle.y) {
        opposingPoint.y = rectangle.y + rectangle.height;
      } else {
        opposingPoint.y = rectangle.y;
      }

      Rectangle newR = createRect(opposingPoint, newDragged);

      derived = new ResizeContext(resizeType, newDragged, newR, originalRectangle);
    }

    return derived;
  }

  private Rectangle createRect(Point p1, Point p2) {
    return new Rectangle(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.abs(p1.x - p2.x), Math.abs(p1.y - p2.y));
  }

  public Rectangle getOriginalRectangle() {
    return originalRectangle;
  }

  public Rectangle getRectangle() {
    return rectangle;
  }

  public Point getResizePoint() {
    return resizePoint;
  }
  public ResizeType getResizeType() {
    return resizeType;
  }
}
