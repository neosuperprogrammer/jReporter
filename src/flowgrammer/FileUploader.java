package flowgrammer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import flowgrammer.DAO.JReporterDAO;
import flowgrammer.exception.JReporterException;

public class FileUploader {

	private static final int maxFileSize = 5 * 1024 * 1024;
	private static final int maxMemSize = 4 * 1024;
	
	static Logger log = Logger.getLogger(FileUploader.class);
	
	public static void processRequest(HttpServletRequest req,
			HttpServletResponse resp, String tempFilePath, String filePath) throws IOException {
		File file = null ;
		String cmd = "";
		String title = "";
		
		HttpSession session = req.getSession();
		Integer sessionId = (Integer) session.getAttribute("IdUser");
		if (sessionId == null) {
			errorAuth(resp);
			return;
		}
		int idUser = sessionId;
		
		// Check that we have a file upload request
//		resp.setContentType("text/html");
//		java.io.PrintWriter out = response.getWriter( );
//		if( !isMultipart ){
//			out.println("<html>");
//			out.println("<head>");
//			out.println("<title>Servlet upload</title>");  
//			out.println("</head>");
//			out.println("<body>");
//			out.println("<p>No file uploaded</p>"); 
//			out.println("</body>");
//			out.println("</html>");
//			return;
//		}
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// maximum size that will be stored in memory
		factory.setSizeThreshold(maxMemSize);
		// Location to save data that is larger than maxMemSize.
		factory.setRepository(new File(tempFilePath));

		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);
		// maximum file size to be uploaded.
		upload.setSizeMax( maxFileSize );

		try{ 
			// Parse the request to get file items.
			List fileItems = upload.parseRequest(req);

			// Process the uploaded file items
			Iterator i = fileItems.iterator();
			while ( i.hasNext () ) 
			{
				FileItem fi = (FileItem)i.next();
				if ( !fi.isFormField () )	
				{
					// Get the uploaded file parameters
					String fieldName = fi.getFieldName();
					String fileName = fi.getName();
					String contentType = fi.getContentType();
					boolean isInMemory = fi.isInMemory();
					long sizeInBytes = fi.getSize();
					// Write the file
					if( fileName.lastIndexOf("\\") >= 0 ){
						file = new File( tempFilePath + 
								fileName.substring( fileName.lastIndexOf("\\"))) ;
					}else{
						file = new File( tempFilePath + 
								fileName.substring(fileName.lastIndexOf("\\")+1)) ;
					}
					fi.write( file ) ;
				}
				else {
					String fieldName = fi.getFieldName();
					String fieldValue = fi.getString();
					
					System.out.println("name : " + fieldName + ", value : " + fieldValue);
					if (fieldName.equals("command")) {
						cmd = fieldValue;
					}
					if (fieldName.equals("title")) {
						title = fieldValue;
					}
//					if (fieldName.equals("idUser")) {
//						idUser = Integer.valueOf(fieldValue);
//					}
				}
			}
			
			Connection conn = JReporterDAO.getConnection();
			int fileId = -1;
			try {
				fileId = JReporterDAO.upload(conn, idUser, title);
			}
			catch (JReporterException ex) {
				errorJson(resp, "JReporterDAO.upload failed : " + ex.getMessage());
				conn.rollback();
				conn.close();
				return;
			}
			
			if (fileId < 0) {
				errorJson(resp, "file id is wrong, fileId : " + fileId);
				conn.rollback();
				conn.close();
				return;
			}
			
			String fileLoc = filePath + fileId + ".jpg";
			if (!file.renameTo(new File(fileLoc))) {
				errorJson(resp, "file rename failed, loc : " + fileLoc);
				file.delete();
				conn.rollback();
				conn.close();
				return;
			}
			try {
				thumbnail(fileLoc, 180, 180);
			}
			catch (JReporterException ex) {
				errorJson(resp, "thumbnail faile : " + ex.getMessage());
				file.delete();
				conn.rollback();
				conn.close();
				return;
			}
			
			conn.commit();
			conn.setAutoCommit(true);
			conn.close();
			
			successJson(resp);
		}
		catch(Exception ex) {
			errorJson(resp, ex.getMessage());
		}
	}
	
	private static void successJson(HttpServletResponse resp) throws IOException {
		resp.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		PrintWriter out = resp.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("successful", "1");
//		String jsonObject = "{\"successful\":\"1\"}";
		out.print(jsonObject.toString());
		out.flush();
	}

	public static void errorJson(ServletResponse resp, String error) throws IOException {
		log.error(error);
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("error", error);			
		out.print(jsonObject.toString());
		out.flush();
	}
	
	private static void errorAuth(ServletResponse resp) throws IOException {
		System.out.println("auth failed!!!");
		errorJson(resp, "Authorization required");
	}
	
	private static void thumbnail(String imageLoc, int width, int height) {
        try {
            // Read the original image from the Server Location
            BufferedImage bufferedImage = ImageIO.read(new File(imageLoc));
            // Calculate the new Height if not specified
            int calcHeight = height > 0 ? height : (width * bufferedImage.getHeight() / bufferedImage.getWidth());
            String thumbPath = imageLoc.replace(".jpg", "-thumb.jpg");
			// Write the image
//            ImageIO.write(createResizedCopy(bufferedImage, width, calcHeight), imageOutput, response.getOutputStream());
			ImageIO.write(createResizedCopy(bufferedImage, width, height), "jpg", new File(thumbPath)); 
        } catch (Exception e) {
        	throw new JReporterException(e);
        }
	}
	
    private static BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight) {
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledBI.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }
}
