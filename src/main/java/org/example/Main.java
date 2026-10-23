package org.example;

import java.util.Random;

// --- 1. ESTRUCTURAS DE DATOS Y ENMARCADO ---
class Marco {
    String flagLimite = "01111110"; // Límite del marco, marcamos el inicio y fin del marco segun el protocolo (HDLC)
    String tipo;                    // "DATOS" o "ACK", dice si es un acknolage
    int secuencia; //0 o 1
    String datos; //payload
    int checksum;

    public Marco(String tipo, int secuencia, String datos) {
        this.tipo = tipo;
        this.secuencia = secuencia;
        this.datos = datos;
        this.checksum = tipo.equals("DATOS") ? calcularChecksum(datos) : 0;
    }

    public int calcularChecksum(String texto) {  //convierte el texto en un arreglo de caracteres, hace la suma y devuelve el modulo con 256
        int suma = 0;
        for (char c : texto.toCharArray()) {
            suma += (int) c;
        }
        return suma % 256;
    }

    public Marco copiar() { //copia la trama en el buffer sin afectar el original
        Marco copia = new Marco(this.tipo, this.secuencia, this.datos);
        copia.checksum = this.checksum;
        return copia;
    }
}

// --- 2. LÓGICA DEL RECEPTOR ---
class Receptor {
    int secuenciaEsperada = 0; //espera q el primero empiece en 0
    StringBuilder mensajeReensamblado = new StringBuilder(); // aca pegamos el texto

    public Marco recibirMarco(Marco marco) { //revisa errores, vuelve a calcular el checksum y lo compara con el que se hizo al principio
        if (marco.calcularChecksum(marco.datos) != marco.checksum) {
            System.out.println("   [Receptor] Error detectado (Checksum inválido). Marco descartado en silencio."); //si son distintas devuelve null
            return null;
        }

        if (marco.secuencia != secuenciaEsperada) { //si es correcto el checksum pero no es la secuencia esperada, signfica q envio con retraso, se envia el acknolage anterior
            System.out.println("   [Receptor] Marco duplicado detectado (Sec: " + marco.secuencia + "). Reenviando ACK anterior.");
            return new Marco("ACK", marco.secuencia, "");
        }

        System.out.println("   [Receptor] Marco " + marco.secuencia + " verificado OK. Datos entregados a Capa de Red: '" + marco.datos + "'");

        // si el marco paso todos los controles de seguridad, pegamos el texto a nuestra frase final
        mensajeReensamblado.append(marco.datos);

        Marco ack = new Marco("ACK", secuenciaEsperada, "");
        secuenciaEsperada = 1 - secuenciaEsperada; //cambia la secuencia esperada
        return ack;
    }

    //funcion para mostrar el resultado final
    public void mostrarMensajeFinal() {
        System.out.println("\n=======================================================");
        System.out.println(" [RECEPTOR] MENSAJE FINAL: '" + mensajeReensamblado.toString() + "'");
        System.out.println("=======================================================\n");
    }
}

// SIMULADOR DEL MEDIO DE TRANSMISIÓN---
class MedioFisico {
    Receptor receptor;
    double probPerdida;
    double probError;
    Marco ackBuffer;
    Random random;

    public MedioFisico(Receptor receptor, double probPerdida, double probError) {
        this.receptor = receptor;
        this.probPerdida = probPerdida;
        this.probError = probError;
        this.ackBuffer = null;
        this.random = new Random();
    }

    public void transmitirAReceptor(Marco marco) {
        simularLatencia();

        if (random.nextDouble() < probPerdida) {
            System.out.println("   [Medio Físico] -> ❌ MARCO PERDIDO en la red.");
            return;
        }

        Marco marcoEnviado = marco.copiar();

        if (random.nextDouble() < probError) {
            System.out.println("   [Medio Físico] -> ⚠️ MARCO DAÑADO (interferencia).");
            marcoEnviado.datos += "_daño_aleatorio";
        }

        this.ackBuffer = this.receptor.recibirMarco(marcoEnviado);
    }

    public Marco transmitirAcuseAEmisor() {
        if (this.ackBuffer == null) return null;

        simularLatencia();

        if (random.nextDouble() < probPerdida) {
            System.out.println("   [Medio Físico] -> ❌ ACUSE DE RECIBO (ACK) PERDIDO.");
            this.ackBuffer = null;
            return null;
        }

        Marco ack = this.ackBuffer;
        this.ackBuffer = null;
        return ack;
    }

    private void simularLatencia() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

//LOGICA DEL TRANSMISOR
class Transmisor {
    MedioFisico medio;
    int secuenciaActual = 0;

    public Transmisor(MedioFisico medio) {
        this.medio = medio;
    }

    public void transferenciaDesdeCapaRed(String mensaje) {
        System.out.println("\n[Capa de Red Origen] Solicitando transferencia del mensaje: '" + mensaje + "'\n");

        int tamañoMax = 4;

        for (int i = 0; i < mensaje.length(); i += tamañoMax) { //vamos a dividir el mensaje en 4 caracteres
            int fin = Math.min(i + tamañoMax, mensaje.length());
            String pedazoDatos = mensaje.substring(i, fin);

            Marco marco = new Marco("DATOS", secuenciaActual, pedazoDatos);
            boolean confirmado = false;

            while (!confirmado) {
                System.out.println("[Transmisor] Enviando Marco " + secuenciaActual + " con datos: '" + pedazoDatos + "'");
                medio.transmitirAReceptor(marco);

                Marco ack = medio.transmitirAcuseAEmisor();

                if (ack == null) {
                    System.out.println("[Transmisor] TIMEOUT: Temporizador expiró sin recibir ACK. Retransmitiendo...\n");
                } else if (ack.secuencia == secuenciaActual) {
                    System.out.println("[Transmisor] Acuse de recibo (ACK " + ack.secuencia + ") procesado con éxito. Avanzando...\n");
                    secuenciaActual = 1 - secuenciaActual;
                    confirmado = true;
                } else {
                    System.out.println("[Transmisor] ACK inesperado (" + ack.secuencia + "). Retransmitiendo...\n");
                }
            }
        }
    }
}

// --- 5. EJECUCIÓN PRINCIPAL ---
public class Main {
    public static void main(String[] args) {
        Receptor receptorDestino = new Receptor();

        // 30% probabilidad de pérdida, 20% probabilidad de daño
        MedioFisico canalFisico = new MedioFisico(receptorDestino, 0.3, 0.2);
        Transmisor transmisorOrigen = new Transmisor(canalFisico);

        String datosParaEnviar = "HOLA UNIVERSIDAD";
        transmisorOrigen.transferenciaDesdeCapaRed(datosParaEnviar);

        System.out.println("--- TRANSFERENCIA DE LA CAPA DE RED COMPLETADA SATISFACTORIAMENTE ---");

        //Le decimos al receptor que imprima el texto
        receptorDestino.mostrarMensajeFinal();
    }
}