package Aplicaciones;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

public class ServidorChat {
    private static final int PUERTO = 12345;
    // Mapa para gestionar clientes conectados: Username -> ManejadorCliente
    private static final Map<String, ManejadorCliente> clientesConectados = new ConcurrentHashMap<>();
    // Mapa de Canales: NombreCanal -> Lista de Clientes
    private static final Map<String, Set<ManejadorCliente>> canales = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        logInfo("Servidor de Chat iniciado en el puerto " + PUERTO);
        canales.put("general", ConcurrentHashMap.newKeySet());

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ManejadorCliente cliente = new ManejadorCliente(socket);
                new Thread(cliente).start();
            }
        } catch (IOException e) {
            logInfo("Error en el servidor: " + e.getMessage());
        }
    }

    // REGISTRO DE INFORMACIÓN (LOGGING)
    public static synchronized void logInfo(String mensaje) {
        String log = "[" + LocalDateTime.now() + "] [LOG]: " + mensaje;
        System.out.println(log);
        // Opcional: Escribir log en un archivo local (logging persistente)
        try (FileWriter fw = new FileWriter("servidor_chat.log", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(log);
        } catch (IOException ignored) {}
    }

    // MANEJADOR DE CADA CLIENTE (HILO SEPARADO)
    private static class ManejadorCliente implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String usuario;
        private String estadoPerfil = "¡Hola! Estoy usando el Chat en Java.";
        private String canalActual = "general";

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Solicitud de Autenticación / Perfil
                out.println("SERVIDOR: Bienvenido. Ingrese su nombre de usuario:");
                this.usuario = in.readLine();

                while (usuario == null || usuario.trim().isEmpty() || clientesConectados.containsKey(usuario)) {
                    out.println("SERVIDOR: Nombre inválido o ya ocupado. Intente otro:");
                    this.usuario = in.readLine();
                }

                clientesConectados.put(usuario, this);
                canales.get("general").add(this);
                logInfo("Usuario '" + usuario + "' se ha conectado desde " + socket.getInetAddress());

                out.println("SERVIDOR: Conectado con éxito al canal 'general'.");
                out.println("Comandos disponibles: /privado <usr> <msg>, /canal <nombre>, /perfil <estado>, /usuarios, /salir");

                String mensaje;
                while ((mensaje = in.readLine()) != null) {
                    if (mensaje.startsWith("/")) {
                        procesarComando(mensaje);
                    } else {
                        // Mensaje al canal actual
                        transmitirCanal("[" + canalActual + "] " + usuario + ": " + mensaje);
                    }
                }
            } catch (IOException e) {
                logInfo("Conexión perdida con usuario: " + usuario);
            } finally {
                desconectar();
            }
        }

        // PROCESAMIENTO DE COMANDOS
        private void procesarComando(String comandoCompleto) {
            String[] partes = comandoCompleto.split(" ", 3);
            String cmd = partes[0].toLowerCase();

            switch (cmd) {
                case "/privado": // SOPORTE PARA MENSAJES PRIVADOS
                    if (partes.length >= 3) {
                        String destino = partes[1];
                        String msgPrivado = partes[2];
                        ManejadorCliente clienteDestino = clientesConectados.get(destino);
                        if (clienteDestino != null) {
                            clienteDestino.out.println("[PRIVADO de " + usuario + "]: " + msgPrivado);
                            this.out.println("[PRIVADO a " + destino + "]: " + msgPrivado);
                            logInfo("Mensaje privado enviado de " + usuario + " a " + destino);
                        } else {
                            this.out.println("SERVIDOR: El usuario '" + destino + "' no está conectado.");
                        }
                    } else {
                        this.out.println("SERVIDOR: Uso correcto: /privado <usuario> <mensaje>");
                    }
                    break;

                case "/canal": // SOPORTE PARA MÚLTIPLES CANALES
                    if (partes.length >= 2) {
                        String nuevoCanal = partes[1];
                        canales.get(canalActual).remove(this);
                        canalActual = nuevoCanal;
                        canales.computeIfAbsent(nuevoCanal, k -> ConcurrentHashMap.newKeySet()).add(this);
                        this.out.println("SERVIDOR: Te has unido al canal '" + nuevoCanal + "'");
                        logInfo("Usuario " + usuario + " cambió al canal " + nuevoCanal);
                    }
                    break;

                case "/perfil": // GESTIÓN DE INFORMACIÓN DE PERFIL
                    if (partes.length >= 2) {
                        this.estadoPerfil = partes[1];
                        this.out.println("SERVIDOR: Perfil/Estado actualizado a: '" + estadoPerfil + "'");
                    }
                    break;

                case "/usuarios": // VER LISTA Y PERFIL DE USUARIOS
                    StringBuilder lista = new StringBuilder("--- USUARIOS CONECTADOS ---\n");
                    for (ManejadorCliente c : clientesConectados.values()) {
                        lista.append("- ").append(c.usuario).append(" (Estado: ").append(c.estadoPerfil).append(")\n");
                    }
                    this.out.println(lista.toString());
                    break;

                case "/salir":
                    desconectar();
                    break;

                default:
                    this.out.println("SERVIDOR: Comando no reconocido.");
            }
        }

        private void transmitirCanal(String mensaje) {
            Set<ManejadorCliente> miembros = canales.get(canalActual);
            if (miembros != null) {
                for (ManejadorCliente cliente : miembros) {
                    cliente.out.println(mensaje);
                }
            }
        }

        private void desconectar() {
            if (usuario != null) {
                clientesConectados.remove(usuario);
                if (canales.get(canalActual) != null) {
                    canales.get(canalActual).remove(this);
                }
                logInfo("Usuario '" + usuario + "' se ha desconectado.");
            }
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}