/*
 * Copyright 2017 David Haegele
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package hageldave.imagingkit.core.operations;

import static hageldave.imagingkit.core.util.ImagingKitUtils.clamp_0_1;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.ImgBase;
import hageldave.imagingkit.core.PixelBase;
import hageldave.imagingkit.core.PixelConvertingSpliterator.PixelConverter;
import hageldave.imagingkit.core.PixelManipulator;

/**
 * This Enum class provides a variation of different blend functions to
 * blend a bottom and top value (pixels). It also provides methods to calculate
 * the blend of two colors with additional transparency specification
 * (see {@link #alphaBlend(int, int, double, Blending)}), as well as ready to use
 * Consumers for blending two {@link Img}s
 * (see {@link #getAlphaBlendingWith(Img, double)}).
 * <p>
 * Here's a short example on how to blend two images using the Blending class:
 * <pre>
 * {@code
 * Img bottom, top;
 * ... initialize images ...
 * bottom.forEach(Blending.AVERAGE.getBlendingWith(top));
 * }</pre>
 * You can also specify the offset of the top image on the bottom image:
 * <pre>
 * {@code
 * Img bottom, top;
 * ... initialize images ...
 * int x = 3;  // horizontal offset
 * int y = -5; // vertical offset
 * bottom.forEach(Blending.DIFFERENCE.getBlendingWith(top, x, y));
 * }</pre>
 * If you do not rely on the Img class or only need to blend two single colors
 * you may omit this level of abstraction and use the per color or
 * channel methods:
 * <pre>
 * {@code
 * int bottomARGB = 0x8844FF11;
 * int topARGB =    0xFF118899;
 * double opacity = 0.7;
 * int blendARGB = Blending.blend(bottomARGB, topARGB, opacity, Blending.DODGE);
 *
 * int channel1 = 0xBB;
 * int channel2 = 0x7F;
 * int channelBlend = Blending.SCREEN.blendFunction.blend(channel1, channel2);
 * }</pre>
 *
 *
 * @author hageldave
 * @since 1.3
 */
public enum Blending {
	/** Blend function: f(a,b)=b 										*/
	NORMAL(		(a,b) -> b
	),

	/** Blend function: f(a,b)=(a+b)/2 									*/
	AVERAGE( 	(a,b) -> (a+b)/2 			//((a+b)>>1)
	),

	/** Blend function: f(a,b)=a*b <br> a,b in [0,1] 					*/
	MULTIPLY( 	(a,b) -> a*b 				//((a*b)>>8)
	),

	/** Blend function: f(a,b) = 1 - (1-a) * (1-b) <br> a,b in [0,1] 	*/
	SCREEN( 	(a,b) -> 1 - (1-a) * (1-b)	//(0xff - ((0xff-a) * (0xff-b)>>8))
	),

	/** Blend function: f(a,b) = min(a,b) 								*/
	DARKEN( 	(a,b) -> Math.min(a, b)
	),

	/** Blend function: f(a,b) = max(a,b) 								*/
	BRIGHTEN(	(a,b) -> Math.max(a, b)
	),

	/** Blend function: f(a,b) = |a-b|	 								*/
	DIFFERENCE( (a,b) -> Math.abs(a-b)
	),

	/** Blend function: f(a,b) = a+b		 							*/
	ADDITION(	(a,b) -> a+b				//Math.min(a+b, 0xff)
	),

	/** Blend function: f(a,b) = a + b - 1 <br> a,b in [0,1]	 		*/
	SUBTRACTION((a,b) -> a + b - 1 			//Math.max(a+b-0xff, 0)
	),

	/** Blend function: f(a,b) = a<sup>2</sup>/(1-b) <br> a,b in [0,1]	*/
	REFLECT(	(a,b) -> b == 1 ? (a == 0 ?  0:1):a*a/(1-b)	//Math.min(a*a / Math.max(0xff-b, 1), 0xff)
	),

	/** Blend function:<pre>{@code
	 * f(a,b) = 2*a*b            for a < 1/2
	 * f(a,b) = 1-2*(1-a)*(1-b)  else
	 * a,b in [0,1]
	 * }</pre>															*/
	OVERLAY(	(a,b) -> a < 0.5 ? 2*a*b : 1-2*(1-a)*(1-b)	//a < 128 ? (a*b)>>7 : 0xff-(((0xff-a)*(0xff-b))>>7)
	),

