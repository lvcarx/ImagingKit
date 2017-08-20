package hageldave.imagingkit.core;

/**
 * Basic interface for Pixel classes of {@link ImgBase} implementations.
 * <br>
 * An implementation of PixelBase stores a position and works like a pointer into the
 * data of an image.
 * An instance of PixelBase (or any implementation of it) is NOT the pixel value of an image at a
 * specific location and changing its position will not change that image.
 * Instead an instance is used to iterate the pixel values of an image. Such a pixel value can be 
 * retrieved and manipulated as long as the instance points to it.
 * 
 * @author hageldave
 * @since 2.0
 */
public interface PixelBase {

	/**
	 * Returns the alpha value of this pixel at its current position. 
	 * If the underlying image does not support alpha, 1.0 is returned.
	 * @return alpha value of this pixel with 0.0 as fully transparent and 1.0 as fully opaque.
	 * May exceed [0,1] range depending on implementation.
	 */
	public double a_asDouble();

	/**
	 * Returns the red value of this pixel at its current position.
	 * @return red value of this pixel with 0.0 as no red contribution and 1.0 as full red contribution.
	 * May exceed [0,1] range depending on implementation.
	 */
	public double r_asDouble();

	/**
	 * Returns the green value of this pixel at its current position.
	 * @return green value of this pixel with 0.0 as no green contribution and 1.0 as full green contribution.
	 * May exceed [0,1] range depending on implementation.
	 */
	public double g_asDouble();

	/**
	 * Returns the blue value of this pixel at its current position.
	 * @return blue value of this pixel with 0.0 as no blue contribution and 1.0 as full blue contribution.
	 * May exceed [0,1] range depending on implementation.
	 */
	public double b_asDouble();

	/**
	 * Sets the alpha value of this pixel at its current position with 0.0 as fully transparent and 1.0 as fully opaque.
	 * If the underlying image does not support alpha, this call imediately returns without modifying the image. 
	 * @param a the alpha value. May exceed [0,1] range depending on implementation.
	 * @return this pixel for chaining.
	 */
	public PixelBase setA_fromDouble(double a);

	/**
	 * Sets the red value of this pixel at its current position with 0.0 as no red contribution and 1.0 as full red contribution. 
	 * @param r the red value. May exceed [0,1] range depending on implementation.
	 * @return this pixel for chaining.
	 */
	public PixelBase setR_fromDouble(double r);

	/**
	 * Sets the green value of this pixel at its current position with 0.0 as no green contribution and 1.0 as full green contribution. 
	 * @param g the green value. May exceed [0,1] range depending on implementation.
	 * @return this pixel for chaining.
	 */
	public PixelBase setG_fromDouble(double g);

	/**
	 * Sets the blue value of this pixel at its current position with 0.0 as no blue contribution and 1.0 as full blue contribution. 
	 * @param b the blue value. May exceed [0,1] range depending on implementation.
	 * @return this pixel for chaining.
	 */
	public PixelBase setB_fromDouble(double b);

	/**
	 * Sets the alpha, red, green and blue value of this pixel.
	 * Assumed minimum and maximum value for each channel is 0.0 and 1.0
	 * but values may exceed [0,1] range depending on implementation.
	 * @param a alpha value (0=transparent, 1=opaque)
	 * @param r red value
	 * @param g green value
	 * @param b blue value
	 * @return this pixel for chaining.
	 */
	public default PixelBase setARGB_fromDouble(double a, double r, double g, double b){
		setA_fromDouble(a);
		setR_fromDouble(r);
		setG_fromDouble(g);
		setB_fromDouble(b);
		return this;
	}

	/**
	 * Sets the red, green and blue value of this pixel. 
	 * The alpha value is set to 1.0 (opaque) if the underlying image supports alpha.
	 * Assumed minimum and maximum value for each channel is 0.0 and 1.0
	 * but values may exceed [0,1] range depending on implementation.
	 * @param r red value
	 * @param g green value
	 * @param b blue value
	 * @return this pixel for chaining.
	 */
	public default PixelBase setRGB_fromDouble(double r, double g, double b){
		setA_fromDouble(1.0);
		setR_fromDouble(r);
		setG_fromDouble(g);
		setB_fromDouble(b);
		return this;
	}

