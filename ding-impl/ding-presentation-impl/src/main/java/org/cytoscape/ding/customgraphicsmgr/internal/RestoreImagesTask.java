package org.cytoscape.ding.customgraphicsmgr.internal;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.ding.customgraphics.AbstractDCustomGraphics;
import org.cytoscape.ding.customgraphics.CustomGraphicsManager;
import org.cytoscape.ding.customgraphics.Taggable;
import org.cytoscape.ding.customgraphics.image.BitmapCustomGraphics;
import org.cytoscape.ding.customgraphics.image.SVGCustomGraphics;
import org.cytoscape.ding.customgraphics.vector.GradientOvalLayer;
import org.cytoscape.ding.customgraphics.vector.GradientRoundRectangleLayer;
import org.cytoscape.ding.customgraphicsmgr.internal.event.CustomGraphicsLibraryUpdatedEvent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2020 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

public class RestoreImagesTask implements Task {

	private final CustomGraphicsManager manager;

	private final ExecutorService imageLoaderService;

	private static final int TIMEOUT = 1000;
	private static final int NUM_THREADS = 8;

	private static final String METADATA_FILE = "image_metadata.props";

	private File imageHomeDirectory;
	private final Set<URL> defaultImageURLs;

	private final CyServiceRegistrar serviceRegistrar;
	
	// For image I/O, PNG is used as bitmap image format.
	private static final String PNG_EXT = ".png";
	private static final String SVG_EXT = ".svg";

	// Default vectors
	private static final Set<Class<?>> DEF_VECTORS = new HashSet<>();
	private static final Set<String> DEF_VECTORS_NAMES = new HashSet<>();
	
	private static final Logger logger = LoggerFactory.getLogger(CyUserLog.NAME);

	static {
		DEF_VECTORS.add(GradientRoundRectangleLayer.class);
		DEF_VECTORS.add(GradientOvalLayer.class);
		
		for (var cls : DEF_VECTORS)
			DEF_VECTORS_NAMES.add(cls.getName());
	}
	
