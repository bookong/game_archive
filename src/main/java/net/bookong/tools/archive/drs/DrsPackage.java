package net.bookong.tools.archive.drs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.bookong.tools.archive.OpenFileException;
import net.bookong.tools.archive.ReadFileException;
import net.bookong.tools.utils.ByteUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 读取 DRS 扩展名的包文件
 * 
 * @author jiangxu
 *
 */
public class DrsPackage {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**　文件头　*/
	private static byte[] FILE_HEAD = {
		0x43, 0x6f, 0x70, 0x79, 0x72, 0x69, 0x67, 0x68, 0x74, 0x20, 0x28, 0x63, 0x29, 0x20, 0x31, 0x39,	//Copyright (c) 19
		0x39, 0x37, 0x20, 0x45, 0x6E, 0x73, 0x65, 0x6D, 0x62, 0x6C, 0x65, 0x20, 0x53, 0x74, 0x75, 0x64,	//97 Ensemble Stud
		0x69, 0x6F, 0x73, 0x2E, 0x1A, 0x00, 0x00, 0x00, 0x31, 0x2E, 0x30, 0x30, 0x74, 0x72, 0x69, 0x62,	//ios.....1.00trib
		0x65, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00													//e.......
	};
	private static byte[] BUFF_4_BYTE = new byte[4];
	private String packageFileName;
	private boolean open;
	private RandomAccessFile randomAccessFile;
	private Map<Long, FileInfo> dataFiles = new HashMap<Long, FileInfo>();

	/**
	 * 打开数据包
	 * @param file 数据包文件
	 */
	public void open(File file) {
		logger.info("Start to open {} ...", file.getAbsolutePath());
		close();
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			if(file.length() < 60){
				throw new OpenFileException("File format error:" + file.getAbsolutePath());
			}
			byte[] b = new byte[FILE_HEAD.length];
			randomAccessFile.read(b, 0, FILE_HEAD.length);
			if(!Arrays.equals(FILE_HEAD, b)){
				throw new OpenFileException("File format error(file header does not match):" + file.getAbsolutePath());
			}
			packageFileName = file.getName();
			loadDrsInfo();
			open = true;
			logger.info("Open the file {} successfully", packageFileName);
		} catch (Exception e) {
			throw new OpenFileException("Fail to open file:" + file.getAbsolutePath(), e);
		}
	}
	
	/**
	 * 读取文件数据
	 * @param fileNo 文件编号
	 * @return 返回读取文件的内容
	 */
	public byte[] readFileData(Long fileNo) {
		if(!isOpen()){
			throw new ReadFileException("Must open drs file.");
		}
		
		FileInfo fileInfo = dataFiles.get(fileNo);
		if(fileInfo == null){
			throw new ReadFileException("Can not find data file. fileNo:" + fileNo);
		}

		try {
			randomAccessFile.seek(fileInfo.offset);
			byte[] buff = new byte[fileInfo.len];
			randomAccessFile.read(buff, 0, fileInfo.len);
			return buff;
		} catch (IOException e) {
			throw new ReadFileException("Fail to read data file. fileNo:" + fileNo, e);
		}
	}
	
	/**
	 * 关闭数据包
	 */
	public void close() {
		if(!open){
			return;
		}
		
		if(randomAccessFile != null){
			try {
				randomAccessFile.close();
			} catch (IOException e) {
				logger.warn("Fail to close file input stream.");
			}
		}
		
		dataFiles.clear();
		open = false;
		logger.info("Close the file {} successfully", packageFileName);
	}
	
	private void loadDrsInfo() throws Exception {
		long dataTableNo = readLongValue();
		long firstFileOffset = readLongValue();
		
		if(logger.isDebugEnabled()){
			logger.debug("data table number:{}", dataTableNo);
			logger.debug("first file offset:{}", firstFileOffset);
		}
		
		List<DataTable> dataTables = new ArrayList<DataTable>();
		for(int i=0; i<dataTableNo; i++){
			if(logger.isDebugEnabled()){
				logger.debug("loading data table info ... ({}/{})", (i+1), dataTableNo);
			}
			
			DataTable dt = new DataTable();
			dataTables.add(dt);
			dt.dataType = readStringValue().trim();
			dt.dataOffset = readLongValue();
			dt.dataFileCount = readLongValue();
			
			if(logger.isDebugEnabled()){
				logger.debug("\tdata type:{}", dt.dataType);
				logger.debug("\tdata offset:{}", dt.dataOffset);
				logger.debug("\tdata file count:{}", dt.dataFileCount);
			}
		}
		
		for (DataTable dt : dataTables) {
			for(int i=0; i<dt.dataFileCount; i++){
				if(logger.isDebugEnabled()){
					logger.debug("loading data file ({}) info ... ({}/{})", dt.dataType, (i + 1), dt.dataFileCount);
				}
				
				FileInfo fi = new FileInfo();
				fi.fileNo = readLongValue();
				fi.offset = readLongValue();
				fi.len = (int)readLongValue();
				dataFiles.put(fi.fileNo, fi);
				
				if(logger.isDebugEnabled()){
					logger.debug("\tfile number:{}", fi.fileNo);
					logger.debug("\tdata offset:{}", fi.offset);
					logger.debug("\tfile length:{}", fi.len);
				}
			}
		}
	}
	
	private long readLongValue() throws Exception {
		randomAccessFile.read(BUFF_4_BYTE);
		return ByteUtils.getLongValue(BUFF_4_BYTE);
	}
	
	private String readStringValue() throws Exception {
		randomAccessFile.read(BUFF_4_BYTE);
		return new String(BUFF_4_BYTE);
	}
	
	private class DataTable{
		/** 数据类型 */
		String dataType;
		/** 数据的偏移量 */
		long dataOffset;
		/** 数据表里的文件数量 */
		long dataFileCount;
	}
	
	public boolean isOpen() {
		return open;
	}
	
	public Map<Long, FileInfo> getDataFiles() {
		return dataFiles;
	}
}
