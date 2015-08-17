package com.oxygenxml.tei.standoff;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ro.sync.exml.editor.EditorPageConstants;
import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.PluginWorkspace;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.xml.WSXMLTextNodeRange;
import ro.sync.exml.workspace.api.editor.page.text.xml.XPathException;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;
import ro.sync.exml.workspace.api.standalone.ToolbarComponentsCustomizer;
import ro.sync.exml.workspace.api.standalone.ToolbarInfo;
import ro.sync.exml.workspace.api.standalone.ui.ToolbarButton;
import ro.sync.xml.parser.ParserCreator;

/**
 * A proof of concept on how to create a standoff plugin.
 */
public class StandoffPluginExtension implements WorkspaceAccessPluginExtension {
	/**
	 * A toolbar that adds an action that allows you to quickly test the plugin.
	 */
	private static final String SAMPLE_TOOLBAR_ID = "standoff.tei.plugin";
	private StandalonePluginWorkspace pluginWorkspaceAccess;

	/**
	 * @see ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension#applicationStarted(ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace)
	 */
	@Override
	public void applicationStarted(final StandalonePluginWorkspace pluginWorkspaceAccess) {
		this.pluginWorkspaceAccess = pluginWorkspaceAccess;
		pluginWorkspaceAccess.addToolbarComponentsCustomizer(new ToolbarComponentsCustomizer() {
			@Override
			public void customizeToolbar(ToolbarInfo toolbarInfo) {
				if (SAMPLE_TOOLBAR_ID.equals(toolbarInfo.getToolbarID())) {
					Action openSampleAction = new AbstractAction() {
						@Override
						public void actionPerformed(ActionEvent arg0) {
							mark();
						}
					};
					ToolbarButton sampleButton = new ToolbarButton(openSampleAction, true);
					sampleButton.setText("Mark");

					toolbarInfo.setComponents(new JComponent[] {sampleButton});
				}
			}
		});
	}


	private void mark() {
	  WSEditor currentEditorAccess = pluginWorkspaceAccess.getCurrentEditorAccess(PluginWorkspace.MAIN_EDITING_AREA);
	  if (
	      currentEditorAccess.getCurrentPageID().equals(EditorPageConstants.PAGE_TEXT)
	      && currentEditorAccess.getCurrentPage() instanceof WSXMLTextEditorPage) {
	    final WSXMLTextEditorPage editorPage = (WSXMLTextEditorPage) currentEditorAccess.getCurrentPage();

	    String selectedText = editorPage.getSelectedText();
	    if (selectedText != null) {
	      Element[] selectedNodes = parse(selectedText);
	      if (selectedNodes != null) {
	        try {
	          WSXMLTextNodeRange[] elems = editorPage.findElementsByXPath("//*:standoff");
	          if (elems != null && elems.length > 0) {
	            int startOffset = editorPage.getOffsetOfLineStart(elems[0].getStartLine()) + elems[0].getStartColumn();
	            int endOffset = editorPage.getOffsetOfLineStart(elems[0].getEndLine()) + elems[0].getEndColumn();
	            String standofftext = editorPage.getDocument().getText(startOffset, endOffset - startOffset);
	            final int insertIndex = startOffset + standofftext.indexOf(">") + 1; 
	            if (selectedNodes.length == 1) {
	              // One node selected.
	              final String insert = "<hi stf_target=\"#" + selectedNodes[0].getAttribute("xml:id")
	                  + "\" rend=\"test\"/>";
                editorPage.getDocument().insertString(insertIndex, insert, null);
                editorPage.select(insertIndex, insertIndex + insert.length());
	            } else {
	              // Multiple nodes selected.
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
	}

	private Element[] parse(String selectedText) {
	  List<Element> toReturn = new ArrayList<Element>();
	  
	  try {
      DocumentBuilder builder = ParserCreator.newDocumentBuilderFakeResolver();
      // If the user has selected multiple elements we need a root.
      InputSource is = new InputSource(new StringReader("<root>" + selectedText + "</root>"));
      Document parse = builder.parse(is);
      NodeList childNodes = parse.getDocumentElement().getChildNodes();
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node item = childNodes.item(i);
        if (item.getNodeType() == Node.ELEMENT_NODE) {
          toReturn.add((Element) item);
        }
      }
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
	  
	  return toReturn.isEmpty() ? null : toReturn.toArray(new Element[0]);
  }


  @Override
	public boolean applicationClosing() {
		return true;
	}
}