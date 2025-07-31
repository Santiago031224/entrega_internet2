import java.io.*;
import java.net.*;
import java.util.*;

public final class ServidorWeb {
    public static void main(String argv[]) throws Exception {
        int puerto = 6789;

        // Estableciendo el socket de escucha.
        ServerSocket socketdeEscucha = new ServerSocket(puerto);
        System.out.println("Servidor Web escuchando en el puerto " + puerto);

        // Procesando las solicitudes HTTP en un ciclo infinito.
        while (true) {
            Socket socketdeConexion = socketdeEscucha.accept();

            // Construye un objeto para procesar el mensaje de solicitud HTTP.
            SolicitudHttp solicitud = new SolicitudHttp(socketdeConexion);

            // Crea un nuevo hilo para procesar la solicitud.
            Thread hilo = new Thread(solicitud);

            // Inicia el hilo.
            hilo.start();
        }
    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor para recibir el socket.
    public SolicitudHttp(Socket socket) {
        this.socket = socket;
    }

    // Implementa el método run() de la interface Runnable.
    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void proceseSolicitud() throws Exception {
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Recoge la línea de solicitud HTTP.
        String lineaDeSolicitud = br.readLine();
        System.out.println();
        System.out.println(lineaDeSolicitud);

        // Recoge y muestra las líneas del header.
        String lineaDelHeader;
        while ((lineaDelHeader = br.readLine()).length() != 0) {
            System.out.println(lineaDelHeader);
        }

        // Extrae el nombre del archivo solicitado.
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        partesLinea.nextToken(); // Ignora el método (por ejemplo, GET)
        String nombreArchivo = partesLinea.nextToken();

        nombreArchivo = "." + nombreArchivo; // Prefijo para el directorio actual

        FileInputStream fis = null;
        boolean existeArchivo = true;

        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }

        String lineaDeEstado;
        String lineaDeTipoContenido;
        String cuerpoMensaje = null;

        if (existeArchivo) {
            lineaDeEstado = "200 OK" + CRLF;
            lineaDeTipoContenido = "Content-Type: " + contentType(nombreArchivo) + CRLF;
        } else {
            lineaDeEstado = "404 Not Found" + CRLF;
            lineaDeTipoContenido = "Content-Type: text/html" + CRLF;
            cuerpoMensaje = "<HTML>" +
                            "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                            "<BODY><b>404</b> Not Found</BODY></HTML>";
        }

        // Enviar la línea de estado
        os.writeBytes(lineaDeEstado);

        // Enviar el tipo de contenido
        os.writeBytes(lineaDeTipoContenido);

        // Línea en blanco para indicar fin del header
        os.writeBytes(CRLF);

        // Enviar cuerpo del mensaje
        if (existeArchivo) {
            enviarBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(cuerpoMensaje);
        }

        os.close();
        br.close();
        socket.close();
    }

    private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception {
        byte[] buffer = new byte[1024];
        int bytes = 0;

        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
            return "text/html";
        }
        if (nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";
        }
        if (nombreArchivo.endsWith(".png")) {
            return "image/png";
        }
        if (nombreArchivo.endsWith(".css")) {
            return "text/css";
        }
        if (nombreArchivo.endsWith(".js")) {
            return "application/javascript";
        }
        return "application/octet-stream";
    }
}
