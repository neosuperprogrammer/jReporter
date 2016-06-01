package flowgrammer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import flowgrammer.DAO.JReporterDAO;
import flowgrammer.Model.Photo;
import flowgrammer.exception.JReporterException;
import flowgrammer.setting.Global;

public class MainController extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger log = Logger.getLogger(MainController.class);
	
	private String filePath;
	private String tempFilePath;
	
//	@Override
//	public void init() throws ServletException {
//		super.init();
//		filePath = getServletContext().getInitParameter("file-upload"); 
//		filePath = getServletContext().getRealPath("") + filePath;
//
//		tempFilePath = getServletContext().getInitParameter("file-upload-temp"); 
//		tempFilePath = getServletContext().getRealPath("") + tempFilePath;
//		
////		System.out.println("temp file path : " + tempFilePath);
////		System.out.println("file path : " + filePath);
//		
//	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		
		if (Global.isTestServer) {
			filePath = config.getServletContext().getInitParameter("file-upload-test"); 
			filePath = config.getServletContext().getRealPath("") + filePath;
		}
		else {
			filePath = config.getServletContext().getInitParameter("file-upload"); 
		}

		tempFilePath = config.getServletContext().getInitParameter("file-upload-temp"); 
		tempFilePath = config.getServletContext().getRealPath("") + tempFilePath;
		
		
		String realPath = config.getServletContext().getRealPath("");
		log.info("rea path : " + realPath);
		
//		ApplicationContext context = new FileSystemXmlApplicationContext(realPath + "/WEB-INF/jReporter-servlet.xml");
		
		super.init(config);
		
		log.info("service start!!!");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doProcess(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doProcess(req, resp);
	}

	private void doProcess(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("utf-8");
		resp.setCharacterEncoding("utf-8");
		

//		log.debug("debug message");
//		log.info("info message");
//		log.warn("warn message");
//		log.error("error message");
//		log.fatal("fatal message");

		HttpSession session = req.getSession();
		Integer idUser = (Integer) session.getAttribute("IdUser");
		
		System.out.println("IdUser : " + idUser);
		
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (isMultipart) {
			FileUploader.processRequest(req, resp, tempFilePath, filePath);
			return;
		}
		
		printRequestParam(req);

		String cmd = req.getParameter("command");
		if (cmd == null) {
			errorJson(resp, "command is unknown : " + cmd);
			return;
		}
		
		if (cmd.equals("login")) {
			processLogin(req, resp);
		}
		else if (cmd.equals("register")) {
			processRegister(req, resp);
		}
		else if (cmd.equals("stream")) {
			processStream(req, resp);
		}
		else if (cmd.equals("logout")) {
			processLogout(req, resp);
		}
		else {
			errorJson(resp, "command not found : " + cmd);
		}
	}

	private void processLogout(HttpServletRequest req, HttpServletResponse resp) {
		HttpSession session = req.getSession();
		session.removeAttribute("IdUser");
		session.invalidate();
	}

	private void processStream(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String idPhoto = req.getParameter("IdPhoto");
		HttpSession session = req.getSession();
		Object idUserSession = session.getAttribute("IdUser");
		int idUser = -1;
		if (idUserSession != null) {
			idUser = (Integer) idUser;
		}
		
		ArrayList<Photo> photoList = JReporterDAO.stream(idUser, idPhoto);

		if (photoList == null) {
			errorJson(resp, "Photo stream is broken");
		}
		else {
			successStream(resp, photoList);
		}
	}

	private void successStream(HttpServletResponse resp, ArrayList<Photo> photoList) throws IOException {
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		
		for (Photo photo : photoList) {
			JSONObject jsonPhoto = new JSONObject();
			jsonPhoto.put("IdPhoto", photo.IdPhoto);
			jsonPhoto.put("username", photo.username);
			jsonPhoto.put("title", photo.title);
			jsonArray.put(jsonPhoto);
		}
		jsonObject.put("result", jsonArray);
		System.out.println("json : " + jsonObject.toString());
		
		
		
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();
		out.print(jsonObject.toString());
		out.flush();
	}

	private void processRegister(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		String id = req.getParameter("username");
		String passwd = req.getParameter("password");
		
		try {
			if (!JReporterDAO.register(id, passwd)) {
				errorJson(resp, "register failed!!!");
			}
			else {
				processLogin(req, resp);
			}
		}
		catch (JReporterException e) {
			errorJson(resp, e.getMessage());
		}
	}

	private void processLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String id = req.getParameter("username");
		String passwd = req.getParameter("password");
		
		int userId = JReporterDAO.login(id, passwd);
		if (userId < 0) {
			errorJson(resp, "login failed!!!");
		}
		else {
			HttpSession session = req.getSession();
			session.setAttribute("IdUser", userId);
			successLogin(resp, userId, id);
		}
	}

	private void successLogin(HttpServletResponse resp, int userId, String userName) throws IOException {
//		System.out.println("login success");
		resp.setContentType("application/json");
		// Get the printwriter object from response to write the required json object to the output stream      
		PrintWriter out = resp.getWriter();
		// Assuming your json object is **jsonObject**, perform the following, it will return your json object
//		String jsonObject = "{\"result\":[{\"IdUser\":\"" + userId + "\",\"username\":\"" + userName + "\"},{}]}";
		JSONObject jsonObject = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject userObject = new JSONObject();
		userObject.put("IdUser", userId);
		userObject.put("username", userName);
		jsonArray.put(userObject);
		jsonObject.put("result", jsonArray);
		out.print(jsonObject.toString());
		out.flush();
	}

	private void errorJson(HttpServletResponse resp, String errorString) throws IOException {
		resp.setContentType("application/json");
		PrintWriter out = resp.getWriter();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("error", errorString);
//		String jsonObject = "{\"error\":\"" + errorString + "\"}";
		out.print(jsonObj.toString());
		out.flush();
	}

	private void printRequestParam(HttpServletRequest req) {
		Map<String, String[]> map = req.getParameterMap();	//파라미터 맵을 받아옴
		Iterator<String> iterator = map.keySet().iterator();				//맵의 모든 키를 받아옴
		while(iterator.hasNext()) {
			String key = iterator.next();									//파라미터의 키
			String values[] = map.get(key);								//파라미터 값
			String value = "";												//푸시 데이터 값
			if(values != null) {
				for(String v : values)
					value += v+",";											//배열을 a,b,c,d 형태의 문자열로 만듬
				value = value.substring(0, value.length()-1);
			}
			
			System.out.println("key : " + key + ", value : " + value);
		}
	}
}
