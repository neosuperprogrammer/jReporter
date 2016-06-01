package flowgrammer.exception;

import java.sql.SQLException;

public class JReporterException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JReporterException(String string) {
		super(string);
	}

	public JReporterException(Exception e) {
		super(e);
	}

}
