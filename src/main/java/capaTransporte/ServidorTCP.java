package capaTransporte;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class ServidorTCP {
    public static void main(String[] args) {
        int puerto = 5000; // Puerto donde el servidor escuchará
        int contadorConexiones = 0;

        // Utilizamos try-with-resources para asegurar el cierre del ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor TCP iniciado en el puerto " + puerto + ".");
            System.out.println("Esperando conexiones de clientes...");

            // Bucle infinito para mantener el servidor corriendo y escuchando
            while (true) {
                // El servidor se bloquea aquí hasta que un cliente solicita conexión
                Socket socketCliente = serverSocket.accept();
                contadorConexiones++;

                System.out.println("Nueva conexión entrante desde: " + socketCliente.getInetAddress().getHostAddress());

                // Preparamos el canal para enviarle datos al cliente
                PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true);

                // Construimos la información a enviar
                String fechaActual = new Date().toString();
                String mensaje = "¡Hola Cliente! Eres la conexión número: " + contadorConexiones +
                        ". La hora actual del servidor es: " + fechaActual;

                // Transmisión de datos
                out.println(mensaje);

                // Cierre de la conexión específica con este cliente
                socketCliente.close();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}