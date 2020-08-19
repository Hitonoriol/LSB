import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.BitSet;

import javax.imageio.ImageIO;

public class LSB {

	static final int BYTE = 8; // Bits in bytes
	static final int RGB = 3; // RGB entries
	static final int RGBA = 4; // Number of RGBA entries
	static final int HEADER_LENGTH = 64; // Header length in bits

	static private int setLSB(int a, boolean bit) { // Sets the Least Significant Bit of <a> to <bit>
		if (a % 2 != (bit ? 1 : 0)) {
			if (a % 2 == 0)
				a += 1;
			else
				a -= 1;
		}

		return a;
	}

	static byte[] lsbDecode(File encodedFile) throws IOException {
		BufferedImage encodedImage = ImageIO.read(encodedFile);
		int x = 0, y = 0;
		int i = 0;
		int pixel[];
		BitSet bits = new BitSet();

		Utils.out("Reading bits...");
		while (y < encodedImage.getHeight()) {
			while (x < encodedImage.getWidth()) {
				pixel = encodedImage.getRaster().getPixel(x, y, new int[RGBA]); // Just in case we got an RGBA image

				bits.set(i, (pixel[0] % 2) == 1 ? true : false);
				bits.set(++i, (pixel[1] % 2) == 1 ? true : false);
				bits.set(++i, (pixel[2] % 2) == 1 ? true : false);

				x++;
				i++;
			}
			x = 0;
			y++;
		}
		long secretLen = Utils.decode8((bits.get(0, HEADER_LENGTH)).toByteArray());

		Utils.out("Embedded file length: " + secretLen + " bytes");

		return (bits.get(HEADER_LENGTH, (int) (secretLen * BYTE + BYTE * BYTE))).toByteArray();
	}

	static BufferedImage lsbEncode(File coverFile, File secretFile) throws IOException {
		BufferedImage coverImage = ImageIO.read(coverFile);

		if ((coverImage.getHeight() * coverImage.getWidth() * RGB) < secretFile.length() * BYTE + HEADER_LENGTH)
			return null;

		ByteArrayOutputStream msgStream = new ByteArrayOutputStream();
		Utils.out("Writing secret file length to header: " + secretFile.length());
		msgStream.write(Utils.encode8(secretFile.length()));
		msgStream.write(Files.readAllBytes(secretFile.toPath()));

		BitSet secret = BitSet.valueOf(msgStream.toByteArray());

		msgStream.close();

		final int secretLen = secret.length();
		Utils.out("Embedding " + secretLen / BYTE + " bytes into cover image...");
		int x = 0, y = 0;
		Color color; // RGB pixel color data
		int i = 0; // Bit iterator
		int r, g, b;

		while (y < coverImage.getHeight()) {
			while (x < coverImage.getWidth()) {
				color = new Color(coverImage.getRGB(x, y));

				r = setLSB(color.getRed(), secret.get(i));
				g = setLSB(color.getGreen(), secret.get(++i));
				b = setLSB(color.getBlue(), secret.get(++i));

				coverImage.setRGB(x, y, new Color(r, g, b).getRGB());

				x++;
				i++;

				if (i > secretLen) {
					Utils.out("Stopping at pixel " + x + ", " + y);
					return coverImage;
				}

			}
			x = 0;
			y++;
		}

		return coverImage;
	}

}
