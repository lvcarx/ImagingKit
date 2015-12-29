package hageldave.imagingkit.core;

import org.junit.Test;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;

public class ImgTest {

	@Test
	public void channelMethods_test(){
		int color = 0xffaa1244;
		assertEquals(0xff, Img.a(color));
		assertEquals(0xaa, Img.r(color));
		assertEquals(0x12, Img.g(color));
		assertEquals(0x44, Img.b(color));
		assertEquals(0xa124, Img.ch(color, 4, 16));
		assertEquals(0x44, Img.ch(color, 0, 8));
		
		assertEquals(0x01001234, Img.argb_fast(0x01, 0x00, 0x12, 0x34));
		assertEquals(0xff543210, Img.rgb_fast(0x54, 0x32, 0x10));
		assertEquals(0xff00ff54, Img.rgb_bounded(-12, 260, 0x54));
		assertEquals(0xffffffff, Img.rgb(0x15ff, 0xaff, 0x5cff));
		assertEquals(0b10101110, Img.combineCh(2, 0b10, 0b10, 0b11, 0b10));
	}
	
	@Test
	public void boundaryModes_test(){
		Img img = new Img(4, 4, new int[]
				{
						0,1,2,3,
						4,5,6,7,
						8,9,9,9,
						9,9,9,9
				});
		for(int mode: new int[]{Img.boundary_mode_zero, Img.boundary_mode_mirror, Img.boundary_mode_repeat_edge, Img.boundary_mode_repeat_image}){
			// test corners
			assertEquals(0, img.getValue(0, 0, mode));
			assertEquals(3, img.getValue(3, 0, mode));
			assertEquals(9, img.getValue(0, 3, mode));
			assertEquals(9, img.getValue(3, 3, mode));
		}
		assertEquals(0, img.getValue(-1, 0, Img.boundary_mode_zero));
		assertEquals(0, img.getValue(4, 0, Img.boundary_mode_zero));
		assertEquals(0, img.getValue(0, -1, Img.boundary_mode_zero));
		assertEquals(0, img.getValue(0, 4, Img.boundary_mode_zero));
		
		assertEquals(0, img.getValue(-2, 0, Img.boundary_mode_repeat_edge));
		assertEquals(3, img.getValue(3, -2, Img.boundary_mode_repeat_edge));
		assertEquals(9, img.getValue(-10, 10, Img.boundary_mode_repeat_edge));
		assertEquals(3, img.getValue(10, -10, Img.boundary_mode_repeat_edge));
		
		for(int y = 0; y < 4; y++)
		for(int x = 0; x < 4; x++){
			assertEquals(img.getValue(x, y), img.getValue(x+4, y, Img.boundary_mode_repeat_image));
			assertEquals(img.getValue(x, y), img.getValue(x, y+4, Img.boundary_mode_repeat_image));
			assertEquals(img.getValue(x, y), img.getValue(x-4, y, Img.boundary_mode_repeat_image));
			assertEquals(img.getValue(x, y), img.getValue(x, y-4, Img.boundary_mode_repeat_image));
			assertEquals(img.getValue(x, y), img.getValue(x+8, y+8, Img.boundary_mode_repeat_image));
			assertEquals(img.getValue(x, y), img.getValue(x-8, y-8, Img.boundary_mode_repeat_image));
		}
		
		for(int y = 0; y < 4; y++)
		for(int x = 0; x < 4; x++){
			assertEquals(img.getValue(x, y), img.getValue(x+8, y+8, Img.boundary_mode_mirror));
			assertEquals(img.getValue(x, y), img.getValue(7-x, 7-y, Img.boundary_mode_mirror));
			assertEquals(img.getValue(x, y), img.getValue(-8+x, -8+y, Img.boundary_mode_mirror));
			assertEquals(img.getValue(x, y), img.getValue(-1-x, -1-y, Img.boundary_mode_mirror));
		}
	}
	
