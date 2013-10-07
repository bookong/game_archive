package net.bookong.tools.archive.slp;

import java.util.Arrays;

import net.bookong.tools.utils.ByteUtils;

public class RowDecoder {
	public byte[] img256RowBuff;
	public byte[] featureRowBuff;
	/** 每行的宽度 */
	private int rowWidth;
	/** 一共多少行 */
	private int rowCount;
	private byte[] buff4byte = new byte[4];
	private byte[] buff;
	/** 正在处理这一行上的第几列 */
	int col;
	/** 每行cmd开始位置 */
	int[] cmdOffsets;
	
	
	
	public RowDecoder(Frame frame, byte[] buff, int cmdTableOffset){
		this.buff = buff;
		this.rowWidth = frame.width;
		this.rowCount = frame.height;
		
		img256RowBuff = new byte[rowWidth];
		featureRowBuff = new byte[rowWidth];
		cmdOffsets = new int[rowCount];
		for(int i=0; i<rowCount; i++){
			System.arraycopy(buff, cmdTableOffset + 4*i, buff4byte, 0, 4);
			cmdOffsets[i] = (int)ByteUtils.getLongValue(buff4byte);
		}
	}
	
	/**
	 * 解析一行
	 */
	public void decod(int row, int outlineTableOffset){
		Arrays.fill(img256RowBuff, (byte)0x0);
		Arrays.fill(featureRowBuff, (byte)0x0);
		
		int cmdOffset = cmdOffsets[row];
		
		// 得到每行的边界 left 和 right
		System.arraycopy(buff, outlineTableOffset + (row * 4), buff4byte, 0, 2);
		int left = (int) ByteUtils.getShortValue(buff4byte);

		// left 前都是透明的
		if(left < rowWidth){
			col = left;
			int cmd = 0x0FF & buff[cmdOffset++];
			
			while (cmd != 0x0F) {
				// 为了压缩命令，0x00、0x01、0x02 命令的判断依据是最低的两位
				// 例如: 0x10 -> 0001 0000 和 0x14 -> 0001 0100 都属于  0x00 命令
				switch(cmd & 0x0F){
				case 0x06:
					cmdOffset = cmdPlayerColorList(cmd, cmdOffset);
					break;
				case 0x07:
					cmdOffset = cmdFill(cmd, cmdOffset);
					break;
				case 0x0A:
					cmdOffset = cmdPlayerColorFill(cmd, cmdOffset);
					break;
				case 0x0B:
					cmdOffset = cmdShadowTransparent(cmd, cmdOffset);
					break;
				case 0x0E:
					cmdOffset = cmdOther(cmd, cmdOffset);
					break;
				default:
					cmdOffset = cmdCommon(cmd, cmdOffset);
					break;
				}
				cmd = 0x0FF & buff[cmdOffset++];
			}
		}
	}
	
	private int cmdCommon(int cmd, int cmdOffset){
		switch (cmd & 0x03) {
			case 0x00: 
				cmdOffset = cmdColorList(cmd, cmdOffset);
				break;
			case 0x01:
				cmdOffset = cmdSkip(cmd, cmdOffset);
				break;
			case 0x02:
				cmdOffset = cmdBigColorList(cmd, cmdOffset);
				break;
			case 0x03:
				cmdOffset = cmdBigSkip(cmd, cmdOffset);
				break;
			default:
				System.out.println("Unknow cmd:" + ByteUtils.getHexString((byte)cmd) + ", cmdOffset:" + (cmdOffset-1));
			}
			return cmdOffset;
	}
	
	/**
	 * 指定后面多少个调色板索引值
	 * Value: 0x00
	 * Command name: Color list
	 * Pixel count: >>2
	 * Description: An array of palette indices. This is about as bitmap as it gets in SLPs.
	 */
	private int cmdColorList(int cmd, int cmdOffset){
		int count = cmd >> 2;
		for(int i=0; i<count; i++){
			byte colorValue = buff[cmdOffset++];
			img256RowBuff[col] = colorValue;
			featureRowBuff[col] = (byte)0xFF;
			col++;
		}
		return cmdOffset;
	}
	
	/**
	 * 指定要跳过多少个透明色
	 * Value: 0x01
	 * Command name: Skip
	 * Pixel count: >>2 or next
	 * Description: The specified number of pixels are transparent.
	 */
	private int cmdSkip(int cmd, int cmdOffset){
		int count = cmd >> 2;
		if(count == 0){
			count = 0x0FF & buff[cmdOffset++];
		}
		col += count;
		return cmdOffset;
	}
	
	/**
	 * 指定后面多少个调色板索引值
	 * Value: 0x02
	 * Command name: Big color list
	 * Pixel count: >>4 * 256 and next
	 * Description: An array of palette indexes. Supports a greater number of pixels than the above color list.
	 */
	private int cmdBigColorList(int cmd, int cmdOffset){
		int count = (cmd >> 4) * 256 + (0x0FF & buff[cmdOffset++]);
		for(int i=0; i<count; i++){
			byte colorValue = buff[cmdOffset++];
			img256RowBuff[col] = colorValue;
			featureRowBuff[col] = (byte)0xFF;
			col++;
		}
		return cmdOffset;
	}
	
