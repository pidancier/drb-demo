/**
 * Copyright 2015, GAEL Consultant
 * 25 rue Alfred Nobel,
 * Parc Descartes Nobel, F-77420 Champs-sur-Marne, France
 * (tel) +33 1 64 73 99 55, (fax) +33 1 64 73 51 60
 * Contact: info@gael.fr
 * 
 * Gael Consultant Proprietary - Delivered under License Agreement.
 * Copying and Disclosure Prohibited Without Express Written 
 * Permission From Gael Consultant.
 * 
 * Author        : Frédéric PIDANCIER (frederic.pidancier@gael.fr)
 * Creation date : 18 déc. 2015 - 23:11:13 
 * 
 */
package fr.gael.drb.demo;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedImageList;

import org.apache.log4j.Logger;

import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.DrbFactoryResolver;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexMetadataResolver;
import fr.gael.drbx.cortex.DrbCortexModel;
import fr.gael.drbx.image.ImageFactory;
import fr.gael.drbx.image.jai.RenderingFactory;

/**
 * Sample class to demonstrates the easy usage of Drb/DrbxCortex/DrvxImage 
 * modules to manipulates huge images from space.
 */
public class ImageConverterDemo
{
   private static Logger logger = Logger.getLogger(ImageConverterDemo.class);
   // Lazy initialization of DRB with cortex resolver.
   static
   {
      try
      {
         DrbFactoryResolver.
            setMetadataResolver(new DrbCortexMetadataResolver());
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
   }
   public static void main(String[] args) throws IOException
   {
      if (args.length < 2)
      {
         System.err.println ("Missing arguments: <command> <product>");
         System.err.println ("   where 'command' is the expected format");
         System.err.println ("Example: ImageConverterDemo png  /data/alos/product/PSMXXX");
         System.err.println ("   or    ImageConverterDemo tiff /data/alos/product/PSMXXX");
         System.exit(1);
      }
      
      // Remove all extra messages.
      System.setProperty("com.sun.media.jai.disableMediaLib", "true");
      System.setProperty ("net.sf.ehcache.skipUpdateCheck", "true");
      System.setProperty ("org.terracotta.quartz.skipUpdateCheck", "true");

      
      String command = new String(args[0]);
      String data = new String(args[1]);
      
      DrbNode node = DrbFactory.openURI(data);
      
      try
      {
         DrbCortexModel model = DrbCortexModel.getDefaultModel();
         DrbCortexItemClass cl = model.getClassOf(node);
         logger.info("Product " + data + " recognized as \"" + 
            cl.getLabel() +"\".");
      }
      catch (Exception e)
      {
         logger.error ("Cortex error.", e);
      }
      
      // Request image from drb-image
      RenderedImageList input_list = null;
      RenderedImage input_image = null;
      try
      {
         /**
          * ImageFactory retrieve the images element from DrbCortex definition
          * of the product type. One product type can contains a collection
          * of images and, if defined a rendered image. 
          * The collection of images is the real images ectracted from the data.
          * According to the query extraction, no modification is applied on it.
          * It is declared as followed in the cortex OWL definition:
          * 
          * <img:descriptor rdf:parseType="Literal">
          *    <img:raster xmlns:img="http://www.gael.fr/drb/image">
          *       <img:source>
          *          (*[matches(name(),"IMG-(H|V){2}-ALPSR.*")])[1]/sarDataFile
          *       </img:source>
          *       <img:width>
          *          dataFileDescriptor/sarRelatedData/totalNumberOfDataGroups
          *       </img:width>
          *       <img:height>
          *           dataFileDescriptor/sarRelatedData/numberOfLinesPerDataSet
          *       </img:height>
          *       <img:bandNumber>2</img:bandNumber>
          *       <img:data sampleModel="pixelInterleaved">
          *          data(signalData/signalDataRecord/sarSignalData)
          *       </img:data>
          *    </img:raster>
          * </img:descriptor>
          * 
          * The rendered image is usually used for display, organizing 
          * spectral bands to create real color outputs, or modifying color 
          * depth to be supported by modern display systems, Crossing band
          * to display specific information (PVI, or complex images formatting).
          * A sample of a renderer (extracted from alos description):
          * 
          * <img:rendering rdf:parseType="Literal">
          *    <img:operator name="alosL1CGeoTif" xmlns:img="&img;">
          *       <img:script language="beanshell" version="1.0">
          *        rgb = JAI.create("bandSelect", source, new int[] {0});
          *        rgb = JAI.create("scale", rgb, 0.25f, 0.25f);
          *        rgb = JAI.create("normalize", rgb);
          *        return JAI.create("bandSelect", rgb, new int[] {0});
          *       </img:script>
          *    </img:operator>
          * </img:rendering>
          * 
          * Do not hesitate to look into the gael's cortex topics distributions 
          * to find out much more examples.
          */
         input_list = ImageFactory.createImage (node);
         input_image = RenderingFactory.createDefaultRendering(input_list);
      }
      catch (Exception e)
      {
         logger.debug ("Cannot retrieve default rendering");
         if (logger.isDebugEnabled ())
         {
            logger.debug ("Error occurs during rendered image reader", e);
         }
         
         if (input_list == null)
            throw new RuntimeException("No image retieve in input product "+data);
         input_image = input_list;
      }
      
      // Generates all the images not rendered
      for (int image_index=0; image_index<input_list.size(); image_index++)
      {
         File file = File.createTempFile("image", 
            "-" + (image_index+1) + "." + command, 
            new File ("."));
         
         logger.info("Saving file " + file.getAbsolutePath());
         
         ImageIO.write((RenderedImage)input_list.get(image_index),command,file);
      }
      
      // Also generates the rendered image
      if (input_image!=null)
      {
         File file=File.createTempFile("rendered", "."+command,new File("."));
         logger.info("Saving file " + file.getAbsolutePath());
         ImageIO.write(input_image,command,file);
      }
   }
}
