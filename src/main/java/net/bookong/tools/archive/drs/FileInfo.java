package net.bookong.tools.archive.drs;

/**
 * 打包在 DRS 文件中的文件信息
 * @author jiangxu
 *
 */
public class FileInfo {
	/** 文件编号 */
	public long fileNo;
	/** 偏移量 */
	long offset;
	/** 文件长度 */
	int len;
}
