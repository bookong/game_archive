package net.bookong.tools.archive;

/**
 * 读取文件的异常
 * 
 * @author jiangxu
 *
 */
public class ReadFileException extends RuntimeException {
	private static final long serialVersionUID = 197913103332378092L;
	
	public ReadFileException(String message) {
		super(message);
	}

	public ReadFileException(String message, Throwable cause) {
		super(message, cause);
	}
}
