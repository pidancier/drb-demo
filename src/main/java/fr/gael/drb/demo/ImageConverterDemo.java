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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.media.jai.RenderedImageList;
import javax.media.jai.widget.ImageCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import fr.gael.drb.DrbFactory;
import fr.gael.drb.DrbNode;
import fr.gael.drb.impl.DrbFactoryResolver;
import fr.gael.drbx.cortex.DrbCortexItemClass;
import fr.gael.drbx.cortex.DrbCortexMetadataResolver;
import fr.gael.drbx.cortex.DrbCortexModel;
import fr.gael.drbx.image.ImageFactory;
import fr.gael.drbx.image.jai.RenderingFactory;

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
         System.err.println ("   where command is \"show\", or the expected format");
         System.err.println ("Example: ImageConverterDemo show /data/alos/product/PSMXXX");
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
      
      ImageConverterDemo icd = new ImageConverterDemo();
      
      // Show the image in a frame
      if ("show".equals(command))
      {
         BufferedImage image = icd.convertRenderedImgToBuffImg (input_image);
         ReaderShow window = new ReaderShow(image);
         window.init();
         window.setVisible(true);
      }
      else
      {
         for (int image_index=0; image_index<input_list.size(); image_index++)
         {
            File file = File.createTempFile("converter", 
               "-" + (image_index+1) + "." + command, 
               new File ("."));
            
            logger.info("Saving file " + file.getAbsolutePath());
            
            ImageIO.write((RenderedImage)input_list.get(image_index),command,file);
         }
      }
   }
   
   /**
    * This class is a sample code of  a frame to display a BufferedImage
    */
   static class ReaderShow extends JFrame 
   {
      private static final long serialVersionUID = -3628472241640714476L;
      BufferedImage image;
      public ReaderShow(BufferedImage image)
      {
         this.image = image;
      }
      
      public void init ()
      {
         final JPanel addPanel = new JPanel();
         addPanel.setLayout( null);
         addPanel.setBounds( 100, 100, 800, 600 );
         getContentPane().add(addPanel);

         final ImageCanvas canvas = new ImageCanvas(this.image);
         canvas.setBounds(0, 0, 800, 600);

         final Graphics g = this.image.createGraphics();
         final int imageW = addPanel.getWidth();
         final int imageH = addPanel.getHeight();
         g.drawImage(this.image, 0, 0, imageW, imageH, null, null);

         // JFrame settings
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         addPanel.add(canvas);
         setSize(1024, 768);
         setLocationByPlatform(true);
      }
      
   }
   
   public BufferedImage convertRenderedImgToBuffImg(final RenderedImage image)
   {
      if (image instanceof BufferedImage)
         return (BufferedImage) image;

      final ColorModel cm = image.getColorModel();
      final int width = image.getWidth();
      final int height = image.getHeight();
      final WritableRaster raster = cm.createCompatibleWritableRaster(width, height);
      final boolean is_alpha = cm.isAlphaPremultiplied();
      final Hashtable<String, Object> properties = new Hashtable<String, Object>();
      final String[] keys = image.getPropertyNames();
      if (keys != null)
      {
         for (int i = 0; i < keys.length; i++)
         {
            properties.put(keys[i], image.getProperty(keys[i]));
         }
      }
      final BufferedImage result=new BufferedImage(cm, raster, is_alpha, properties);
      image.copyData(raster);
      return result;
   }
}
