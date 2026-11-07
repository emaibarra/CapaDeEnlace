package Aplicaciones;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteChat {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in);
             // Opcion para GUARDAR CONVERSACIONES localmente
             PrintWriter guardarChat = new PrintWriter(new FileWriter("historial_chat.txt", true))) {

            System.out.println("--- CONECTADO AL SERVIDOR DE CHAT ---");

            // Hilo secundario para escuchar continuamente mensajes entrantes
            Thread lectorMensajes = new Thread(() -> {
                try {
                    String respuestaServidor;
                    while ((respuestaServidor = in.readLine()) != null) {
                        System.out.println(respuestaServidor);

                        // Guardar en archivo el historial de conversación
                        guardarChat.println(respuestaServidor);
                        guardarChat.flush();
                    }
                } catch (IOException e) {
                    System.out.println("Conexión con el servidor cerrada.");
                }
            });
            lectorMensajes.start();

            // Hilo principal para enviar datos digitados por la consola
            while (scanner.hasNextLine()) {
                String mensaje = scanner.nextLine();
                out.println(mensaje);

                if (mensaje.equalsIgnoreCase("/salir")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("No se pudo conectar al servidor: " + e.getMessage());
        }
    }
}
