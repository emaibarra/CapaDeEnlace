package capaTransporte;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

//El servidor permite comunicarse con varios clientes mediante hilos
public class ServidorTCP {
    public static void main(String[] args) {
        int puerto = 5000;
        // Usamos AtomicInteger para evitar problemas de concurrencia al contar
        AtomicInteger contadorConexiones = new AtomicInteger(0);

        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("Servidor TCP Multihilo iniciado en el puerto " + puerto + ".");
            System.out.println("Esperando múltiples conexiones concurrentes...");

            while (true) {
                // El hilo principal solo se bloquea aquí esperando nuevos clientes
                Socket socketCliente = serverSocket.accept();

                // Incremento seguro a nivel de hilos
                int numeroConexion = contadorConexiones.incrementAndGet();

                System.out.println("Nueva conexión desde: " + socketCliente.getInetAddress().getHostAddress() +
                        " | Asignando al Hilo-" + numeroConexion);

                // Instanciamos la clase que maneja la lógica y la ejecutamos en un nuevo Hilo
                ManejadorCliente manejador = new ManejadorCliente(socketCliente, numeroConexion);
                new Thread(manejador).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Clase que implementa Runnable para ejecutarse en su propio hilo
class ManejadorCliente implements Runnable {
    private Socket socketCliente;
    private int numeroConexion;

    public ManejadorCliente(Socket socket, int numero) {
        this.socketCliente = socket;
        this.numeroConexion = numero;
    }

    @Override
    public void run() {
        // Todo lo que ocurra en este método run() se ejecuta en paralelo al hilo principal
        try (PrintWriter out = new PrintWriter(socketCliente.getOutputStream(), true)) {

            // Simulamos un pequeño retardo para demostrar la concurrencia (opcional)
            Thread.sleep(500);

            String fechaActual = new Date().toString();
            String mensaje = "Hola Cliente! Eres la conexion numero: " + numeroConexion +
                    ". Hora: " + fechaActual +
                    " [Atendido por: " + Thread.currentThread().getName() + "]";

            out.println(mensaje);

        } catch (IOException | InterruptedException e) {
            System.err.println("Error al comunicar con el cliente " + numeroConexion);
            e.printStackTrace();
        } finally {
            try {
                // Aseguramos el cierre del socket independientemente de si hubo errores
                socketCliente.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}