	public RestoreImagesTask(
			Set<URL> defaultImageURLs,
			File imageLocation,
			CustomGraphicsManager manager,
			CyServiceRegistrar serviceRegistrar
	) {
		this.manager = manager;
		this.serviceRegistrar = serviceRegistrar;

		// For loading images in parallel.
		this.imageLoaderService = Executors.newFixedThreadPool(NUM_THREADS);
		this.imageHomeDirectory = imageLocation;
		this.defaultImageURLs = defaultImageURLs;
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {		
		tm.setTitle("Load Image Library");
		tm.setStatusMessage("Loading image library from local disk...");
		tm.setProgress(0.0);

		long startTime = System.currentTimeMillis();

		restoreImages();
		restoreSampleImages();

		long endTime = System.currentTimeMillis();
		double sec = (endTime - startTime) / (1000.0);
		logger.info("Image saving process finished in " + sec + " sec.");
		
		serviceRegistrar.getService(CyEventHelper.class).fireEvent(new CustomGraphicsLibraryUpdatedEvent(manager));
	}
	
	private void restoreSampleImages() throws IOException {
		// Filter by display name
		var allGraphics = manager.getAllCustomGraphics();
		var names = new HashSet<String>();

		for (var cg : allGraphics)
			names.add(cg.getDisplayName());
		
		for (var url : defaultImageURLs) {
			var parts = url.getFile().split("/");
			var displayName = parts[parts.length - 1];

			if (manager.getCustomGraphicsBySourceURL(url) == null && !names.contains(displayName)) {
				var cg = new BitmapCustomGraphics(manager.getNextAvailableID(), displayName, url);

				if (cg != null) {
					manager.addCustomGraphics(cg, url);
					cg.setDisplayName(displayName);
				}
			}
		}
	}

	private void restoreImages() {
		var cs = new ExecutorCompletionService(imageLoaderService);
		imageHomeDirectory.mkdir();

		long startTime = System.currentTimeMillis();

		// Load metadata first.
		var prop = new Properties();
		
		try {
			prop.load(new FileInputStream(new File(imageHomeDirectory, METADATA_FILE)));
			logger.info("Custom Graphics Image property file loaded from: " + imageHomeDirectory);
		} catch (Exception e) {
			logger.info("Custom Graphics Metadata was not found. (This is normal for the first time.)");
			// Restore process is not necessary.
			return;
		}

		if (imageHomeDirectory != null && imageHomeDirectory.isDirectory()) {
			var imageFiles = imageHomeDirectory.listFiles();
			var nameMap = new HashMap<Future<?>, String>();
			var idMap = new HashMap<Future<?>, Long>();
			var urlMap = new HashMap<Future<?>, URL>();
			var tagMap = new HashMap<Future<?>, Set<String>>();
			
			var validFiles = new HashSet<File>();
			
			for (var file : imageFiles) {
				if (!isSupportedImageFile(file))
					continue;

				var fileName = file.getName();
				var key = fileName.split("\\.")[0];
				var value = prop.getProperty(key);
				
				// Filter unnecessary files.
				if (value == null ||
						(!value.contains(BitmapCustomGraphics.TYPE_NAME)
								&& !value.contains(SVGCustomGraphics.TYPE_NAME)))
					continue;
				
				var imageProps = value.split(",");
				
				if (imageProps == null || imageProps.length < 2)
					continue;

				var name = imageProps[2];
				
				if (name.contains("___"))
					name = name.replace("___", ",");

				URL url = null;
				
				try {
					url = file.toURI().toURL();
				} catch (MalformedURLException e) {
					logger.error("Cannot create URL from image file " + file, e);
					continue;
				}
				
				var task = isPNG(file) ? new LoadPNGImageTask(url) : new LoadSVGImageTask(url);
				var future = cs.submit(task);
				
				validFiles.add(file);
				nameMap.put(future, name);
				idMap.put(future, Long.parseLong(imageProps[1]));
				urlMap.put(future, url);

				String tagStr = null;
				
				if (imageProps.length > 3) {
					tagStr = imageProps[3];
					var tags = new TreeSet<String>();
					var tagParts = tagStr.split("\\" + AbstractDCustomGraphics.LIST_DELIMITER);
					
					for (var tag : tagParts)
						tags.add(tag.trim());

					tagMap.put(future, tags);
				}
			}
			
			for (var entry : urlMap.entrySet()) {
				try {
					var future = cs.take();
					var image = future.get();

					if (!(image instanceof BufferedImage || image instanceof String))
						continue;

					var id = idMap.get(future);
					var name = nameMap.get(future);
					
					URL url = null;
					
					try {
						// Try to use the image name as URL whenever possible, because the name is the only parameter
						// that contains the original URL from passthrough mappings.
						// Otherwise, every time a session is loaded, all the passthrough images will be
						// downloaded and cached again, creating duplicates.
						url = new URL(name);
					} catch (MalformedURLException me) {
						url = urlMap.get(future);
					}
					
					var cg = image instanceof BufferedImage
							? new BitmapCustomGraphics(id, name, url, (BufferedImage) image)
							: new SVGCustomGraphics(id, name, url, (String) image);

					if (cg instanceof Taggable && tagMap.get(future) != null)
						((Taggable) cg).getTags().addAll(tagMap.get(future));
					
					if (url != null)
						manager.addCustomGraphics(cg, url);
				} catch (Exception e) {
					logger.error("Cannot load image file " + entry.getValue(), e);
				}
			}
		}

		try {
			imageLoaderService.shutdown();
			imageLoaderService.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long endTime = System.currentTimeMillis();
		double sec = (endTime - startTime) / (1000.0);
		logger.info("Image loading process finished in " + sec + " sec.");
		logger.info("Currently,  " + (manager.getAllCustomGraphics().size() - 1) + " images are available.");
	}

	@Override
	public void cancel() {
	}

	private boolean isSupportedImageFile(File file) {
		return isPNG(file) || isSVG(file);
	}
	
	private boolean isPNG(File file) {
		var name = file.getName().toLowerCase();
		
		return name.endsWith(PNG_EXT);
	}
	
	private boolean isSVG(File file) {
		var name = file.getName().toLowerCase();
		
		return name.endsWith(SVG_EXT);
	}
	
	private final class LoadPNGImageTask implements Callable<BufferedImage> {

		private final URL url;

		public LoadPNGImageTask(URL url) {
			if (url == null)
				throw new IllegalStateException("URL string cannot be null.");
			
			this.url = url;
		}

		@Override
		public BufferedImage call() throws Exception {
			return ImageIO.read(url);
		}
	}
	
	private final class LoadSVGImageTask implements Callable<String> {
		
		private final URL url;
		
		public LoadSVGImageTask(URL url) {
			if (url == null)
				throw new IllegalStateException("URL string cannot be null.");
			
			this.url = url;
		}
		
		@Override
		public String call() throws Exception {
		    try (var reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
			    var sb = new StringBuilder();
			    String line;
				
				while ((line = reader.readLine()) != null) {
		            sb.append(line);
		            sb.append("\n");
		        }
				
				return sb.toString();
		    } catch (Exception e) {
		    	throw e;
		    }
		}
	}
}
