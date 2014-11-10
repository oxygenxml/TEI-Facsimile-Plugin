package com.oxygenxml.image.markup;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import ro.sync.exml.plugin.PluginDescriptor;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.editor.page.text.xml.XPathException;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.util.URLUtil;

/**
 * An workspace access extension that contributes a custom view. This view can 
 * load an image referred from the current editor. 
 * 
 * @author alex_jitianu
 */
public class ImageMarkupPluginExtension implements WorkspaceAccessPluginExtension {
  /**
   * A toolbar that adds an action that allows you to quickly test the plugin.
   */
  private static final String SAMPLE_TOOLBAR_ID = "image.markup.sample";
  /**
   * The panel that paints the image.
   */
  private ImageViewerPanel imageViewerPanel = new ImageViewerPanel();
  /**
   * Controller for keeping the image in sync with the editor.
   */
  private ImageController ctrl = new ImageController(imageViewerPanel);

  /**
   * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
   */
  @Override
  public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
    ctrl.init(pluginWorkspaceAccess);
    pluginWorkspaceAccess.addViewComponentCustomizer(new ViewComponentCustomizer() {
      /**
       * @see ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer#customizeView(ro.sync.exml.workspace.api.standalone.ViewInfo)
       */
      @Override
      public void customizeView(ViewInfo viewInfo) {
        if (ImageViewerPanel.IMAGE_VIEWER_ID.equals(viewInfo.getViewID())) {
          JPanel jPanel = new JPanel(new BorderLayout());

          JPanel northPanel = new JPanel(new GridBagLayout());

          JButton jButton = new JButton("Open selected");
          jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              ctrl.openSelected();
            }
          });
          northPanel.add(jButton);

          jPanel.add(northPanel, BorderLayout.NORTH);
          jPanel.add(imageViewerPanel.getPlaceholder(), BorderLayout.CENTER);

          viewInfo.setComponent(jPanel);
        }
      }
    });
    
    pluginWorkspaceAccess.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
      @Override
      public void customizeToolbar(ToolbarInfo toolbarInfo) {
        if (SAMPLE_TOOLBAR_ID.equals(toolbarInfo.getToolbarID())) {
          Action openSampleAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
              PluginDescriptor descriptor = ImageMarkupPlugin.getInstance().getDescriptor();
              File sampleFile = new File(descriptor.getBaseDir(), "etc/facsimile.xml");
              
              try {
                boolean open = pluginWorkspaceAccess.open(URLUtil.correct(sampleFile));
                if (open) {
                  pluginWorkspaceAccess.showView(ImageViewerPanel.IMAGE_VIEWER_ID, false);
                  URL imageURL = URLUtil.correct(new File(sampleFile.getParentFile(), "Bovelles-49r.png"));
                  ctrl.openImage(imageURL);
                }
              } catch (MalformedURLException e) {
                e.printStackTrace();
              } catch (IOException e) {
                e.printStackTrace();
              } catch (XPathException e) {
                e.printStackTrace();
              }
            }
          };
          ToolbarButton sampleButton = new ToolbarButton(openSampleAction, true);
          sampleButton.setText("Image Markup Sample");
          
          toolbarInfo.setComponents(new JComponent[] {sampleButton});
        }
      }
    });
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
}