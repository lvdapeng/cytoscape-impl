package org.cytoscape.ding.customgraphics.image;

import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.cytoscape.ding.customgraphics.paint.TexturePaintFactory;
import org.cytoscape.ding.internal.util.ImageUtil;

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

/**
 * Creates bitmap images from URLs or Base64 Data URLs (e.g. "data:image/jpeg;base64,LzlqLzRBQ...").
 */
public class BitmapCustomGraphics extends AbstractURLImageCustomGraphics<BitmapLayer> {

	// DO NOT change, or you can break saving/restoring image_metadata.props!
	public static final String TYPE_NAMESPACE = "org.cytoscape.ding.customgraphics.bitmap";
	// DO NOT change, or you can break saving/restoring image_metadata.props!
	public static final String TYPE_NAME = "URLImageCustomGraphics";
	
	private static final String DEF_TAG = "bitmap image";
	private static final String DEF_IMAGE_FILE = "/images/no_image.png";

	private BufferedImage originalImage;
	private BufferedImage scaledImage;
	
	static BufferedImage DEF_IMAGE;
	
	static {
		try {
			DEF_IMAGE = ImageIO.read(BitmapCustomGraphics.class.getClassLoader().getResource(DEF_IMAGE_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BitmapCustomGraphics(Long id, String name, URL url) {
		super(id, name, url);
		
		tags.add(DEF_TAG);
		createImage();
		buildCustomGraphics(originalImage);
	}

	public BitmapCustomGraphics(Long id, String name, BufferedImage img) {
		super(id, name);

		if (img == null)
			throw new IllegalArgumentException("Image cannot be null.");

		fitRatio = DEF_FIT_RATIO;

		if (displayName.startsWith("bundle:")) {
			int index = displayName.lastIndexOf("/");
			displayName = displayName.substring(index + 1);
		}

		tags.add(DEF_TAG);
		this.originalImage = img;
		buildCustomGraphics(originalImage);
	}
	
	public BitmapCustomGraphics(Long id, String name, URL url, BufferedImage img) {
		this(id, name, img);
		
		sourceUrl = url;
	}
	
	@Override
	public String getTypeNamespace() {
		// This way, we can refactor this class package without breaking the serialization and backwards compatibility.
		return TYPE_NAMESPACE;
	}
	
	@Override
	public String getTypeName() {
		// This way, we can refactor this class package without breaking the serialization and backwards compatibility.
		return TYPE_NAME;
	}

	@Override
	public Image getRenderedImage() {
		if (width == originalImage.getWidth() && height == originalImage.getHeight())
			return originalImage;

		if (scaledImage == null || scaledImage.getWidth() != width || scaledImage.getHeight() != height)
			resizeImage(width, height);
		
		return scaledImage;
	}
	
	public BufferedImage getOriginalImage() {
		return originalImage;
	}

	public Image resetImage() {
		if (scaledImage != null) {
			scaledImage.flush();
			scaledImage = null;
		}
		
		buildCustomGraphics(originalImage);
		
		return originalImage;
	}
	
	private void buildCustomGraphics(BufferedImage targetImg) {
		layers.clear();

		width = targetImg.getWidth();
		height = targetImg.getHeight();

		var bound = new Rectangle2D.Double(-width / 2, -height / 2, width, height);
		var paintFactory = new TexturePaintFactory(targetImg);

		var cg = new BitmapLayer(bound, paintFactory);
		layers.add(cg);
	}

	private void createImage() {
		try {
			originalImage = ImageIO.read(getSourceURL());
		} catch (Exception e) {
			originalImage = DEF_IMAGE;
		}

		if (originalImage == null)
			originalImage = DEF_IMAGE;
	}
	
	private Image resizeImage(int width, int height) {
		var img = originalImage.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
		
		try {
			scaledImage = ImageUtil.toBufferedImage(img);
		} catch (InterruptedException e) {
			// Could not get scaled one
			e.printStackTrace();
			return originalImage;
		}
		
		buildCustomGraphics(scaledImage);
		
		return scaledImage;
	}
	
	@Override
	public int hashCode() {
		int prime = 31;
		int result = 11;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof BitmapCustomGraphics))
			return false;
		
		var other = (BitmapCustomGraphics) obj;
		
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id)) {
			return false;
		}
		
		return true;
	}
}
