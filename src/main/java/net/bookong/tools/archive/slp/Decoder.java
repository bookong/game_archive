package net.bookong.tools.archive.slp;

import java.nio.ByteBuffer;

import net.bookong.tools.utils.ByteUtils;

/**
 * PLS 文件格式解码器
 * @author jiangxu
 * @see http://www.digitization.org/wiki/index.php?title=SLP
 */
public class Decoder {
	/**　文件头信息：版本　2.0N　*/
	public static byte[] FILE_HEADER_VERSION = {0x32, 0x2E, 0x30, 0x4E};
	
	/** 文件头信息：注释　 */
	public static byte[] FILE_HEADER_COMMENT = { 
		0x41, 0x72, 0x74, 0x44, 0x65, 0x73, 0x6B, 0x20, 												// ArtDesk.
		0x31, 0x2E, 0x30, 0x30, 0x20, 0x53, 0x4C, 0x50, 0x20, 0x57, 0x72, 0x69, 0x74, 0x65, 0x72, 0x00 	// 1.00.SLP.Writer.
	};
	
	/**
	 * 解析 SLP 文件成对应的 Frame 信息
	 * @param buff SLP文件内容
	 */
	public static Frame[] decode(byte[] buff){
		byte[] buff4byte = new byte[4];
		System.arraycopy(buff, 4, buff4byte, 0, 4);
		int framesNum = (int)ByteUtils.getLongValue(buff4byte);
		Frame[] frames = new Frame[framesNum];
		for (int i = 0; i < framesNum; i++) {
			frames[i] = createFrame(buff, i * 32 + 32);
		}
		return frames;
	}
	
	private static Frame createFrame(byte[] buff, int offset){
		Frame frame = new Frame();
		byte[] buff4byte = new byte[4];
		System.arraycopy(buff, offset, buff4byte, 0, 4);
		int cmdTableOffset = (int)ByteUtils.getLongValue(buff4byte);

		System.arraycopy(buff, offset + 4, buff4byte, 0, 4);
		int outlineTableOffset = (int)ByteUtils.getLongValue(buff4byte);

		System.arraycopy(buff, offset + 16, buff4byte, 0, 4);
		frame.width = (int) ByteUtils.getLongValue(buff4byte);

		System.arraycopy(buff, offset + 20, buff4byte, 0, 4);
		frame.height = (int) ByteUtils.getLongValue(buff4byte);

		System.arraycopy(buff, offset + 24, buff4byte, 0, 4);
		frame.centerX = (int) ByteUtils.getLongValue(buff4byte);

		System.arraycopy(buff, offset + 28, buff4byte, 0, 4);
		frame.centerY = (int) ByteUtils.getLongValue(buff4byte);
		
		frame.img256Buff = ByteBuffer.allocateDirect(frame.width * frame.height);
		frame.featureBuff = ByteBuffer.allocateDirect(frame.width * frame.height);
		
		RowDecoder rowBuffer = new RowDecoder(frame, buff, cmdTableOffset);
		// 循环处理每一行数据
		for (int row = 0; row < frame.height; row++) {
			rowBuffer.decod(row, outlineTableOffset);
			frame.img256Buff.put(rowBuffer.img256RowBuff);
			frame.featureBuff.put(rowBuffer.featureRowBuff);
		}
		return frame;
	}
}
