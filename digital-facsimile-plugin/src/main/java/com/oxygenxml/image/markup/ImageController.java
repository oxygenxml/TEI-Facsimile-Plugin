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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import ro.sync.util.URLUtil;

import com.oxygenxml.image.markup.controller.ImageScaleSupport;
import com.oxygenxml.image.markup.controller.ScaleListener;
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
   * A timer user to coalesce the document changes.
   */
  private javax.swing.Timer reloadAreasTimer = new javax.swing.Timer(800, new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (pluginWorkspaceAccess.isViewShowing(ImageViewerPanel.IMAGE_VIEWER_ID)) {
        try {
          reloadAreas(pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA).getCurrentPage());
        } catch (XPathException e1) {
          e1.printStackTrace();
        }
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
  
  private boolean inhibit = false; 
  private DocumentListener documentListener = new DocumentListener() {
    @Override
    public void removeUpdate(DocumentEvent e) {
      if (!inhibit) {
        reloadAreasTimer.stop();
        reloadAreasTimer.start();
      }
    }
    @Override
    public void insertUpdate(DocumentEvent e) {
      removeUpdate(e);
    }
    @Override
    public void changedUpdate(DocumentEvent e) {
      removeUpdate(e);
    }
  };
  
  private ImageScaleSupport imageScaleSupport;
  /**
   * The loaded image.
   */
  private String selectedImageToLoad;
  
  /**
   * Constructor.
   * 
   * @param viewerPanel The image panel.
   */
  public ImageController(ImageViewerPanel viewerPanel) {
    timer.setRepeats(false);
    this.imageViewerPanel = viewerPanel;
    
    imageScaleSupport = new ImageScaleSupport(viewerPanel.getPlaceholder());
    imageScaleSupport.addScaleListener(new ScaleListener() {
      @Override
      public void scaleEvent(double oldScale, double newScale) {
        // The scale has changed. Reload the areas.
        try {
          reloadAreas(null);
          syncZone();
        } catch (XPathException e) {
          e.printStackTrace();
        }
      }
    });
    viewerPanel.setImageScaleSupport(imageScaleSupport);

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
            // Get the coordinatyes attributes individually.
            String xpath = createXPath(originalArea) + "/@*[local-name() = 'ulx' or local-name() = 'uly' or local-name() = 'lrx' or local-name() = 'lry']";
            try {
              WSXMLTextNodeRange[] ranges = textEditorPage.findElementsByXPath(xpath);
              if (ranges != null && ranges.length > 0) {
                List<WSXMLTextNodeRange> toSort = new ArrayList<WSXMLTextNodeRange>(Arrays.asList(ranges));
                // We need to sort them descending to make sure we are using the correct offsets.
                Collections.sort(toSort, new Comparator<WSXMLTextNodeRange>() {
                  @Override
                  public int compare(WSXMLTextNodeRange o1,
                      WSXMLTextNodeRange o2) {
                    int result = 0;
                    if (o1.getStartLine() < o2.getStartLine()) {
                      result = -1;
                    } else if (o1.getStartLine() == o2.getStartLine()) {
                      result = o1.getStartColumn() - o2.getStartColumn();
                    } else {
                      result = 1;
                    }
                    
                    return result;
                  }
                  
                });
                // Replace each attribute with it's corresponding one instead of replacing them all at once.
                textEditorPage.beginCompoundUndoableEdit();
                disableSync(textEditorPage);
                int selectStart = -1;
                int selectEnd = -1;
                try {
                  Document document = textEditorPage.getDocument();
                  for (int i = toSort.size() - 1; i >= 0; i--) {
                    WSXMLTextNodeRange range  = toSort.get(i);
                    
                    int startOffset = textEditorPage.getOffsetOfLineStart(range.getStartLine()) + range.getStartColumn() - 1;
                    int endOffset = textEditorPage.getOffsetOfLineStart(range.getEndLine()) + range.getEndColumn() - 1;
                    
                    if (selectEnd == -1) {
                      selectEnd = endOffset;
                    }
                    selectStart = startOffset;
                    
                    String text = document.getText(startOffset, endOffset - startOffset);
                    String str = getReplacement(newArea, text);
                    
                    if (!str.equals(text.replace('\'', '"'))) {
                      document.remove(startOffset, endOffset - startOffset);
                      document.insertString(startOffset, str, null);
                    }
                  }
                  
                  textEditorPage.select(selectStart, selectEnd);
                } finally {
                  textEditorPage.endCompoundUndoableEdit();
                  enableSync(textEditorPage);
                }
              }
            } catch (XPathException e) {
              e.printStackTrace();
            } catch (BadLocationException e) {
              e.printStackTrace();
            }
          }
        }
      }

      @Override
      public void rectangleAdded(Rectangle newArea, Rectangle closestArea) {
        insertNewArea(newArea, closestArea);
      }
    });

    imageViewerPanel.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        showPopup(e);
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
      textComponent.getDocument().removeDocumentListener(documentListener);
      textComponent.getDocument().addDocumentListener(documentListener);
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
      textComponent.getDocument().removeDocumentListener(documentListener);
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

    int x = imageScaleSupport.applyScale(Integer.parseInt(splits[0]));
    int y = imageScaleSupport.applyScale(Integer.parseInt(splits[1]));
    int lx = imageScaleSupport.applyScale(Integer.parseInt(splits[2]));
    int ly = imageScaleSupport.applyScale(Integer.parseInt(splits[3]));
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
    URL toOpen = null;
    WSEditorPage currentPage = null;
    if (currentEditorAccess != null) {
      currentPage = currentEditorAccess.getCurrentPage();
      if (currentPage instanceof WSXMLTextEditorPage) {
        String selectedImageToLoad = ((WSXMLTextEditorPage) currentPage).getSelectedText();
        if (selectedImageToLoad != null) {
          try {
            toOpen = new URL(currentEditorAccess.getEditorLocation(), selectedImageToLoad);
          } catch (MalformedURLException e1) {
            e1.printStackTrace();
          }
        }
      }
    }
    
    if (toOpen == null) {
      toOpen = pluginWorkspaceAccess.chooseURL("Image Chooser", new String[] {"jpeg", "jpg", "png"}, "Image files");
    }
    
    if (toOpen != null) {
      try {
        openImage(currentPage, toOpen);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (XPathException e) {
        e.printStackTrace();
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

    reloadAreas(currentPage);
  }

  /**
   * Reload the areas from the current page.
   * 
   * @param currentPage Current page.
   * 
   * @throws XPathException Unable to identify the areas.
   */
  private void reloadAreas(WSEditorPage currentPage) throws XPathException {
    if (currentPage == null) {
      WSEditor currentEditorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
          PluginWorkspace.MAIN_EDITING_AREA);
      if (currentEditorAccess != null) {
        currentPage = currentEditorAccess.getCurrentPage();
      }
    }
    
    if (currentPage instanceof WSXMLTextEditorPage) {
      Object[] evaluateXPath = ((WSXMLTextEditorPage) currentPage).evaluateXPath("for $zone in //zone return string-join(($zone/@ulx, $zone/@uly, $zone/@lrx, $zone/@lry), ',')");
      if (evaluateXPath != null && evaluateXPath.length > 0) {
        init(evaluateXPath);
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
  public void openImage(URL toOpen)
      throws IOException, XPathException {
    WSEditor currentEditorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(
        PluginWorkspace.MAIN_EDITING_AREA);
    if (currentEditorAccess != null) {
      WSEditorPage currentPage = currentEditorAccess.getCurrentPage();
      if (currentPage instanceof WSXMLTextEditorPage) {
        try {
          openImage(currentPage, toOpen);
          selectedImageToLoad = URLUtil.extractFileName(toOpen);
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

        // An action that duplicates the current area.
        AbstractAction duplicateAction = new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent arg0) {
            Rectangle closest = toProcess;
            Rectangle clone = (Rectangle) toProcess.clone();
            clone.translate(0, toProcess.height + 2);
            
            List<Rectangle> areas = new ArrayList<Rectangle>(decorator.getAreas());
            while (areas.contains(clone)) {
              closest = (Rectangle) clone.clone();
              clone.translate(0, toProcess.height + 2);
            }
            
            int panelHeight = imageViewerPanel.getHeight();
            // Make sure the area is inside the panel area.
            if (clone.y < panelHeight && (clone.y + clone.height) < panelHeight) {
              areas.add(clone);
              decorator.setAreas(areas);

              insertNewArea(clone, closest);
            }
          }
        };
        duplicateAction.putValue(Action.NAME, "Duplicate");
        popup.add(duplicateAction);
        
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
                disableSync(textEditorPage);
                try {
                  try {
                    String xpath = createXPath(toProcess);
                    WSXMLTextNodeRange[] ranges = textEditorPage.findElementsByXPath(xpath);

                    if (ranges != null && ranges.length > 0) {
                      WSXMLTextNodeRange range = ranges[0];
                      Document document = textEditorPage.getDocument();
                      int startOffset = textEditorPage.getOffsetOfLineStart(range.getStartLine()) + range.getStartColumn() - 1;
                      int endOffset = textEditorPage.getOffsetOfLineStart(range.getEndLine()) + range.getEndColumn() - 1;

                      document.remove(startOffset, endOffset - startOffset);
                    }
                  } catch (XPathException e) {
                    e.printStackTrace();
                  } catch (BadLocationException e) {
                    e.printStackTrace();
                  }
                } finally {
                  enableSync(textEditorPage);
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
    String xpath = 
        "//zone[@ulx='" + toProcess.x
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
    StringBuilder b = new StringBuilder("<zone");
    
    buildZoneAttrs(toProcess, b);
    
    b.append("/>");

    return b.toString();
  }

  /**
   * Builds the coordinates.
   * 
   * @param toProcess Rectangle to write the coordinates for.
   * @param b Builder to write them into.
   */
  private void buildZoneAttrs(final Rectangle toProcess, StringBuilder b) {
    int x = imageScaleSupport.getOriginal(toProcess.x);
    int y = imageScaleSupport.getOriginal(toProcess.y);
    int lrx = imageScaleSupport.getOriginal(toProcess.x + toProcess.width);
    int lry = imageScaleSupport.getOriginal(toProcess.y + toProcess.height);
    
    b.append(" ulx=\"").append(x).append("\"");
    b.append(" uly=\"").append(y).append("\"");
    b.append(" lrx=\"").append(lrx).append("\"");
    b.append(" lry=\"").append(lry).append("\"");
  }
  
  /**
   * Gets the replacement for a coordinates text.
   * 
   * @param newArea New coordinates.
   * @param text A coordinates text.
   * 
   * @return Replacement with the new value.
   */
  private String getReplacement(final Rectangle newArea, String text) {
    StringBuilder b = new StringBuilder();
    int x = imageScaleSupport.getOriginal(newArea.x);
    int y = imageScaleSupport.getOriginal(newArea.y);
    int lrx = imageScaleSupport.getOriginal(newArea.x + newArea.width);
    int lry = imageScaleSupport.getOriginal(newArea.y + newArea.height);
    
    if (text.startsWith("ulx")) {
      b.append("ulx=\"").append(x).append("\"");
    } else if (text.startsWith("uly")) {
      b.append("uly=\"").append(y).append("\"");
    } else if (text.startsWith("lrx")) {
      b.append("lrx=\"").append(lrx).append("\"");
    } else if (text.startsWith("lry")) {
      b.append("lry=\"").append(lry).append("\"");
    }
    
    return b.toString();
  }
  
  /**
   * Inserts a new area into the document.
   * 
   * @param newArea The new area.
   * @param closestArea The reference area. The new area will be inserted after this existing area.
   */
  private void insertNewArea(Rectangle newArea, Rectangle closestArea) {
    WSEditor editorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
    if (editorAccess != null) {
      WSEditorPage currentPage = editorAccess.getCurrentPage();
      if (currentPage instanceof WSXMLTextEditorPage) {
        final WSXMLTextEditorPage textEditorPage = (WSXMLTextEditorPage) currentPage;
        textEditorPage.beginCompoundUndoableEdit();
        
        disableSync(textEditorPage);
        try {
          // Search for the reference area.
          WSXMLTextNodeRange[] ranges = null;
          if (closestArea != null) {
            ranges = textEditorPage.findElementsByXPath(createXPath(closestArea));
          }
          if (ranges == null ||  ranges.length == 0) { 
            // Try a different approach.
            String string = "//*:graphic[@url='" + selectedImageToLoad + "']";
            ranges = textEditorPage.findElementsByXPath(string);  
          }

          if (ranges != null && ranges.length > 0) {
            WSXMLTextNodeRange range = ranges[0];
            int endOffset = textEditorPage.getOffsetOfLineStart(range.getEndLine()) + range.getEndColumn() - 1;

            Document document = textEditorPage.getDocument();
            // Inserts the new area.
            document.insertString(endOffset, buildZoneElement(newArea), null);
            document.insertString(endOffset, "\n", null);
            // The previous new line has indented the zone element. We should look for it again.
            WSXMLTextNodeRange[] newRanges = textEditorPage.findElementsByXPath(createXPath(newArea));
            final int startSelect = textEditorPage.getOffsetOfLineStart(newRanges[0].getStartLine()) + newRanges[0].getStartColumn() - 1;
            final int endSelect = textEditorPage.getOffsetOfLineStart(newRanges[0].getEndLine()) + newRanges[0].getEndColumn() - 1;
            // Select the newly inserted area.
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                textEditorPage.select(startSelect, endSelect);
              }
            });
          } else {
            System.err.println("Unable to get insert location.");
          }
        } catch (XPathException e) {
          e.printStackTrace();
        } catch (BadLocationException e) {
          e.printStackTrace();
        } finally {
          textEditorPage.endCompoundUndoableEdit();
          enableSync(textEditorPage);
        }
      }
    }
  }
  
  /**
   * Changes in the text page will be reflected in the image.
   * 
   * @param textEditorPage The text page to sync with.
   */
  private void enableSync(WSXMLTextEditorPage textEditorPage) {
    ((JTextComponent) textEditorPage.getTextComponent()).addCaretListener(caretListener);
    inhibit = false;
  }

  /**
   * No longer listens for changes in the text page.
   * 
   * @param textEditorPage The text page to inhibit notifications from.
   */
  private void disableSync(WSXMLTextEditorPage textEditorPage) {
    inhibit = true;
    ((JTextComponent) textEditorPage.getTextComponent()).removeCaretListener(caretListener);
  }
}
