
public class Utils {

	static byte[] encode8(long l) { // Encode long as 8 bytes
		byte[] result = new byte[8];
		for (int i = 7; i >= 0; i--) {
			result[i] = (byte) (l & 0xFF);
			l >>= 8;
		}
		return result;
	}

	static long decode8(byte[] b) { // Decode byte array to long
		long result = 0;
		for (int i = 0; i < 8; i++) {
			result <<= 8;
			result |= (b[i] & 0xFF);
		}
		return result;
	}

	static void out(String arg) {
		System.out.println(arg);
	}

}