	@Test
	public void pixelRetrieval_test(){
		Img img = new Img(4, 3, new int[]
				{
						0,1,2,3,
						4,5,6,7,
						8,9,9,5
				});
		
		assertEquals(img.getData().length, img.getWidth()*img.getHeight());
		assertEquals(img.getData().length, img.numValues());
		
		int i = 0;
		for(int y = 0; y < img.getHeight(); y++)
		for(int x = 0; x < img.getWidth(); x++){
			assertEquals(img.getData()[i], img.getValue(x, y));
			i++;
		}
		
		// test interpolation
		img = new Img(5,3, new int[]
				{
					0,1,2,3,4,
					2,3,4,5,6,
					4,5,6,7,8
				});
		
		assertEquals(img.getValue(0, 0), img.interpolateValue(0, 0));
		assertEquals(img.getValue(img.getWidth()-1, img.getHeight()-1), img.interpolateValue(1, 1));
		assertEquals(img.getValue(0, img.getHeight()-1), img.interpolateValue(0, 1));
		assertEquals(img.getValue(img.getWidth()-1, 0), img.interpolateValue(1, 0));
		assertEquals(img.getValue(2, 1), img.interpolateValue(0.5f, 0.5f));
		
		// test copypixels
		Img img2 = new Img(2,2);
		img.copyArea(0, 0, 2, 2, img2, 0, 0);
		assertEquals(img.getValue(0, 0), img2.getValue(0, 0));
		assertEquals(img.getValue(1, 1), img2.getValue(1, 1));
		img.copyArea(1, 1, 2, 2, img2, 0, 0);
		assertEquals(img.getValue(1, 1), img2.getValue(0, 0));
		assertEquals(img.getValue(2, 2), img2.getValue(1, 1));
		img.copyArea(4, 2, 1, 1, img2, 1, 0);
		assertEquals(img.getValue(4, 2), img2.getValue(1, 0));
	}
	
	@Test
	public void buffimg_test(){
		BufferedImage bimg = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		bimg.setRGB(0, 0, 1, 1, new int[]
				{
					0,1,2,3,
					4,5,6,7,
					8,9,0,1,
					2,3,4,5
				}, 0, 4);
		
		{
			// test same pixels
			Img img = new Img(bimg);
			for(int y = 0; y < 4; y++)
			for(int x = 0; x < 4; x++){
			assertEquals(bimg.getRGB(x, y), img.getValue(x, y));
			}
		}
		
		{
			// test remoteness
			Img img = new Img(bimg);
			Img img2 = Img.createRemoteImg(bimg);
			for(int y = 0; y < 4; y++)
			for(int x = 0; x < 4; x++){
				img2.setValue(x, y, -2000-x-y);
				assertNotEquals(bimg.getRGB(x, y), img.getValue(x, y));
				assertEquals(bimg.getRGB(x, y), img2.getValue(x, y));
			}
		}
		
		{	
			// test remoteness in both directions
			Img img = Img.createRemoteImg(bimg);
			BufferedImage r_bimg = img.getRemoteBufferedImage();
			for(int y = 0; y < 4; y++)
			for(int x = 0; x < 4; x++){
				assertEquals(bimg.getRGB(x, y), r_bimg.getRGB(x, y));
				bimg.setRGB(x, y, x+y+144);
				assertEquals(bimg.getRGB(x, y), r_bimg.getRGB(x, y));
				r_bimg.setRGB(x, y, x+y+166);
				assertEquals(bimg.getRGB(x, y), r_bimg.getRGB(x, y));
			}
		}
		
	}
	
	@Test
	public void iterator_test(){
		Img img = new Img(16,9);
		{
			int i = 0;
			for(Pixel p: img){
				p.setValue(i);
				i++;
			}
			assertEquals(img.numValues(), i);
		}
		for(int i = 0; i < img.numValues(); i++){
			assertEquals(i, img.getData()[i]);
		}
		
		
		
	}
	
	
}
