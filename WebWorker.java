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
import java.nio.file.Files;
import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;
private String type;

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
      
      //
      // Store user url
      //
      String address;
      address = readHTTPRequest(is);
      type = "text/html";
      
      writeHTTPHeader( os, type, address );
      writeContent( os, address );
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
   
} // end run

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line, local = " ";
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         
         String localSpot = line.substring(0,3);
         // Determine if the line is a GET
         if( localSpot.equals( "GET" ) ) {
            local = line.substring( 4 );
            local = local.substring( 0, local.indexOf(" ") );
            System.err.println( "Request file is: " + local );
         } // end if
         
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return local;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType, String address) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   
   File x = new File( address );
   //
   // Checking if file is real
   //
   if( x.exists() && !x.isDirectory() ) {
   
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: David's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      
   } // end if
   // if file not found
   else {
   
      os.write("HTTP/1.1 404 Not Found\n".getBytes());   
      os.write("Date: ".getBytes());
      os.write((df.format(d)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: David's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      
   } // end else
   
   return;
   
} // end writeHTTPHeader

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String address) throws Exception
{
   //
   // Pulling the url the user entered in the start
   //
   address = address.substring( 1 );
   File file = new File( address );
   
   //
   // Checking to see if the file is in the stated location
   //
   if( file.exists() && !file.isDirectory() ) {
      FileInputStream stream = new FileInputStream( address );
      BufferedReader read = new BufferedReader( new InputStreamReader( stream ) );
      
      // Looking for tags
      String filex;
      while(( filex = read.readLine() ) != null ) {
         // checking for tag <cs371date>
         if( filex.equals("<cs371date>") ) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("DD/MM/YY");
            Date specific = new Date();
            String finalDate = dateFormat.format(specific);
            os.write(finalDate.getBytes());
         } // end if
         
         // checking for tag <cs371>
         if( filex.equals("<cs371server>") ) {
            os.write("David's Server.".getBytes() );
         } // end if
         
         read.close();
         
      } // end while
      
   } // end if
   
   //
   // File does not exist
   //
   else {
      os.write( "<h3>Error: 404 not found</h3>".getBytes() );
   } // end else
   
} // end writeContent

} // end class