	/** Blend function:<pre>{@code
	 * f(a,b) = 2*a*b            for b < 1/2
	 * f(a,b) = 1-2*(1-a)*(1-b)  else
	 * a,b in [0,1]
	 * }</pre>															*/
	HARDLIGHT(	(a,b) -> b < 0.5 ? 2*a*b : 1-2*(1-a)*(1-b) //b < 128 ? (a*b)>>7 : 0xff-(((0xff-a)*(0xff-b))>>7)
	),

	/** Blend function: (1-a)*multiply(a,b)+a*screen(a,b)<br>a,b in [0,1]*/
	SOFTLIGHT(	(a,b) -> {double c = a*b; return c + (a*(1-((1-a)*(1-b)))-c);}
						//{int c=(a*b)>>8; return c + (a*(0xff-(((0xff-a)*(0xff-b)) >> 8)-c) >> 8);}
	),

	/** Blend function: a/(1-b) <br> a,b in [0,1] 						*/
	DODGE(		(a,b) -> b == 1 ? 1:a/(1-b)//Math.min((a<<8)/Math.max(0xff-b, 1),0xff)
	),
	;

	////// ATTRIBUTES / METHODS //////

	/** This blending's {@link BlendFunction} */
	public final BlendFunction blendFunction;

	/** Enum Constructor */
	private Blending(BlendFunction func) {
		this.blendFunction = func;
	}

	/**
	 * Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img. The top image will be set off to the specified point on the target.
	 * The specified opacity defines how strongly the blended color will occlude
	 * the bottom color (default value is 1 for complete occlusion).
	 * <br><b>
	 * This consumer will apply an ARGB blending, respecting the pixels alpha
	 * (opacity) values.
	 * </b>
	 * This means that not only the specified blend opacity is considered but
	 * also the individual opacity of the pixel. E.g. if the blend opacity
	 * is 0.5 and the top pixel's opacity is also 0.5 (a=128) the effective
	 * occlusion strength of the blend is 0.5*0.5=0.25 and the bottom color will
	 * "shine through" the blend influencing the final color by a factor of 0.75.
	 * See also the RGB blending version without opacity consideration
	 * {@link #getBlendingWith(Img, int, int)}.
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @param xTopOffset horizontal offset of the top image on the bottom
	 * @param yTopOffset vertical offset of the top image on the bottom
	 * @param opacity of the blended color over the bottom color
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 *
	 * @see #getAlphaBlendingWith(Img, double)
	 * @see #getBlendingWith(Img, int, int)
	 * @see #getBlendingWith(Img)
	 */
	public PixelManipulator<PixelBasePair> getAlphaBlendingWith(ImgBase<? extends PixelBase> topImg, int xTopOffset, int yTopOffset, double opacity){
		return alphaBlendingWith(topImg, xTopOffset, yTopOffset, opacity, blendFunction);
	}

	/**
	 * Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img. The top image will be set off to the specified point on the target.
	 * Both images are treated as fully opaque regardless of their alpha values.
	 * <br><b>
	 * This consumer will apply an RGB blending, ignoring the pixels alpha
	 * values, the bottom image's alpha values however will be preserved.
	 * </b>
	 * This means that only the RGB values of the pixels are taken into account,
	 * the result is therefore fully dependent on this blend function.
	 * See also the ARGB alpha blending version which respects pixels alpha values
	 * as well as an additional global opacity parameter
	 * {@link #getAlphaBlendingWith(Img, int, int, double)}
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @param xTopOffset horizontal offset of the top image on the bottom
	 * @param yTopOffset vertical offset of the top image on the bottom
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 *
	 * @see #getBlendingWith(Img)
	 * @see #getAlphaBlendingWith(Img, int, int, double)
	 * @see #getAlphaBlendingWith(Img, double)
	 */
	public PixelManipulator<PixelBasePair> getBlendingWith(ImgBase<? extends PixelBase> topImg, int xTopOffset, int yTopOffset){
		return blendingWith(topImg, xTopOffset, yTopOffset, blendFunction);
	}

