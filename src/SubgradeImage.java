
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class SubgradeImage {
	private BufferedImage image;
	private byte bimage[];
	
	public SubgradeImage(BufferedImage sImage)
	{
		image = sImage;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageOutputStream ios;
		try {
			ios = ImageIO.createImageOutputStream(baos);
		} catch (IOException e1) {
			e1.printStackTrace();
			return;
		}
		Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix("jpg");
		ImageWriter iw = (ImageWriter)writers.next();
		if (iw != null)
		{
			iw.setOutput(ios);
			try {
				iw.write(image);
				ios.flush();
				ios.close();
				bimage = baos.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public BufferedImage getImage()
	{
		return image;
	}
	
	public byte[] getImageBytes()
	{
		return bimage;
	}
}
