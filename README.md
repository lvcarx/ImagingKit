# ImagingKit
[![Maven Central](https://img.shields.io/maven-central/v/com.github.hageldave.imagingkit/imagingkit-core.svg)](http://search.maven.org/#artifactdetails|com.github.hageldave.imagingkit|imagingkit-core|1.2|jar)
[![Build Status](https://travis-ci.org/hageldave/ImagingKit.svg?branch=master)](https://travis-ci.org/hageldave/ImagingKit)
[![Coverage Status](https://coveralls.io/repos/github/hageldave/ImagingKit/badge.svg?branch=master)](https://coveralls.io/github/hageldave/ImagingKit?branch=master)

A Java library for imaging tasks that integrates well with the commonly used java.awt.image environment (especially well with TYPE_INT BufferedImages). Its goal is to make image processing more convenient and to ease performance optimization. The library is intended for images using integer typed values like 24bit RGB or 32bit ARGB.

So far the *ImagingKit-Core* artifact of the library is available through the maven central repository:
```
<dependency>
    <groupId>com.github.hageldave.imagingkit</groupId>
    <artifactId>imagingkit-core</artifactId>
    <version>1.2</version>
</dependency>
```
--
### Code Examples
Convert an image to grayscale:
```java
BufferedImage buffimg = ImageLoader.loadImage("myimage_colored.png", BufferedImage.TYPE_INT_ARGB);
Img img = Img.createRemoteImg(buffimg);
img.forEachParallel((pixel) -> {
	int gray = (pixel.r() + pixel.g() + pixel.b())/3;
	pixel.setARGB(pixel.a(), gray, gray, gray);
});
ImageSaver.saveImage(buffimg,"myimage_grayscale.png");
```
Fancy polar color thing:
```java
Img img = new Img(1024, 1024);
img.forEach(px -> {
	double x = (px.getX()-512)/512.0;
	double y = (px.getY()-512)/512.0;
	double len = Math.max(Math.abs(x),Math.abs(y));
	double angle = (Math.atan2(x,y)+Math.PI)*(180/Math.PI);
	
	double r = 255*Math.max(0,1-Math.abs((angle-120)/120.0));
	double g = 255*Math.max(0, 1-Math.abs((angle-240)/120.0));
	double b = 255*Math.max(0, angle <= 120 ? 
			1-Math.abs((angle)/120.0):1-Math.abs((angle-360)/120.0));
	
	px.setRGB((int)(r*(1-len)), (int)(g*(1-len)), (int)(b*(1-len)));
});
ImageSaver.saveImage(img.getRemoteBufferedImage(), "polar_colors.png");
```
Shifting hue (using color space transformation):
```java
URL lenaURL = new URL("http://sipi.usc.edu/database/preview/misc/4.2.04.png");
BufferedImage lenaBImg = ImageLoader.loadImage(lenaURL.openStream(), BufferedImage.TYPE_INT_ARGB);
Img img = Img.createRemoteImg(lenaBImg);

img.forEach(ColorSpaceTransformation.RGB_2_HSV.get());
int hueShift = (int)((360-30) * (256.0f/360.0f));
img.forEach(pixel -> {
	// R channel corresponds to hue
	pixel.setR((pixel.r()+hueShift));
});
img.forEach(ColorSpaceTransformation.HSV_2_RGB.get());

ImageSaver.saveImage(img.getRemoteBufferedImage(), "lena_hue_shift.png");
```
Swing framebuffer rendering:
```java
Img img2display = new Img(160, 90); 
Img img2render = img2display.copy();
BufferedImage bimg = img2display.getRemoteBufferedImage();
JPanel canvas = new JPanel(){ public void paint(Graphics g) { 
    super.paint(g); 
    g.drawImage(bimg, 0,0,getWidth(),getHeight(), 0,0,160,90, null);
}};
canvas.setPreferredSize(img2display.getDimension());
JFrame f = new JFrame("IMG"); f.setContentPane(canvas); 
f.pack(); f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
SwingUtilities.invokeLater(()->{f.setVisible(true);});

BiConsumer<Pixel, Long> shader = (px, time)->{
    px.setRGB_fromNormalized(
            px.getXnormalized(), 
            px.getYnormalized(), 
            (float)(0.5 + 0.5*Math.sin(time/250.0)));
};

final long fpsLimitTime = 1000/25; // 25fps
long t = System.currentTimeMillis()+fpsLimitTime;
while(true){
    long now = System.currentTimeMillis();
    Thread.sleep(Math.max(0, t-now));
    img2render.forEachParallel(px->{shader.accept(px, now);});
    // copy is fast so no intermediate changes will be seen on display
    System.arraycopy(img2render.getData(), 0, img2display.getData(), 0, img2display.numValues());
    canvas.repaint();
    t = now+fpsLimitTime;
}
```