	/**
	 * Sets the red, green and blue value of this pixel. 
	 * The alpha value is preserved.
	 * Assumed minimum and maximum value for each channel is 0.0 and 1.0
	 * but values may exceed [0,1] range depending on implementation.
	 * @param r red value
	 * @param g green value
	 * @param b blue value
	 * @return this pixel for chaining.
	 */
	public default PixelBase setRGB_fromDouble_preserveAlpha(double r, double g, double b){
		setR_fromDouble(r);
		setG_fromDouble(g);
		setB_fromDouble(b);
		return this;
	}

	/**
	 * @return the x coordinate of this pixel's current position
	 */
	public int getX();

	/**
	 * @return the y coordinate of this pixel's current position
	 */
	public int getY();

	/**
	 * Returns the normalized x coordinate of this Pixel.
	 * This will return 0 for Pixels at the left boundary and 1 for Pixels
	 * at the right boundary of the Img.<br>
	 * <em>For Img's that are only 1 Pixel wide, <u>NaN</u> is returned.</em>
	 * @return normalized x coordinate within [0..1]
	 */
	public default double getXnormalized(){
		return getX()*1.0/(getSource().getWidth()-1.0);
	}

	/**
	 * Returns the normalized y coordinate of this Pixel.
	 * This will return 0 for Pixels at the upper boundary and 1 for Pixels
	 * at the lower boundary of the Img.<br>
	 * <em>For Img's that are only 1 Pixel high, <u>NaN</u> is returned.</em>
	 * @return normalized y coordinate within [0..1]
	 * @since 1.2
	 */
	public default double getYnormalized(){
		return getY()*1.0/(getSource().getHeight()-1.0);
	}

	/**
	 * Returns this pixel's index. 
	 * The index relates to the pixels's position in the following way:<br>
	 * <pre>
	 * index = y * width + x
	 *     y = index / width
	 *     x = index % width
	 * </pre>
	 * @return index of this pixel
	 */
	public int getIndex();

	/**
	 * Sets the index of this pixel and thus its position.
	 * The index relates to the pixels's position in the following way:<br>
	 * <pre>
	 * index = y * width + x
	 *     y = index / width
	 *     x = index % width
	 * </pre>
	 * Index values outside the range [0, number of pixels] are not allowed
	 * but not necessarily enforced either. Accessing image data with an index/position
	 * outside of the underlying image's boundaries may cause an Exception.
	 * @param index to set the pixel to
	 * @return this pixel for chaining
	 */
	public PixelBase setIndex(int index);

	/**
	 * Sets the position of this pixel to the specified coordinates.
	 * Positions outside of the underlying images boundaries are not allowed 
	 * but not necessarily enforced either. Accessing image data with a position
	 * outside of the boundaries may cause an Exception.
	 * @param x coordinate (0 left most, width-1 right most)
	 * @param y coordinate (0 top most, height-1 bottom most)
	 * @return this pixel for chaining
	 */
	public PixelBase setPosition(int x, int y);

	/**
	 * Returns the underlying image of this pixel (the image referenced by this pixel)
	 * @return the image this pixel belongs to
	 */
	public ImgBase<?> getSource();

	/**
	 * Returns a String representation of this pixel at its current position.
	 * The returned String has the following pattern:<br>
	 * <em>Pixelclass</em>[a:<em>alpha</em> r:<em>red</em> g:<em>green</em> b:<em>blue</em>]@(<em>x</em>,<em>y</em>)
	 * @return string representation of this pixel at its current position
	 */
	public default String asString(){
		return String.format("%s[a:%.3f r:%.3f g:%.3f b:%.3f]@(%d,%d)",
				this.getClass().getSimpleName(),
				a_asDouble(),
				r_asDouble(),
				g_asDouble(),
				b_asDouble(),
				getX(),
				getY());
	}

}
