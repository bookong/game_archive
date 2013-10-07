package net.bookong.tools.archive.slp;

import java.nio.ByteBuffer;

/**
 * 从 SLP 文件中解析出的一帧的内容
 * 
 * @author jiangxu
 * 
 */
public class Frame {
	/** 中心 X 坐标 */
	public int centerX;
	/** 中心 Y 坐标 */
	public int centerY;
	/** 这一帧图像的宽 */
	public int width;
	/** 这一帧图像的高 */
	public int height;
	
	/** 不带调色板的256色位图数据缓冲(每个byte记录一个位图索引) */
	public ByteBuffer img256Buff;
	
	/** 
	 * 特性缓冲区：记录透明色和玩家标记等信息<br>
	 * 0x00: 透明<br>
	 * 0xFF: 不透明<br>
	 * 0x10 ~ 0x20: 记录玩家颜色索引，真正对应的调色板索引是 ([player number] * 16) + alphaBuff<br>
	 * 0xEE: 轮廓
	 */
	public ByteBuffer featureBuff;
}
