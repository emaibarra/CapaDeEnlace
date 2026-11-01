package capaTransporte;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClienteTCP {
    public static void main(String[] args) {
        String host = "127.0.0.1"; // Localhost (máquina origen y destino son la misma para la prueba)
        int puerto = 5000;         // Debe coincidir con el puerto del servidor

        System.out.println("Intentando conectar al servidor " + host + " en el puerto " + puerto + "...");

        try (Socket socket = new Socket(host, puerto)) {
            // Si la ejecución llega aquí, la conexión orientada a la conexión se estableció exitosamente
            System.out.println("Conexión establecida.");

            // Preparamos el canal para leer los datos que envíe el servidor
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Leemos la transferencia de datos (el servicio)
            String respuestaServidor = in.readLine();

            System.out.println("\n--- MENSAJE RECIBIDO ---");
            System.out.println(respuestaServidor);
            System.out.println("------------------------\n");

            // La conexión se cierra automáticamente al terminar el bloque try-with-resources
        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor. Asegúrate de que esté en ejecución.");
            e.printStackTrace();
        }
    }
}