package com.oxygenxml.image.markup;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.WSTextEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextNodeRange;
import ro.sync.exml.workspace.api.editor.page.text.xml.XPathException;
import ro.sync.exml.workspace.api.listeners.WSEditorChangeListener;
import ro.sync.exml.workspace.api.listeners.WSEditorListener;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

import com.oxygenxml.image.markup.decorator.RectangleImageDecorator;

/**
 * The image controller that is aware of the TEI vocabulary.
 */
public class ImageController {
  /**
   * Place holder for the image.
   */
  private ImageViewerPanel imageViewerPanel;
  /**
   * Decorator installed on the panel.
   */
  private RectangleImageDecorator decorator;
  /**
   * Oxygen access.
   */
  private StandalonePluginWorkspace pluginWorkspaceAccess;
  /**
   * A timer user to coalesce the caret move events.
   */
  private javax.swing.Timer timer = new javax.swing.Timer(400, new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (pluginWorkspaceAccess.isViewShowing(ImageViewerPanel.IMAGE_VIEWER_ID)) {
        syncZone();
      }
    }
  });

  /**
   * Caret listener added on the text page to keep in sync.
   */
  private CaretListener caretListener = new CaretListener() {
    int previous = -1;
    @Override
    public void caretUpdate(CaretEvent e) {
      if (e.getDot() != previous) {
        previous = e.getDot();
        timer.stop();
        timer.start();
      }
    }
  };

  /**
   * Constructor.
   * 
   * @param viewerPanel The image panel.
   */
  public ImageController(ImageViewerPanel viewerPanel) {
    timer.setRepeats(false);
    this.imageViewerPanel = viewerPanel;

    decorator = RectangleImageDecorator.install(imageViewerPanel);
    decorator.addAreaUpdateListener(new AreaUpdatedListener() {
      @Override
      public void rectangleUpdated(Rectangle originalArea, Rectangle newArea) {
        // Remove the zone from the editor.
        WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
        if (editorAccess != null) {
          WSEditorPage currentPage = editorAccess.getCurrentPage();
          if (currentPage instanceof WSXMLTextEditorPage) {
            WSXMLTextEditorPage textEditorPage = (WSXMLTextEditorPage) currentPage;
            String xpath = createXPath(originalArea);
            try {
              WSXMLTextNodeRange[] ranges = textEditorPage.findElementsByXPath(xpath);
              if (ranges != null && ranges.length > 0) {
                WSXMLTextNodeRange range = ranges[0];
                Document document = textEditorPage.getDocument();
                int startOffset = textEditorPage.getOffsetOfLineStart(range.getStartLine()) + range.getStartColumn() - 1;
                int endOffset = textEditorPage.getOffsetOfLineStart(range.getEndLine()) + range.getEndColumn();

                document.remove(startOffset, endOffset - startOffset);
                
                // Insert the new content.
                document.insertString(startOffset, buildZoneElement(newArea), null);
              }
            } catch (XPathException e) {
              e.printStackTrace();
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
          }
        }
      }
    });

    imageViewerPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        showPopup(e);
      }
    });
  }

  /**
   * Link to the Oxygen workspace.
   * 
   * @param pluginWorkspaceAccess Oxygen workspace access.
   */
  public void init(final StandalonePluginWorkspace pluginWorkspaceAccess) {
    this.pluginWorkspaceAccess = pluginWorkspaceAccess;
    pluginWorkspaceAccess.addEditorChangeListener(new WSEditorChangeListener() {
      private WSEditorListener editorListener = new WSEditorListener() {
        @Override
        public void editorPageChanged() {
          WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
          installTextPageListeners(editorAccess);
        }
      };

      private WSEditor installedEditor;
      @Override
      public void editorSelected(URL editorLocation) {
        if (installedEditor != null) {
          installedEditor.removeEditorListener(editorListener);
          uninstallTextPageListeners(installedEditor);
          installedEditor = null;
        }

        if (editorLocation != null) {
          WSEditor editorAccess = pluginWorkspaceAccess.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
          editorAccess.addEditorListener(editorListener);
          installTextPageListeners(editorAccess);
          installedEditor = editorAccess;
        }
      }

      @Override
      public boolean editorAboutToBeClosed(URL editorLocation) {
        WSEditor editorAccess = pluginWorkspaceAccess.getEditorAccess(editorLocation, PluginWorkspace.MAIN_EDITING_AREA);
        editorAccess.removeEditorListener(editorListener);
        uninstallTextPageListeners(editorAccess);

        return true;
      }
    }, PluginWorkspace.MAIN_EDITING_AREA);
  }

  /**
   * Install sync listener on the text page of the given editor.
   * 
   * @param editorAccess Editor access.
   */
  private void installTextPageListeners(WSEditor editorAccess) {
    WSEditorPage currentPage = editorAccess.getCurrentPage();
    if (currentPage instanceof WSXMLTextEditorPage) {
      JTextArea textComponent = (JTextArea) ((WSXMLTextEditorPage) currentPage).getTextComponent();

      textComponent.removeCaretListener(caretListener);
      textComponent.addCaretListener(caretListener);
    }
  }

  /**
   * UnInstall sync listener on the text page of the given editor.
   * 
   * @param editorAccess Editor access.
   */
  private void uninstallTextPageListeners(WSEditor editorAccess) {
    WSEditorPage currentPage = editorAccess.getCurrentPage();
    if (currentPage instanceof WSXMLTextEditorPage) {
      JTextArea textComponent = (JTextArea) ((WSXMLTextEditorPage) currentPage).getTextComponent();

      textComponent.removeCaretListener(caretListener);
    }
  }

  /**
   * Identify the current active zone in the editor and paint it accordingly in 
   * the viewer.
   */
  protected void syncZone() {
    WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
    if (editorAccess != null && editorAccess.getCurrentPage() instanceof WSXMLTextEditorPage) {
      WSXMLTextEditorPage editorPage = (WSXMLTextEditorPage) editorAccess.getCurrentPage();

      try {
        Object[] zones = editorPage.evaluateXPath("for $zone in ancestor-or-self::zone[1] return string-join(($zone/@ulx, $zone/@uly, $zone/@lrx, $zone/@lry), ',')");
        if (zones != null && zones.length > 0) {
          select((String) zones[0]);
        }
      } catch (XPathException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Initializes the decorator with the selected areas.
   * 
   * @param zones Areas to draw over the image.
   */
  public void init(Object[] zones) {
    List<Rectangle> areas = new ArrayList<Rectangle>();
    for (int i = 0; i < zones.length; i++) {
      String val = (String) zones[i];

      Rectangle rectangle = buildRectangle(val);
      areas.add(rectangle);
    }

    decorator.setAreas(areas);

    // Invalidate the placeholder.
    JComponent placeholder = imageViewerPanel.getPlaceholder();
    //		placeholder.invalidate();
    //		placeholder.revalidate();
    placeholder.repaint();
  }

  /**
   * Parses a string representation of the rectangle: x1, y1, x2, y2.
   * x1, y1 - top left corner
   * x2, y2 - bottom right corner
   * 
   * @param zone
   * 
   * @return The rectangle
   */
  private Rectangle buildRectangle(String zone) {
    String[] splits = zone.split(",");

    int x = Integer.parseInt(splits[0]);
    int y = Integer.parseInt(splits[1]);
    int lx = Integer.parseInt(splits[2]);
    int ly = Integer.parseInt(splits[3]);
    Rectangle rectangle = new Rectangle(
        x,
        y,
        lx - x,
        ly - y);
    return rectangle;
  }

  /**
   * Selects a zone. The zone is in given in the form: x1, y1, x2, y2.
   * x1, y1 - top left corner
   * x2, y2 - bottom right corner
   * 
   * @param zone A zone to select.
   * 
   * @return The rectangle
   */
  public void select(String zone) {
    if (zone.length() > 0) {
      decorator.setActive(buildRectangle(zone));
    }
  }

  /**
   * Open the selected image from the current editor.
   */
  void openSelected() {
    WSEditor currentEditorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
        PluginWorkspace.MAIN_EDITING_AREA);
    if (currentEditorAccess != null) {
      WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
      if (currentPage instanceof WSXMLTextEditorPage) {
        String selectedText = ((WSTextEditorPage) currentPage).getSelectedText();
        try {
          URL toOpen = new URL(currentEditorAccess.getEditorLocation(), selectedText);
          openImage(currentPage, toOpen);

        } catch (MalformedURLException e1) {
          e1.printStackTrace();
        } catch (IOException e1) {
          e1.printStackTrace();
        } catch (XPathException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  /**
   * Open the image with the given URL.
   * 
   * @param currentPage
   * @param toOpen
   * @throws IOException
   * @throws XPathException
   */
  private void openImage(WSEditorPage currentPage, URL toOpen)
      throws IOException, XPathException {
    imageViewerPanel.showImage(toOpen);

    Object[] evaluateXPath = ((WSXMLTextEditorPage) currentPage).evaluateXPath("for $zone in //zone return string-join(($zone/@ulx, $zone/@uly, $zone/@lrx, $zone/@lry), ',')");
    if (evaluateXPath != null && evaluateXPath.length > 0) {
      init(evaluateXPath);
    }
  }
  
  /**
   * Open the image with the given URL.
   * 
   * @param currentPage
   * @param toOpen
   * @throws IOException
   * @throws XPathException
   */
  public void openImage(URL toOpen)
      throws IOException, XPathException {
    WSEditor currentEditorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
        PluginWorkspace.MAIN_EDITING_AREA);
    if (currentEditorAccess != null) {
      WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
      if (currentPage instanceof WSXMLTextEditorPage) {
        try {
          openImage(currentPage, toOpen);
        } catch (MalformedURLException e1) {
          e1.printStackTrace();
        } catch (IOException e1) {
          e1.printStackTrace();
        } catch (XPathException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  private void showPopup(MouseEvent ev) {
    Point point = ev.getPoint();
    if (ev.isPopupTrigger()) {
      Rectangle candidate = null;
      for (Rectangle rectangle : decorator.getAreas()) {
        if (rectangle.contains(point)) {
          if (candidate == null
              // This is a smaller rectangle.
              || candidate.contains(rectangle)) {
            candidate = rectangle;
          }
        }
      }

      if (candidate != null) {
        decorator.setActive(candidate);
        JPopupMenu popup = new JPopupMenu();
        final Rectangle toProcess = candidate;
        AbstractAction copyAction = new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            /**
             * <zone
			                ulx="25"
			                uly="25"
			                lrx="180"
			                lry="60">
             */

            String string = buildZoneElement(toProcess);
            StringSelection selection = new StringSelection(string);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
          }
        };
        copyAction.putValue(Action.NAME, "Copy");
        popup.add(copyAction);

        AbstractAction removeAction = new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            decorator.removeArea(toProcess);

            // Remove the zone from the editor.
            WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
            if (editorAccess != null) {
              WSEditorPage currentPage = editorAccess.getCurrentPage();
              if (currentPage instanceof WSXMLTextEditorPage) {
                WSXMLTextEditorPage textEditorPage = (WSXMLTextEditorPage) currentPage;
                try {
                  ((JTextComponent) textEditorPage.getTextComponent()).removeCaretListener(caretListener);
                  try {
                    String xpath = createXPath(toProcess);
                    WSXMLTextNodeRange[] ranges = textEditorPage.findElementsByXPath(xpath);

                    if (ranges != null && ranges.length > 0) {
                      WSXMLTextNodeRange range = ranges[0];
                      Document document = textEditorPage.getDocument();
                      int startOffset = textEditorPage.getOffsetOfLineStart(range.getStartLine()) + range.getStartColumn() - 1;
                      int endOffset = textEditorPage.getOffsetOfLineStart(range.getEndLine()) + range.getEndColumn();

                      document.remove(startOffset, endOffset - startOffset);
                    }
                  } catch (XPathException e) {
                    e.printStackTrace();
                  } catch (BadLocationException e) {
                    e.printStackTrace();
                  }
                } finally {
                  ((JTextComponent) textEditorPage.getTextComponent()).addCaretListener(caretListener);
                }
              }
            }
          }
        };
        removeAction.putValue(Action.NAME, "Remove");
        popup.add(removeAction);

        popup.addPopupMenuListener(new PopupMenuListener() {
          @Override
          public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
          @Override
          public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            decorator.setActive(null);
          }
          @Override
          public void popupMenuCanceled(PopupMenuEvent e) {
            decorator.setActive(null);
          }
        });

        popup.show(imageViewerPanel, point.x, point.y);
      }
    }
  }
  
  /**
   * Creates an XPath that identifies the given rectangle.
   *  
   * @param toProcess The rectangle to identify.
   * 
   * @return An XPath expression that when executed will identify the area in the document.
   */
  private String createXPath(final Rectangle toProcess) {
    String xpath = "//zone[@ulx='" + toProcess.x
        + "'][@uly='" + toProcess.y
        + "'][@lrx='" + (toProcess.width + toProcess.x)
        + "'][@lry='" + (toProcess.height + toProcess.y)
        + "']";
    return xpath;
  }
  
  /**
   * Builds an TEI zone element for the given rectangle.
   * 
   * @param toProcess Rectangle to serialize.
   * 
   * @return A serialization of the rectangle, that can be inserted in the document.
   */
  private String buildZoneElement(final Rectangle toProcess) {
    StringBuilder b = new StringBuilder("<zone ulx=\"");
    b.append(toProcess.x).append("\"");
    b.append(" uly=\"").append(toProcess.y).append("\"");
    b.append(" lrx=\"").append(toProcess.x + toProcess.width).append("\"");
    b.append(" lry=\"").append(toProcess.y + toProcess.height).append("\"/>");

    return b.toString();
  }
}
