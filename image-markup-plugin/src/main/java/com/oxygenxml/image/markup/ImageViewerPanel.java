package com.oxygenxml.image.markup;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import com.oxygenxml.image.markup.controller.ImageScaleSupport;
import com.oxygenxml.image.markup.decorator.ImageDecorator;

/**
 * Placeholder that draws an image. It supports additional decorators that can
 * paint over the original image.
 *  
 * @author alex_jitianu
 */
public class ImageViewerPanel extends JPanel implements Scrollable {
  /**
   * The ID of the view.
   */
  public static final String IMAGE_VIEWER_ID = "Image-Markup";
  /**
   * A scroll pane over the image.
   */
  JScrollPane imageScroll;
  /**
   * The painted image.
   */
  private BufferedImage image;
  /**
   * Decorator used to draw over the image.
   */
  private ImageDecorator decorator;
  /**
   * Scale support.
   */
  private ImageScaleSupport imageScaleSupport;

  /**
   * Constructor.
   */
  public ImageViewerPanel() {
    imageScroll = new JScrollPane(this);
  }
  
  /**
   * Show the given image.
   * 
   * @param imageURL Image location.
   * 
   * @throws IOException Unable to load the image from the given location.
   */
  public void showImage(URL imageURL) throws IOException {
    image = ImageIO.read(imageURL);

    decorator.clean();

    imageScroll.invalidate();
    imageScroll.revalidate();
    imageScroll.repaint();
  }

  /**
   * Paints the image and delegates to the decorator.
   */
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (image != null) { 
      Graphics2D g2d = (Graphics2D) g;

      AffineTransform tx = new AffineTransform();
      double scale = imageScaleSupport.getScale();
      tx.scale(scale, scale);
      g2d.drawImage(image, tx, this);

      decorator.paint(g);
    }
  }

  /**
   * @return The image container.
   */
  public JComponent getPlaceholder() {
    return imageScroll;
  }

  /**
   * @see javax.swing.Scrollable#getScrollableUnitIncrement(java.awt.Rectangle, int, int)
   */
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return 20;
  }

  /**
   * @see javax.swing.Scrollable#getScrollableBlockIncrement(java.awt.Rectangle, int, int)
   */
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    if (orientation == SwingConstants.VERTICAL) {
      return visibleRect.height;
    }
    return visibleRect.width;
  }

  /**
   * @see javax.swing.Scrollable#getScrollableTracksViewportWidth()
   */
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  /**
   * @see javax.swing.Scrollable#getScrollableTracksViewportHeight()
   */
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  /**
   * @return The size of the component.
   * 
   * 
   * @see javax.swing.Scrollable#getPreferredScrollableViewportSize()
   */
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }  

  /**
   * @see javax.swing.JComponent#getPreferredSize()
   */
  @Override
  public Dimension getPreferredSize() {
    Dimension toRet = null;
    Dimension scrollDim = imageScroll.getViewport().getSize(); 
    int scrollWidth = scrollDim.width;
    int scrollHeight = scrollDim.height;

    if(image == null) {
      toRet = new Dimension(scrollWidth, scrollHeight);
    } else {
      // One to one
      double scale = imageScaleSupport.getScale();
      int width2 = (int) (image.getWidth() * scale);
      int height2 = (int) (image.getHeight() * scale);
      toRet = 
          new Dimension(
              width2 < scrollWidth ? scrollWidth : width2, 
                  height2 < scrollHeight ? scrollHeight : height2);
    }
    return toRet;
  }

  /**
   * 
   * @param dec
   */
  public void setDecorator(ImageDecorator dec) {
    decorator = dec;
  }
  
  public void setImageScaleSupport(ImageScaleSupport imageScaleSupport) {
    this.imageScaleSupport = imageScaleSupport;
  }
  
  public ImageScaleSupport getImageScaleSupport() {
    return imageScaleSupport;
  }
}
