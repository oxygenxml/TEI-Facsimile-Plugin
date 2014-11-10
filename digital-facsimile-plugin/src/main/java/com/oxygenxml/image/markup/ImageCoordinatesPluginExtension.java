package com.oxygenxml.image.markup;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ViewComponentCustomizer;
import ro.sync.exml.workspace.api.standalone.ViewInfo;

/**
 * An workspace access extension that contributes a custom view. This view can 
 * load an image referred from the current editor. 
 * 
 * @author alex_jitianu
 */
public class ImageCoordinatesPluginExtension implements WorkspaceAccessPluginExtension {
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
  }

  @Override
  public boolean applicationClosing() {
    return true;
  }
}