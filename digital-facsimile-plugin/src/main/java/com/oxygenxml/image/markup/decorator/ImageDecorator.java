package com.oxygenxml.image.markup.decorator;

import java.awt.Graphics;

public interface ImageDecorator {
	/**
	 * Decorates the component
	 * 
	 * @param g Graphics to paint into.
	 */
	public void paint(Graphics g);

	/**
	 * Clean all decorating information.
	 */
	public void clean();
}
