package net.bookong.tools.utils;

/**
 * @author jiangxu
 *
 */
public class ByteUtils {
	private final static String[] HEX_VALUE = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
	
	/**
	 *  以 long 型返回 byte[4] 中内容
	 * 
	 * @param longVal 必须是 byte[4]，469291760 → F0 D2 F8 1B
	 */
	public static long getLongValue(byte[] longVal) {
		return (0x0FF & longVal[0]) 
				| ((0x0FF & longVal[1]) << 8) 
				| ((0x0FF & longVal[2]) << 16)
				| ((0x0FF & longVal[3]) << 24);
	}
	
	/**
	 *  以 long 型返回 byte[2] 中内容
	 * 
	 * @param shortVal 必须是 byte[2]，7263 → 5F 1C
	 */
	public static long getShortValue(byte[] shortVal) {
		return (0x0FF & shortVal[0]) 
				| ((0x0FF & shortVal[1]) << 8);
	}

	/**
	 * 以十六进制 dump 出指定内容
	 * @param b 要 dump 的数据数组
	 * @param off 开始 dump 的偏移量
	 * @param len 要 dump 的数据长度
	 */
	public static String hexDump(byte[] b, int off, int len) {
		StringBuilder buff = new StringBuilder("           0  1  2  3  4  5  6  7  8  9  A  B  C  D  E  F");
		StringBuilder desc = new StringBuilder();
		for (int i = off; i < off + len; i++) {
			int idx = i - off;
			if (idx % 16 == 0) {
				buff.append("    ").append(desc);
				desc.setLength(0);
				buff.append("\n");
				buff.append(String.format("%08x:", idx).toUpperCase());
			}
			
			buff.append(" ").append(getHexString(b[i]));
			if (b[i] >= 0x21 && b[i] <= 0x7E) {
				desc.append((char)b[i]);
			}else{
				desc.append(".");
			}
		}
		
		if (len % 16 != 0) {
			for (int i = 0; i < 16 - len % 16; i++) {
				buff.append("   ");
			}
		}
		buff.append("    ").append(desc);
		
		return buff.toString();
	}
	
	public static String getHexString(byte b){
		return HEX_VALUE[(0x0F0 & b) >> 4] + HEX_VALUE[0x0F & b];
	}
}