	/**
	 * Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img. The top image will have no offset on the bottom image (starting at (0,0)).
	 * The specified opacity defines how strongly the blended color will occlude
	 * the bottom color (default value is 1 for complete occlusion).
	 * <br><b>
	 * This consumer will apply an ARGB blending, respecting the pixels alpha
	 * (opacity) values. </b>
	 * This means that not only the specified blend opacity is considered but
	 * also the individual opacity of the pixel. E.g. if the blend opacity
	 * is 0.5 and the top pixel's opacity is also 0.5 (a=128) the effective
	 * occlusion strength of the blend is 0.5*0.5=0.25 and the bottom color will
	 * "shine through" the blend influencing the final color by a factor of 0.75.
	 * See also the RGB blending version without opacity consideration
	 * {@link #getBlendingWith(Img, int, int)}.
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @param opacity of the blended color over the bottom color
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 */
	public PixelManipulator<PixelBasePair> getAlphaBlendingWith(ImgBase<? extends PixelBase> topImg, double opacity){
		return getAlphaBlendingWith(topImg,0,0, opacity);
	}

	/** Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img. The top image will have no offset on the bottom image (starting at (0,0)).
	 * Both images are treated as fully opaque regardless of their alpha values.
	 * <br><b>
	 * This consumer will apply an RGB blending, ignoring the pixels alpha
	 * values, the bottom image's alpha values however will be preserved.
	 * </b>
	 * This means that only the RGB values of the pixels are taken into account,
	 * the result is therefore fully dependent on this blend function.
	 * See also the ARGB alpha blending version which respects pixels alpha values
	 * as well as an additional global opacity parameter
	 * {@link #getAlphaBlendingWith(Img, int, int, double)}
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 *
	 * @see #getBlendingWith(Img)
	 * @see #getAlphaBlendingWith(Img, int, int, double)
	 * @see #getAlphaBlendingWith(Img, double)
	 */
	public PixelManipulator<PixelBasePair> getBlendingWith(ImgBase<? extends PixelBase> topImg){
		return getBlendingWith(topImg, 0, 0);
	}


	////// STATIC //////

	/** Interface providing the {@link #blend(int, int)} method */
	public static interface BlendFunction {
		/**
		 * Calculates the blended value of two 8bit values
		 * (e.g. red, green or blue channel values).
		 * The resulting value is also 8bit.
		 *
		 * @param bottom value
		 * @param top value
		 * @return blended value
		 */
		public double blend(double bottom, double top);
	}

	/**
	 * Blends two RGB values according to the specified {@link BlendFunction}.
	 * For blending the values alpha values (if present) are ignored,
	 * the bottom color's alpha value however will be preserved.
	 * The specified blend function will be applied to each color channel
	 * pair of bottom and top color.
	 *
	 * @param bottomRGB bottom color RGB or ARGB (least significant 8 bits are b channel)
	 * @param topRGB top color RGB or ARGB (least significant 8 bits are b channel)
	 * @param func blend function to be used
	 * @return blended RGB or ARGB value
	 *
	 * @see #blend(int, int, Blending)
	 * @see #alphaBlend(int, int, double, Blending)
	 * @see #alphaBlend(int, int, double, BlendFunction)
	 */
	public static <P extends PixelBase> P blend(P bottom, PixelBase top, BlendFunction func){
		bottom.setRGB_fromDouble_preserveAlpha(
				func.blend(bottom.r_asDouble(), top.r_asDouble()),
				func.blend(bottom.g_asDouble(), top.g_asDouble()),
				func.blend(bottom.b_asDouble(), top.b_asDouble()));
		return bottom;
	}

	/**
	 * Blends two RGB values according to the specified {@link Blending}'s blend function.
	 * For blending the values alpha values (if present) are ignored,
	 * the bottom color's alpha value however will be preserved.
	 * The specified blend function will be applied to each color channel
	 * pair of bottom and top color.
	 *
	 * @param bottomRGB bottom color RGB or ARGB (least significant 8 bits are b channel)
	 * @param topRGB top color RGB or ARGB (least significant 8 bits are b channel)
	 * @param blending blend function to be used
	 * @return blended RGB or ARGB value
	 *
	 * @see #blend(int, int, BlendFunction)
	 * @see #alphaBlend(int, int, double, Blending)
	 * @see #alphaBlend(int, int, double, BlendFunction)
	 */
	public static <P extends PixelBase> P blend(P bottom, PixelBase top, Blending blending){
		return blend(bottom, top, blending.blendFunction);
	}

