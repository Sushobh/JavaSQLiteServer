package org.sushobh;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sqlite.jdbc4.JDBC4ResultSet;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

public class SQLiteServer {

    private int portNumber;
    private Connection sqlConnection;
    private HttpServer server;
    public static final   String DATA_BASE_FILE_NAME = "MyDatabase.db";

    public SQLiteServer(int portNumber) {
        this.portNumber = portNumber;
    }

    public void startServer() throws SQLException,IOException {
        if(server == null){
            server = HttpServer.create(new InetSocketAddress(portNumber), 0);
            server.createContext("/",new MainPageHandler());
            server.createContext("/showtables", new AllTablesHandler());
            server.createContext("/table",new IndividualTableHandler());
            server.createContext("/query",new QueryHandler());
            server.createContext("/code.js",new JavaScriptCodeRequestHandler());
            HttpContext uploadContext =  server.createContext("/upload",new DatabaseUploadRequestHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
        }
        System.out.println("Http Server running!");

    }

    public void connectToDatabase(String databasePath) throws SQLException {
        sqlConnection = DriverManager.getConnection("jdbc:sqlite:"+databasePath);
        System.out.println("Connected to database!");
    }


    private class AllTablesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            String response = createJSONStringWithAllTableNames();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

        }
    }

    public File getDBFileToCopyInto() throws IOException {
        File file = new File(System.getProperty("user.dir")+"\\"+DATA_BASE_FILE_NAME);
        if(file.exists()){
            file.delete();
        }
        else
        {

        }
        return file;
    }


    private class DatabaseUploadRequestHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange t) throws IOException {

            DiskFileItemFactory d = new DiskFileItemFactory();

            try {
                ServletFileUpload up = new ServletFileUpload(d);
                List<FileItem> result = up.parseRequest(new RequestContext() {

                    @Override
                    public String getCharacterEncoding() {
                        return "UTF-8";
                    }

                    @Override
                    public int getContentLength() {
                        return 0; //tested to work with 0 as return
                    }

                    @Override
                    public String getContentType() {
                        return t.getRequestHeaders().getFirst("Content-type");
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return t.getRequestBody();
                    }
                });
                t.getResponseHeaders().add("Content-type", "text/plain");
                t.sendResponseHeaders(200, 0);

                for(FileItem fileItem : result){
                    if(fileItem.getFieldName().equalsIgnoreCase("fileData")){
                        byte[] data = Base64.getMimeDecoder().decode(fileItem.get());
                        try (OutputStream stream = new FileOutputStream(getDBFileToCopyInto())) {
                            stream.write(data);
                        }
                        break;
                    }
                }
                OutputStream os = t.getResponseBody();
                os.write("\"{status : true}\"".getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
                t.sendResponseHeaders(200, 0);
                t.getResponseHeaders().add("Content-type", "text/plain");
                OutputStream os = t.getResponseBody();
                os.write("\"{status : false}\"".getBytes());
                os.close();
            }
        }
    }



    private class  JavaScriptCodeRequestHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange t) throws IOException {

            String response = readFile("code.js",Charset.defaultCharset());
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private class IndividualTableHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange t) throws IOException {
            Map<String,String> queryMap = getQueryMap(t.getRequestURI().getQuery());
            String response = "";
            for(Map.Entry<String,String> entry : queryMap.entrySet()){
                if(entry.getKey().equals("tablename")){
                    response = createJSONStringWithTableData(entry.getValue());
                    break;
                }
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }




   private class QueryHandler implements  HttpHandler {

       @Override
       public void handle(HttpExchange t) throws IOException {
           String requestBody = convertStreamToString(t.getRequestBody());
           Map<String,String> queryMap = getQueryMap(requestBody);
           String query = URLDecoder.decode(queryMap.get("query"),"UTF-8");
           String response =  createJSONStringWithQueryResultData(query);
           t.sendResponseHeaders(200, response.length());
           OutputStream os = t.getResponseBody();
           os.write(response.getBytes());
           os.close();
       }
   }

    private class MainPageHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = createMainPageStringHtml();
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());

            File file = new File(Util.getPathAtWorkingDirectory(DATA_BASE_FILE_NAME));
            if(file.exists()){
                try {
                    connectToDatabase(Util.getPathAtWorkingDirectory(DATA_BASE_FILE_NAME));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            os.close();
        }
    }


    private  String createJSONStringWithAllTableNames() {
        List<String> tableNames = getAllTableNames();
        JSONArray jsonArray = new JSONArray();
        for(String tableName : tableNames){
            jsonArray.put(tableName);
        }
        return jsonArray.toString();
    }



    private  List<String> getAllTableNames() {

        List<String> tableNames = new ArrayList<>();

        try {
            Statement statement = sqlConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
            while(resultSet.next())
            {
                // read the result set
                String tableName = resultSet.getString("name");
                tableNames.add(tableName);
            }

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tableNames;
    }

    private static void print(String message){
        System.out.print(message);
    }


    public static Map<String, String> getQueryMap(String query)
    {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }


    private  String createJSONStringWithQueryResultData(String queryString) {
        try{
            Statement statement = sqlConnection.createStatement();
            JDBC4ResultSet resultSet = (JDBC4ResultSet) statement.executeQuery(queryString);
            String[] columnNames = ((JDBC4ResultSet)resultSet).cols;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cols",columnNames);

            JSONArray dataRows = new JSONArray();

            while(resultSet.next())
            {
                List<String> dataArray = new ArrayList<>();
                for(String column : columnNames){
                    String value = resultSet.getString(column);
                    dataArray.add((value == null || value.isEmpty()) ? "EMPTY" : value );
                }
                dataRows.put(dataArray);
            }
            jsonObject.put("data",dataRows);
            return jsonObject.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }

    private  String createJSONStringWithTableData(String tableName) {
        try{
            Statement statement = sqlConnection.createStatement();
            JDBC4ResultSet resultSet = (JDBC4ResultSet) statement.executeQuery("SELECT * FROM "+tableName+";");
            String[] columnNames = ((JDBC4ResultSet)resultSet).cols;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cols",columnNames);

            JSONArray dataRows = new JSONArray();

            while(resultSet.next())
            {
                List<String> dataArray = new ArrayList<>();
                for(String column : columnNames){
                    String value = resultSet.getString(column);
                    dataArray.add((value == null || value.isEmpty()) ? "EMPTY" : value );
                }
                dataRows.put(dataArray);
            }
            jsonObject.put("data",dataRows);
            return jsonObject.toString();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return "";
    }


    public void shutDown(){
        try {
            if(sqlConnection != null){
                sqlConnection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(server != null){
            server.stop(1);
        }

    }



    private  String createMainPageStringHtml() {

        try {
            return readFile("main.html", Charset.defaultCharset());
        } catch (IOException e) {
            e.printStackTrace();
            return "Something went wrong!";
        }
    }

    private String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }


    static String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }


    static Map<String, String> getParameters(HttpExchange httpExchange) throws IOException {
        Map<String, String> parameters = new HashMap<>();
        InputStream inputStream = httpExchange.getRequestBody();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read = 0;
        while ((read = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, read);
        }
        String[] keyValuePairs = byteArrayOutputStream.toString().split("&");
        for (String keyValuePair : keyValuePairs) {
            String[] keyValue = keyValuePair.split("=");
            if (keyValue.length != 2) {
                continue;
            }
            parameters.put(keyValue[0], keyValue[1]);
        }
        return parameters;
    }








}
