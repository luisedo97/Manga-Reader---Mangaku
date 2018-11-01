/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import model.ChapterModel;
import util.DBAccess;
import util.JacksonMapper;
import util.PropertiesReader;
import util.Validator;

/**
 *
 * @author Usuario
 */
@MultipartConfig
public class ChapterFacade {
    private DBAccess db;
    private PropertiesReader pReader;
    private JacksonMapper jackson;
    private Validator validator;

    public ChapterFacade(){
        db = null;
        pReader = null;
        jackson = null;
        validator = null;
    }
    
    public ChapterModel chapterRequestValid(HttpServletRequest request, String st) throws IOException{ //Listo
        jackson = new JacksonMapper();
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        validator = new Validator();
        ResultSet rs = null;
        ChapterModel cm = null;
       // if (validator.sessionExists(request.getSession())){
            cm = jackson.jsonToPojo(st, ChapterModel.class);
            rs = db.execute(pReader.getValue("qca1"), cm.getMangaId(),cm.getChapterNumber());
            
            try {
                System.out.println(rs.next());
                if(!rs.next()){
                    return cm;   
                }

            } catch (SQLException ex) {
                Logger.getLogger(ChapterFacade.class.getName()).log(Level.SEVERE, null, ex);
            }
        //}
       
        return null;
    }
    
    public String chapterCreate(HttpServletRequest request, String str) throws IOException, ServletException{ //Refactorizar
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        jackson = new JacksonMapper();
        ChapterModel cm = chapterRequestValid(request, str);
        if (cm != null){
            if(fileUpload(request,cm)){
                this.requestCreate(request.getSession(), cm, db, pReader, true); //Aqui van datos de session
                db.close();
                return "200";
            }
        }
        return "500";
    }

    public ChapterModel chapterGet(HttpServletRequest request, String str) throws IOException, SQLException{
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        jackson = new JacksonMapper();
        ChapterModel cm = jackson.jsonToPojo(str, ChapterModel.class);
        if (cm != null){
            cm = requestGet(cm, db, pReader);
            db.close();
            return cm;
        }
        return cm;        
     }

    public String chapterUpdate(HttpServletRequest request, String str) throws IOException, ServletException{
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        validator = new Validator();
        ChapterModel cm = chapterRequestValid(request, str);
        
        if(cm != null){
            if(fileUpload(request,cm)){
                this.requestCreate(request.getSession(), cm, db, pReader, true); //Aqui van datos de session
                db.close();
                return "200";
            }
        }
        return "500";
}

    public String chapterDelete(HttpServletRequest request) throws IOException, ServletException{
        pReader = PropertiesReader.getInstance();
        db = new DBAccess(pReader.getValue("dbDriver"),pReader.getValue("dbUrl"),pReader.getValue("dbUser"),pReader.getValue("dbPassword"));
        validator = new Validator();
        jackson = new JacksonMapper();
        ChapterModel cm = jackson.jsonToPojo(request, ChapterModel.class);
            if(cm != null){
                this.requestDelete(request.getSession(), cm, db, pReader, false); //Aqui van datos de la session
                db.close();
                return "200";
            }
        return "500";
}
    
    private void requestUpdate(HttpSession session, ChapterModel cm, DBAccess db, PropertiesReader pReader, boolean isAdmin) 
        throws IOException, ServletException{
        if(isAdmin){
        db.update(pReader.getValue("qcx1"), cm.getChapterNumber(),cm.getChapterName(),cm.getChapterLocation(),cm.getChapterPages(),cm.getMangaId(),cm.getChapterNumber());
        }else{
        db.update(pReader.getValue("qcu1"), cm.getChapterNumber(),cm.getChapterName(),cm.getChapterLocation(),cm.getChapterPages(),cm.getMangaId(),cm.getChapterNumber(), 51); //Aqui van datos de session    
        }
}
    private void requestDelete(HttpSession session, ChapterModel cm, DBAccess db, PropertiesReader pReader, boolean isAdmin)
            throws IOException, ServletException{
        if(isAdmin){
                db.update(pReader.getValue("qcx2"), cm.getMangaId(), cm.getChapterNumber());
        }else{
                db.update(pReader.getValue("qcu2"), cm.getMangaId(), cm.getChapterNumber(),51); //Aqui van los valores de sesion
        }    
    }
    
