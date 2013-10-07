package net.bookong.tools.archive;

/**
 * 打开文件失败的异常
 * @author jiangxu
 *
 */
public class OpenFileException extends RuntimeException {
	private static final long serialVersionUID = 6064328909430543710L;
	
	public OpenFileException(String message) {
		super(message);
	}

	public OpenFileException(String message, Throwable cause) {
		super(message, cause);
	}
}