	/**
	 * 指定要跳过多少个透明色
	 * Value: 0x03
	 * Command name: Big skip
	 * Pixel count: >>4 * 256 and next
	 * Description: The specified number of pixels are transparent. Supports a greater number of pixels than the above skip.
	 */
	private int cmdBigSkip(int cmd, int cmdOffset){
		int count = (cmd >> 4) * 256 + (0x0FF & buff[cmdOffset++]);
		col += count;
		return cmdOffset;
	}
	
	/**
	 * 指定后面多少个玩家颜色信息
	 * Value: 0x06
	 * Command name: Player color list
	 * Pixel count: >>4 or next
	 * Description: An array of player color indexes. The actual palette index is given by adding ([player number] * 16) + 16 to these values.
	 */
	private int cmdPlayerColorList(int cmd, int cmdOffset){
		int count = cmd >> 4;
		if(count == 0){
			count = 0x0FF & buff[cmdOffset++];
		}
		for(int i=0; i<count; i++){
			byte colorValue = buff[cmdOffset++];
			img256RowBuff[col] = colorValue;
			featureRowBuff[col] = (byte)(colorValue + 0x10);
			col++;
		}
		return cmdOffset;
	}
	
	/**
	 * 用指定颜色填充多少个点的信息
	 * Value: 0x07
	 * Command name: Fill
	 * Pixel count: >>4 or next
	 * Description: Fills the specified number of pixels with the following palette index.
	 */
	private int cmdFill(int cmd, int cmdOffset){
		int count = cmd >> 4;
		if(count == 0){
			count = 0x0FF & buff[cmdOffset++];
		}
		byte colorValue = buff[cmdOffset++];
		for(int i=0; i<count; i++){
			img256RowBuff[col] = colorValue;
			featureRowBuff[col] = (byte)0xFF;
			col++;
		}
		return cmdOffset;
	}
	
	/**
	 * 用指定颜色填充多少个点的信息
	 * Value: 0x0A
	 * Command name: Player color fill
	 * Pixel count: >>4 or next
	 * Description: Same as above, but using the player color formula (see Player color list).
	 */
	private int cmdPlayerColorFill(int cmd, int cmdOffset){
		int count = cmd >> 4;
		if(count == 0){
			count = 0x0FF & buff[cmdOffset++];
		}
		byte colorValue = buff[cmdOffset++];
		for(int i=0; i<count; i++){
			img256RowBuff[col] = colorValue;
			featureRowBuff[col] = (byte)(colorValue + 0x10);
			col++;
		}
		return cmdOffset;
	}
	
	/**
	 * 指定一个单位的阴影
	 * Value: 0x0B
	 * Command name: Shadow transparent
	 * Pixel count: >>4 or next
	 * Description: Specifies the shadow for a unit, but most SLPs do not use this.
	 */
	private int cmdShadowTransparent(int cmd, int cmdOffset){
		int count = cmd >> 4;
		if(count == 0){
			count = 0x0FF & buff[cmdOffset++];
		}
		col += count;
		return cmdOffset;
	}
	
	private int cmdOther(int cmd, int cmdOffset){
		switch (cmd) {
			case 0x0E: 
				cmdOffset = cmdShadowPlayer(cmd, cmdOffset);
				break;
			case 0x4E:
				cmdOffset = cmdOutline(cmd, cmdOffset);
				break;
			case 0x5E:
				cmdOffset = cmdOutlineSpan(cmd, cmdOffset);
				break;
			default:
				System.out.println("Unknow cmd:" + ByteUtils.getHexString((byte)cmd) + ", cmdOffset:" + (cmdOffset-1));
			}
			return cmdOffset;
	}
	
	/**
	 * 未知
	 * Value: 0x0E
	 * Command name: Shadow player
	 * Pixel count: next
	 * Description: Unknown.
	 */
	private int cmdShadowPlayer(int cmd, int cmdOffset){
		System.out.println("Shadow player");
		cmdOffset++;
		return cmdOffset;
	}
	
	/**
	 * 边界
	 * Value: 0x4E
	 * Command name: Outline
	 * Pixel count: 1
	 * Description: When normally drawing a unit, this single pixel acts normally. When the unit is behind something, this pixel is replaced with the player color.
	 */
	private int cmdOutline(int cmd, int cmdOffset){
		featureRowBuff[col] = (byte)0xEE;
		col++;
		return cmdOffset;
	}
	
	/**
	 * 边界
	 * Value: 0x5E
	 * Command name: Outline span
	 * Pixel count: next
	 * Description: Same function as above, but supports multiple pixels.
	 */
	private int cmdOutlineSpan(int cmd, int cmdOffset){
		int count = 0x0FF & buff[cmdOffset++];
		for(int i=0; i<count; i++){
			featureRowBuff[col] = (byte)0xEE;
			col++;
		}
		return cmdOffset;
	}
}