	/**
	 * Blends two RGBA values according to the specified {@link BlendFunction} with
	 * the specified opacity factor. The opacity multiplied with the top pixel's
	 * opacity (alpha value) defines how strongly the blended color will occlude
	 * the bottom color (1 for complete occlusion, 0 for full transparency).
	 * The specified blend function will be applied to each color channel
	 * pair of bottom and top color (RGB not alpha).
	 * The resulting alpha value is the sum of the bottom alpha and the blends alpha
	 * (top alpha times specified blend opacity).
	 *
	 * @param bottomARGB bottom color (least significant 8 bits are b channel)
	 * @param topARGB top color (least significant 8 bits are b channel)
	 * @param opacity factor for the resulting blend over the bottom color
	 * @param func blend function
	 * @return blended ARGB value
	 *
	 * @see #alphaBlend(int, int, double, Blending)
	 * @see #blend(int, int, BlendFunction)
	 * @see #blend(int, int, Blending)
	 */
	public static <P extends PixelBase> P alphaBlend(P bottom, PixelBase top, double opacity, BlendFunction func){
//		int temp = 0;
//		double a = Math.min(opacity*(temp=a(topARGB)) + a(bottomARGB),0xff);
//		opacity *= (temp/255.0f);

		double temp = 0;
		double a = clamp_0_1(opacity*(temp=top.a_asDouble()) + bottom.a_asDouble());
		opacity *= temp;

		double transparency = 1-opacity;

		double r = opacity*func.blend(temp=bottom.r_asDouble(), top.r_asDouble()) + temp*transparency;
		double g = opacity*func.blend(temp=bottom.g_asDouble(), top.g_asDouble()) + temp*transparency;
		double b = opacity*func.blend(temp=bottom.b_asDouble(), top.b_asDouble()) + temp*transparency;

		bottom.setARGB_fromDouble(a, r, g, b);
		return bottom;
	}

	/**
	 * Blends two RGBA values according to the specified {@link Blending} with
	 * the specified opacity factor. The opacity multiplied with the top pixel's
	 * opacity (alpha value) defines how strongly the blended color will occlude
	 * the bottom color (1 for complete occlusion, 0 for full transparency).
	 * The specified blend function will be applied to each color channel
	 * pair of bottom and top color (RGB not alpha).
	 * The resulting alpha value is the sum of the bottom alpha and the blends alpha
	 * (top alpha times specified blend opacity).
	 *
	 * @param bottomARGB bottom color (least significant 8 bits are b channel)
	 * @param topARGB top color (least significant 8 bits are b channel)
	 * @param opacity factor for the resulting blend over the bottom color
	 * @param blending the blend function
	 * @return blended ARGB value
	 *
	 * @see #alphaBlend(int, int, double, BlendFunction)
	 * @see #blend(int, int, Blending)
	 * @see #blend(int, int, BlendFunction)
	 */
	public static <P extends PixelBase> P alphaBlend(P bottom, PixelBase top, double opacity, Blending blending){
		return alphaBlend(bottom, top, opacity, blending.blendFunction);
	}

