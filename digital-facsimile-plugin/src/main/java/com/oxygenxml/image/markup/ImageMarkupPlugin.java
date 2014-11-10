package com.oxygenxml.image.markup;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

public class ImageMarkupPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static ImageMarkupPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor
   */
  public ImageMarkupPlugin(PluginDescriptor descriptor) {
    super(descriptor);

    if (instance != null) {
      throw new IllegalStateException("Already instantiated!");
    }
    instance = this;
  }

  /**
   * Get the plugin instance.
   * 
   * @return the shared plugin instance.
   */
  public static ImageMarkupPlugin getInstance() {
    return instance;
  }
}