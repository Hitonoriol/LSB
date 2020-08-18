import java.awt.Color;
import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.BitSet;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Main {

	static final int BYTE = 8; // Bits in bytes
	static final int RGB = 3; // RGB entries
	static final int HEADER_LENGTH = 64; // Header length in bits

	static JFrame frame;

	static void out(String arg) {
		System.out.println(arg);
	}

	static BitSet fromString(String binary) {
		BitSet bitset = new BitSet(binary.length());
		for (int i = 0; i < binary.length(); i++) {
			if (binary.charAt(i) == '1') {
				bitset.set(i);
			}
		}
		return bitset;
	}

	static byte[] encode8(long l) {
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	static long decode8(byte[] b) {
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}

	static void decodeMenu() throws IOException {
		FileDialog encodedDialog = new FileDialog(frame, "Select LSB Encoded Image");
		encodedDialog.setMode(FileDialog.LOAD);
		encodedDialog.setFile("*.png");
		encodedDialog.setVisible(true);

		File encodedFile = new File(encodedDialog.getDirectory() + encodedDialog.getFile());
		if (!encodedFile.exists())
			System.exit(-1);

		File outfile = new File("lsb_decoded_" + FilenameUtils.removeExtension(encodedFile.getName()));

		FileUtils.writeByteArrayToFile(outfile, lsbDecode(encodedFile));
		InputStream is = new BufferedInputStream(new FileInputStream(outfile));
		String mime = URLConnection.guessContentTypeFromStream(is);

		JOptionPane.showMessageDialog(null, "Successfully extracted file with MIME type: [" + mime + "]",
				"Extraction succeded", JOptionPane.INFORMATION_MESSAGE);
	}

	static void encodeMenu() throws IOException {
		FileDialog coverDialog = new FileDialog(frame, "Select Cover Image");
		coverDialog.setMode(FileDialog.LOAD);
		coverDialog.setFile("*.png");
		coverDialog.setVisible(true);
		File coverFile = new File(coverDialog.getDirectory() + coverDialog.getFile());
		if (!coverFile.exists())
			System.exit(-1);

		FileDialog secretDialog = new FileDialog(frame, "Select Secret File");
		secretDialog.setMode(FileDialog.LOAD);
		secretDialog.setVisible(true);
		File secretFile = new File(secretDialog.getDirectory() + secretDialog.getFile());
		if (!secretFile.exists())
			System.exit(-1);

		BufferedImage resultImage = lsbEncode(coverFile, secretFile);

		ImageIO.write(resultImage, "png", new File("lsb_" + coverFile.getName()));
	}

	public static void main(String[] args) throws IOException {

		frame = new JFrame();

		Object[] menuOptions = { "Embed", "Extract" };
		int reply;

		while (true) {
			reply = JOptionPane.showOptionDialog(frame,
					"Choose an option to LSB embed file into image or extract a file out of LSB encoded image.\nClose this window to quit.",
					"LSBEnc", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null, menuOptions, menuOptions[0]);

			if (reply == JOptionPane.CLOSED_OPTION)
				System.exit(0);
			else if (reply == JOptionPane.YES_OPTION)
				encodeMenu();
			else
				decodeMenu();
		}
	}

	static byte[] lsbDecode(File encodedFile) throws IOException {
		BufferedImage encodedImage = ImageIO.read(encodedFile);
		int x = 0, y = 0;
		int i = 0;
		int pixel[];
		BitSet bits = new BitSet();

		out("Reading bits...");
		while (y < encodedImage.getHeight()) {
			while (x < encodedImage.getWidth()) {
				pixel = encodedImage.getRaster().getPixel(x, y, new int[3]);

				bits.set(i, (pixel[0] % 2) == 1 ? true : false);

				bits.set(++i, (pixel[1] % 2) == 1 ? true : false);

				bits.set(++i, (pixel[2] % 2) == 1 ? true : false);

				//out("Read pixel " + x + ", " + y);
				x++;
				i++;
			}
			x = 0;
			y++;
		}
		long secretLen = decode8((bits.get(0, 64)).toByteArray());

		out("Embedded file length: " + secretLen + " bytes");

		return (bits.get(64, (int) (secretLen * BYTE + BYTE * BYTE))).toByteArray();
	}

	static BufferedImage lsbEncode(File coverFile, File secretFile) throws IOException {
		BufferedImage coverImage = ImageIO.read(coverFile);

		if ((coverImage.getHeight() * coverImage.getWidth() * RGB) < secretFile.length() * BYTE + HEADER_LENGTH) {
			JOptionPane.showMessageDialog(null, "Secret file is too large to fit into this image!",
					"Couldn't embed secret", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
			return null;
		}
		
		ByteArrayOutputStream msgStream = new ByteArrayOutputStream();
		out("Writing secret file length to header: " + secretFile.length());
		msgStream.write(encode8(secretFile.length()));
		msgStream.write(Files.readAllBytes(secretFile.toPath()));

		BitSet secret = BitSet.valueOf(msgStream.toByteArray());

		msgStream.close();

		final int secretLen = secret.length();
		out("Embedding " + secretLen / BYTE + " bytes into cover image...");
		int x = 0, y = 0;
		Color color; // RGB pixel color data
		int i = 0; // Bit iterator
		int r, g, b;

		while (y < coverImage.getHeight()) {
			while (x < coverImage.getWidth()) {
				color = new Color(coverImage.getRGB(x, y));

				r = color.getRed();
				if (r % 2 != (secret.get(i) ? 1 : 0)) {
					if (r % 2 == 0)
						r += 1;
					else
						r -= 1;
				}

				g = color.getGreen();
				if (g % 2 != (secret.get(++i) ? 1 : 0)) {
					if (g % 2 == 0)
						g += 1;
					else
						g -= 1;
				}

				b = color.getBlue();
				if (b % 2 != (secret.get(++i) ? 1 : 0)) {
					if (b % 2 == 0)
						b += 1;
					else
						b -= 1;
				}

				coverImage.setRGB(x, y, new Color(r, g, b).getRGB());
				// out(cbf.getRGB() + " encoded");
				x++;
				i++;

				if (i > secretLen) {
					out("Stopping at pixel " + x + ", " + y);
					JOptionPane.showMessageDialog(null, "Successfully embedded secret file into cover image.", "Done",
							JOptionPane.INFORMATION_MESSAGE);
					return coverImage;
				}
			}
			x = 0;
			y++;
		}

		return coverImage;
	}

}
