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

public class WebWorker implements Runnable
{

private Socket socket;
String fileName = "";

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
	  
	  
	  
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      fileName = readHTTPRequest(is, fileName);// this.... doesn't seem right.
	 
      writeHTTPHeader(os,"text/html");
      writeContent(os, fileName);
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
private String readHTTPRequest(InputStream is, String fileName)
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
	
	System.out.println("Checking fileName... " + fileName);
	//if filename ends improperly send to homepage
	if(fileName.endsWith("/")) fileName += "home.html";
   
	//remove leading /
	while(fileName.indexOf("/") == 0) fileName = fileName.substring(1);
	
	fileName = fileName.replace('/', File.separator.charAt(0));
	
	
	//should check for illegal character but just going to throw a 404 instead of 
	//sanitizing the inputs and looking for a correct file
	
	}
	catch(Exception e){
		e.printStackTrace(); //lazy exception handling.
	}
   /*while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);

	 //reads the request
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
	
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }*/
   return fileName;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
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
private void writeContent(OutputStream os, String fileName) throws Exception
{	
	System.out.println("in writeContent");
	String line = null;
	try{
		//open the file with FileReader
		FileReader fileReader = new FileReader(fileName);
		
		//wrap in BufferedReader
		BufferedReader bf = new BufferedReader(fileReader); //would normally give a more meaningful name but this is a smol project.
		
		while(( line = bf.readLine()) != null){
			os.write(line.getBytes());
		}
	/*
    os.write("<html><head></head><body>\n".getBytes());
    os.write("<h3>My web server works!</h3>\n".getBytes());
    os.write("</body></html>\n".getBytes());*/
	
	}
	catch(FileNotFoundException e){
		System.err.println("Error in writeContent, file not found: " + fileName );
	}
	
	catch(IOException e){
		e.printStackTrace();
	}
}


} // end class
