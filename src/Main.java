import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Main {

	static JFrame frame;

	static void decodeMenu() throws IOException {
		FileDialog encodedDialog = new FileDialog(frame, "Select LSB Encoded Image");
		encodedDialog.setMode(FileDialog.LOAD);
		encodedDialog.setFile("*.png");
		encodedDialog.setVisible(true);

		File encodedFile = new File(encodedDialog.getDirectory() + encodedDialog.getFile());
		if (!encodedFile.exists())
			System.exit(-1);

		File outfile = new File("lsb_decoded_" + FilenameUtils.removeExtension(encodedFile.getName()));

		FileUtils.writeByteArrayToFile(outfile, LSB.lsbDecode(encodedFile));
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

		BufferedImage resultImage = LSB.lsbEncode(coverFile, secretFile);

		if (resultImage == null) {
			JOptionPane.showMessageDialog(null, "Secret file is too large to fit into this image!",
					"Couldn't embed secret", JOptionPane.ERROR_MESSAGE);
			System.exit(-1);
		} else
			JOptionPane.showMessageDialog(null, "Successfully embedded secret file into cover image.", "Done",
					JOptionPane.INFORMATION_MESSAGE);

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
			else if (reply == JOptionPane.YES_OPTION) {
				try {
					encodeMenu();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "An error occurred during the encoding process: " + e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else {
				try {
					decodeMenu();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "An error occurred during the decoding process: " + e.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}
		}
	}

}