	/**
	 * Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img according to the specified {@link BlendFunction}.
	 * The top image will be set off to the specified point on the target.
	 * The specified opacity defines how strongly the blended color will occlude
	 * the bottom color (default value is 1 for complete occlusion).
	 * <br><b>
	 * This consumer will apply an ARGB blending, respecting the pixels alpha
	 * (opacity) values.
	 * </b>
	 * This means that not only the specified blend opacity is considered but
	 * also the individual opacity of the pixel. E.g. if the blend opacity
	 * is 0.5 and the top pixel's opacity is also 0.5 (a=128) the effective
	 * occlusion strength of the blend is 0.5*0.5=0.25 and the bottom color will
	 * "shine through" the blend influencing the final color by a factor of 0.75.
	 * See also the RGB blending version without opacity consideration
	 * {@link #blendingWith(Img, int, int, BlendFunction)}.
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @param xTopOffset horizontal offset of the top image on the bottom
	 * @param yTopOffset vertical offset of the top image on the bottom
	 * @param opacity of the blended color over the bottom color
	 * @param func function to be used for blending
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 *
	 * @see #blendingWith(Img, int, int, BlendFunction)
	 * @see #getAlphaBlendingWith(Img, int, int, double)
	 * @see #getAlphaBlendingWith(Img, double)
	 */
	public static PixelManipulator<PixelBasePair> alphaBlendingWith(ImgBase<? extends PixelBase> topImg, int xTopOffset, int yTopOffset, double opacity, BlendFunction func){
		return PixelManipulator.fromConverterAndConsumer(
				getPixelBasePairConverter(topImg),
				(pair)->
		{
			int x = pair.px0.getX()-xTopOffset;
			int y = pair.px0.getY()-yTopOffset;

			if(x >= 0 && y >= 0 && x < topImg.getWidth() && y < topImg.getHeight()){
				pair.px1.setPosition(x, y);
				alphaBlend(pair.px0, pair.px1, opacity, func);
			}
		});
	}

	/**
	 * Returns the {@code Consumer<Pixel>} for blending with the specified top
	 * Img according to the specified blend function.
	 * The top image will be set off to the specified point on the target.
	 * Both images are treated as fully opaque regardless of their alpha values.
	 * <br><b>
	 * This consumer will apply an RGB blending, ignoring the pixels alpha
	 * values, the bottom image's alpha values however will be preserved.
	 * </b>
	 * This means that only the RGB values of the pixels are taken into account,
	 * the result is therefore fully dependent on this blend function.
	 * See also the ARGB alpha blending version which respects pixels alpha values
	 * as well as an additional global opacity parameter
	 * {@link #alphaBlendingWith(Img, int, int, double, BlendFunction)}
	 *
	 * @param topImg top image of the blending (consumer is applied to bottom)
	 * @param xTopOffset horizontal offset of the top image on the bottom
	 * @param yTopOffset vertical offset of the top image on the bottom
	 * @param func function to be used for blending
	 * @return Consumer to apply to bottom Img that will perform the specified
	 * blending. {@code bottomImg.forEach(blendingConsumer);}
	 *
	 * @see #alphaBlendingWith(Img, int, int, double, BlendFunction)
	 * @see #getBlendingWith(Img, int, int)
	 * @see #getBlendingWith(Img)
	 */
	public static PixelManipulator<PixelBasePair> blendingWith(ImgBase<? extends PixelBase> topImg, int xTopOffset, int yTopOffset, BlendFunction func){
		return PixelManipulator.fromConverterAndConsumer(
				getPixelBasePairConverter(topImg),
				(pair)->
		{
			int x = pair.px0.getX()-xTopOffset;
			int y = pair.px0.getY()-yTopOffset;

			if(x >= 0 && y >= 0 && x < topImg.getWidth() && y < topImg.getHeight()){
				pair.px1.setPosition(x, y);
				blend(pair.px0, pair.px1, func);
			}
		});
	}


	public static final class PixelBasePair {
		/** the pixel to manipulate in consumer */
		private PixelBase px0;
		/** the pixel of the other image */
		private PixelBase px1;
	}

	private static PixelConverter<PixelBase, PixelBasePair> getPixelBasePairConverter(final ImgBase<? extends PixelBase> secondImg) {
		return new PixelConverter<PixelBase, Blending.PixelBasePair>() {
			@Override
			public PixelBasePair allocateElement() {
				PixelBasePair toReturn = new PixelBasePair();
				toReturn.px1 = secondImg.getPixel();
				return toReturn;
			}

			@Override
			public void convertPixelToElement(PixelBase px, PixelBasePair element) {
				element.px0 = px;
				element.px1.setPosition(px.getX(), px.getY());
			}

			@Override
			public void convertElementToPixel(PixelBasePair element, PixelBase px) {
				// nothing to do as element.px0 is px
			}
		};
	}

}
