/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.net.FileNameMap;
import java.net.URLConnection;


public class WebWorker implements Runnable
{

private Socket socket;
private String fileName = "";

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
	  //initial response
	  String response = "HTTP/1.1 200 OK\n";
	  
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);
	  File pageFile = openFile(fileName);
	  //handle files that are not on the server
	  if(!pageFile.exists()){
		  response = "HTTP/1.1 404 NOT FOUND\n";
		  fileName = "heretic.html";
		  pageFile = openFile(fileName);
	  }
	  
      writeHTTPHeader(os,getMimeType(fileName), response);
      writeContent(os, pageFile);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
	String line;
	BufferedReader r = new BufferedReader(new InputStreamReader(is));
   
	//read the first line of the GET request to parse filename, subsequent lines are handled
	//as written by previous author and left untouched
    try{
		while(! r.ready()) Thread.sleep(1); // wait until the buffered reader is ready.
		line = r.readLine();
	
	System.out.println("First line of get is: " + line);
	//attempt to serve the request
	//break line into tokens to parse filename
	StringTokenizer token = new StringTokenizer(line);
	
	//attempt to split string on tokens and set into fileName	
	if(token.hasMoreElements() && token.nextToken().equalsIgnoreCase("GET") && token.hasMoreElements()) fileName= token.nextToken();
	
	
	//if filename ends improperly send to homepage
	if(fileName.endsWith("/")) fileName += "home.html";
   
	//remove leading /
	while(fileName.indexOf("/") == 0) fileName = fileName.substring(1);
	if(fileName.length() <= 1) fileName = "home.html";
	
	fileName = fileName.replace('/', File.separator.charAt(0));
	
	
	System.out.println("Checking fileName... " + fileName);
	//should check for illegal character but just going to throw a 404 instead of 
	//sanitizing the inputs and looking for a correct file
	
	}
	catch(Exception e){
		e.printStackTrace(); //lazy exception handling.
	}
	
   
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String response) throws Exception
{	
	os.write(response.getBytes());
   	
	os.write("Date: ".getBytes());
   	os.write(getDate().getBytes());
   	os.write("\n".getBytes());
   	os.write("Server: Lupercal! \n".getBytes());
   	//os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   	//os.write("Content-Length: 438\n".getBytes()); 
   	os.write("Connection: close\n".getBytes());
   	os.write("Content-Type: ".getBytes());
   	os.write(contentType.getBytes());
   	os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines

   	return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/

//this is a terrible way of handling the write content method. There are much more elegant solutions

private void writeContent(OutputStream os, File pageFile) throws Exception
{	
	String line = null;
	try{
		//open the file with FileReader
		
	
		//wrap in BufferedReader
		BufferedReader bf = new BufferedReader(new FileReader(pageFile)); //would normally give a more meaningful name but this is a smol project.
		
		/*while(( line = bf.readLine()) != null){
			StringTokenizer token = new StringTokenizer(line);
			
			while(token.hasMoreElements()){
				String temp = token.nextToken(); //temp token to not continue iterating through while checking values
				if(temp.equals("<cs371date>")){
					os.write(("<p> " + getDate() + "</p>\n").getBytes());
					break; //break and do not continue looping
				}
				else if(temp.equals("<cs371server>")){
					os.write(("<p> Lupercal! Lupercal, For the WarMaster! </p>\n").getBytes());
					break; //break and do not continue looping
				}
			}
			
		
			os.write(line.getBytes());
		}*/
		
		FileInputStream fs = new FileInputStream(pageFile);
		final byte [] buffer = new byte[0x10000];
		int count = 0;
		while((count = fs.read(buffer)) >= 0){
			os.write(buffer, 0, count);
		}
		bf.close();
	}
	catch(FileNotFoundException e){
		os.write("<p> HERETICAL PAGE NOT FOUND. PURGING... PRUGING... PURGED. </p>".getBytes());
		os.write("<br><br> <p> 404 NOT FOUND </p>".getBytes()); 	

	}
	
	catch(IOException e){
		e.printStackTrace();
	}
	

	return;
}

//returns a formatted date string
private String getDate(){
	Date d = new Date();
	DateFormat df = DateFormat.getDateTimeInstance();
	df.setTimeZone(TimeZone.getTimeZone("GMT"));

    return df.format(d);
}

//handles automatic MIME type identification
private static String getMimeType(String filePath) throws java.io.IOException {
	
	FileNameMap fileNameMap = URLConnection.getFileNameMap();
	String type = fileNameMap.getContentTypeFor(filePath);
	
	return type;
	
}

private static File openFile(String fn) throws java.io. IOException { return new File(fn); }

} // end class
