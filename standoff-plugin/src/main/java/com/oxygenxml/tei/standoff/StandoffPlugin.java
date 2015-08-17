package com.oxygenxml.tei.standoff;

import ro.sync.exml.plugin.Plugin;
import ro.sync.exml.plugin.PluginDescriptor;

public class StandoffPlugin extends Plugin {
  /**
   * The static plugin instance.
   */
  private static StandoffPlugin instance = null;

  /**
   * Constructs the plugin.
   * 
   * @param descriptor The plugin descriptor
   */
  public StandoffPlugin(PluginDescriptor descriptor) {
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
  public static StandoffPlugin getInstance() {
    return instance;
  }
}