    private void requestCreate(HttpSession session, ChapterModel cm, DBAccess db, PropertiesReader pReader, boolean isAdmin)
            throws IOException, ServletException{
        if (isAdmin){
        db.update(pReader.getValue("qcx3"), cm.getMangaId(),cm.getChapterNumber(),cm.getChapterName(),cm.getChapterLocation(),cm.getChapterPages());    
        }else{
        db.update(pReader.getValue("qcu3"), 51, cm.getMangaId(),cm.getChapterNumber(),cm.getChapterName(),cm.getChapterLocation(),cm.getChapterPages());    //Aqui van datos de session    
        }
    }
    
    private ChapterModel requestGet(ChapterModel cm, DBAccess db, PropertiesReader pReader) throws SQLException{
    ResultSet rs = null;
    rs = db.execute(pReader.getValue("qca2"), cm.getChapterNumber(),cm.getMangaId());
            try {
                if(rs.next()){
                    cm = new ChapterModel();
                    cm.setChapterLocation(rs.getString(1));
                    cm.setChapterPages(rs.getInt(2));
                    return cm;   
                }
            } catch (SQLException ex) {
                Logger.getLogger(ChapterFacade.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
    }
    
    private String getFileName(Part part) {
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}  
    
    private boolean fileUpload(HttpServletRequest request, ChapterModel cm) throws IOException, ServletException{
                Collection<Part> files = request.getParts();
            InputStream filecontent = null;
            OutputStream os = null;
                int i = 0;
		try {
			String baseDir = request.getServletContext().getRealPath("/manga");
                        StringBuilder mangaDir = new StringBuilder();
                        mangaDir.append(baseDir).append("/").append(cm.getMangaName().toLowerCase()).append("/").append(cm.getChapterNumber());
                        System.out.println(mangaDir.toString());
                        this.createFolder(mangaDir.toString());
                        cm.setChapterLocation(mangaDir.toString());
			for (Part file : files) {
                                if(this.getFileName(file) != null){
                                    String finalDir = mangaDir.toString() + "/" + String.valueOf(i+1).concat(".jpg");
                                    filecontent = file.getInputStream();
                                    os = new FileOutputStream(finalDir);
                                    int read = 0;
                                    byte[] bytes = new byte[1024];
                                        while ((read = filecontent.read(bytes)) != -1) {
                                                os.write(bytes, 0, read);
                                        }
				if (filecontent != null) {
					filecontent.close();
				}
				if (os != null) {
					os.close();
				}
                                i++;
			}
                        }
                        cm.setChapterPages(i);
                    return true;    
		} catch (Exception e) {
			e.printStackTrace();
                return false;
                }
    }
        
    public void createFolder(String str){
        new File(str).mkdirs();
    }
    
    public void downloadFile(HttpServletRequest req, HttpServletResponse resp) throws FileNotFoundException, IOException{
                    // Get the absolute path of the image
         ServletContext sc = req.getServletContext();
         String filename = sc.getRealPath("image.gif");

         // Get the MIME type of the image
         String mimeType = sc.getMimeType(filename);
         if (mimeType == null) {
             sc.log("Could not get MIME type of "+filename);
             resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
             return;
         }
         // Set content type
         resp.setContentType(mimeType);

         // Set content size
         File file = new File(filename);
         resp.setContentLength((int)file.length());

         // Open the file and output streams
         FileInputStream in = new FileInputStream(file);
         OutputStream out = resp.getOutputStream();

         // Copy the contents of the file to the output stream
         byte[] buf = new byte[1024];
         int count = 0;
         while ((count = in.read(buf)) >= 0) {
             out.write(buf, 0, count);
         }
         in.close();
         out.close();
    }
    public <T> String writeJSON(T json) throws JsonProcessingException{
    jackson = new JacksonMapper();    
    return jackson.pojoToJson(json);
    }
}