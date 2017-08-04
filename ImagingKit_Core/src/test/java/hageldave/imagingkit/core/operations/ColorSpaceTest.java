package hageldave.imagingkit.core.operations;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.imagingkit.core.operations.ColorSpaceTransformation;
import hageldave.imagingkit.core.scientific.ColorImg;
import hageldave.imagingkit.core.scientific.ColorPixel;
import static hageldave.imagingkit.core.operations.ColorSpaceTransformation.*;

public class ColorSpaceTest {

	static final int alphaTestImg = 0x88000000;
	static final int greyErrorThreshold = 7;
	static final int white = 0xffffffff;
	static final int black = 0xff000000;

	@Test
	public void test_discrete(){
		Img reference = getTestImg();


		// test RGB <-> HSV
		{
			ColorSpaceTransformation forward = ColorSpaceTransformation.RGB_2_HSV;
			ColorSpaceTransformation backward = ColorSpaceTransformation.HSV_2_RGB;
			// test image transformation (image covers all RGB values)
			Img img = reference.copy();
			img.forEach(true, forward);
			img.forEach(true, backward);
			int[] error = getMaxGreyError(reference, img);
			assertTrue(error[0] >= 0);
			assertTrue(String.format("LAB Error: %d %s %s", error[0],
					Integer.toHexString(error[1]),
					Integer.toHexString(error[2])),
					error[0] < greyErrorThreshold);

			// test alpha preservation
			for(int color: img.getData()){
				assertEquals(alphaTestImg, color & 0xff000000);
			}

			// test black and white transformation accuracy
			int color;
			color = forward.discreteTransform(white);
			color = backward.discreteTransform(color);
			assertEquals(white, color);

			color = forward.discreteTransform(black);
			color = backward.discreteTransform(color);
			assertEquals(black, color);
		}

		// test RGB <-> LAB
		{
			ColorSpaceTransformation forward = ColorSpaceTransformation.RGB_2_LAB;
			ColorSpaceTransformation backward = ColorSpaceTransformation.LAB_2_RGB;
			// test image transformation (image covers all RGB values)
			Img img = reference.copy();
			img.forEach(true, forward);
			img.forEach(true, backward);
			int[] error = getMaxGreyError(reference, img);
			assertTrue(error[0] >= 0);
			assertTrue(String.format("LAB Error: %d %s %s", error[0],
					Integer.toHexString(error[1]),
					Integer.toHexString(error[2])),
					error[0] < greyErrorThreshold);

			// test alpha preservation
			for(int color: img.getData()){
				assertEquals(alphaTestImg, color & 0xff000000);
			}

			// test black and white transformation accuracy
			int color;
			color = forward.discreteTransform(white);
			color = backward.discreteTransform(color);
			assertEquals(white, color);

			color = forward.discreteTransform(black);
			color = backward.discreteTransform(color);
			assertEquals(black, color);
		}

	}


	static Img getTestImg(){
		Img img = new Img(5000,5000);
		img.forEach(px->{
			px.setValue(alphaTestImg | (px.getIndex()&0xffffff));
		});
		return img;
	}

	static int[] getMaxGreyError(Img img1, Img img2){
		assertEquals(img1.getDimension(), img2.getDimension());
		assertNotEquals(img1, img2);
		int maxError = -1;
		int col1 = 0;
		int col2 = 0;
		int size = img1.numValues();
		for(int i = 0; i < size; i++){
			int color1 = img1.getData()[i];
			int color2 = img2.getData()[i];
			int err = Math.abs(Pixel.getGrey(color1, 1,1,1)-Pixel.getGrey(color2, 1,1,1));
			if(err > maxError){
				maxError = err;
				col1 = color1;
				col2 = color2;
			}
		}

		return new int[]{maxError, col1, col2};
	}


	@Test
	public void test_continuous(){
		ColorImg img = getTestColorImg();
		ColorSpaceTransformation[] toTest = {RGB_2_HSV, /*NOT HSV_2_RGB since it is not injective ,*/ RGB_2_LAB, LAB_2_RGB, RGB_2_YCbCr, YCbCr_2_RGB};
		for(ColorSpaceTransformation cst: toTest){
			ColorImg testimg = img.copy();
			testimg.stream(true)
			.forEach(cst);
			// test alpha preservation
			testimg.forEach(true, px->assertEquals(img.getDataA()[px.getIndex()], px.a(), 0));
			testimg.stream(true)
			.forEach(cst.inverse());
			// test alpha preservation
			testimg.forEach(true, px->assertEquals(img.getDataA()[px.getIndex()], px.a(), 0));
			// test if forward transform and backwards transform preserves lumanance to a high degree (max 0.001 error tolerance)
			testimg.forEach(true, px->{
				double lum1 = px.getLuminance();
				double lum2 = ColorPixel.getLuminance(img.getDataR()[px.getIndex()], img.getDataG()[px.getIndex()], img.getDataB()[px.getIndex()]);
				if(Math.abs(lum1-lum2) > 0.001)
					assertEquals(cst.name() +" "+ img.getPixel(px.getX(), px.getY()).asString()+" "+ px.asString(), lum2, lum1, 0.001);
			});
		}
	}

	static ColorImg getTestColorImg(){
		ColorImg img = new ColorImg(5000,5000, true);
		img.forEach(px->{
			int col = alphaTestImg | (px.getIndex()&0xffffff);
			px.setARGB_fromDouble(
					Pixel.a_normalized(col),
					Pixel.r_normalized(col),
					Pixel.g_normalized(col),
					Pixel.b_normalized(col));
		});
		return img;
	}

}
