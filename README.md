TEI-Facsimile-Plugin
====================

A plugin that provides support for working with  Digital Facsimiles in Text Encoding Initiative (TEI) vocabulary.   The plugin contributes a new View in which the user can load an image and draw shapes. These shapes are then  converted into TEI "zone" elements. All the existing "zone" elements from the document are also rendered over the image.

How to install it
--------------------
1. Unzip  *builds/addon/image-markup-plugin-1.0.0-SNAPSHOT-plugin.zip* inside *{OxygenInstallDir}/plugins/*. Please make sure you are not creating any additional directories. After unziping, the directory structure should look like this:

    plugins<br />
    &nbsp;&nbsp;&nbsp;&nbsp;image-markup-plugin-1.0.0-SNAPSHOT-plugin<br />
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;lib<br />
    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;plugin.xml <br />


2. The plugin is also packed as an add-on so you can install it using the Oxygen add-ons support. Depending on the Oxygen version you are using, the procedure might differ but the update site where this add-on is published is this: 

https://github.com/oxygenxml/TEI-Facsimile-Plugin/raw/master/addon/image-markup-plugin.xml


How to quickly test it
--------------------
After installing it, you should see an *Image Markup Sample* button  on the toolbar. Click it to launch a sample file and to initialize the *Image-Markup* view with an image.

1. Adding zones/rectangles
- If you place yourself in a *zone* element, that rectangle will be painted with a different color in the view
- If you right click on a rectangle in image view, you can either delete it (and will reflect in the editor) or copy it
- You can use the mouse to draw a new rectangle over the image. Then right click and copy its coordinates to paste them in the editor.
- there is now Zoom support. There are buttons on the toolbar and you can also press CTRL and use the mouse scroll wheel.
- a rectangle/zone can be resized. It means that you can grab an existing rectangle by one of its corners and resize it.
- you can duplicate an existing rectangle (there is a Duplicate action in the contextual menu presented over a rectangle, in the view)

2. A seamless integration between the document and the view
- for every new rectangle drawn in the view, a new <zone> element will be automatically inserted in the document.
- for every rectangle resized in the view, the corresponding <zone> element will be automatically updated in the document.
- every change in the document will determine the view to automatically reload all the zones

3. Linking a zone with existing transcribed text elements
There is a Copy/Generate ID action in the contextual menu presented for an area (in the image view). What this action does is:
1. if the zone doesn't have an ID it will generate one. The pattern is read from the configuration file etc/id_pattern.txt and it accepts Oxygen editor variables.
2. copy #id to clipboard
The idea is that after invoking this action you will go on an element and just paste the value inside an @facs.


How to use it 
--------------------
1. Go to *Window->Show View* and click on *Image-Markup*
2. Open a sample file
3. Select an image location in the editor and press *Open Selected* in the view. For example if you have a 

	&lt;graphic url="Bovelles-49r.png"/&gt;
	
you should select *Bovelles-49r.png* and press *Open Selected* in the view.
