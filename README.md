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

Supported features:

- If you place yourself in a *zone* element, that rectangle will be painted with a different color in the view
- If you right click on a rectangle in image view, you can either delete it (and will reflect in the editor) or copy it
- You can use the mouse to draw a new rectangle over the image. Then right click and copy its coordinates to paste them in the editor.

Known limitiations (which can be addressed with some Java skills and free time):

- All zone elements are being rendered. Ideally only the zone elements from the surface element with the loaded image should be renderered.
- New zone elements that are manually inserted in the document will not be rendered automatically. You'll have to reload the image.


How to use it 
--------------------
1. Go to *Window->Show View* and click on *Image-Markup*
2. Open a sample file
3. Select an image location in the editor and press *Open Selected* in the view. For example if you have a 

	&lt;graphic url="Bovelles-49r.png"/&gt;
	
you should select *Bovelles-49r.png* and press *Open Selected* in the view.
