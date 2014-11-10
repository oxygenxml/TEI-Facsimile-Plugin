TEI-Facsimile-Plugin
====================

A plugin that provides support for working with  Digital Facsimiles in Text Encoding Initiative (TEI) vocabulary.   The plugin contributes a new View in which the user can load an image and draw shapes. These shapes are then  converted into TEI "zone" elements. All the existing "zone" elements from the document are also rendered over the image.

The plugin is packed as an add-on so you can install it using the Oxygen add-ons support. Depending on the Oxygen version you are using, the procedure might differ but the update site where this add-on is published is this: 

https://github.com/oxygenxml/TEI-Facsimile-Plugin/blob/master/addon/image-markup-plugin.xml

Another possibility is to just unzip the following archive inside {OxygenInstallDir}/plugins/.

https://github.com/oxygenxml/TEI-Facsimile-Plugin/blob/master/addon/image-markup-plugin-1.0.0-SNAPSHOT-plugin.jar

Please make sure you are not creating any additional directories. After unziping, the directory structure should look like this:

    plugins
    --image-markup-plugin-1.0.0-SNAPSHOT-plugin
    ---lib
    ---plugin.xml 
