package test;

import java.net.*;
import java.io.*;

public class ServerDiscovery {
    private static String IPServidor = null;
    private static final int PUERTO_DESCUBRIMIENTO = 2345;

    public static String obtenerIPDelServidor() throws Exception {
        InetAddress direccionBroadcast = InetAddress.getByName("255.255.255.255");
        
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            
            String mensajeDescubrimiento = "Servidor_Descubierto";
            byte[] bytesMensaje = mensajeDescubrimiento.getBytes();

            // Enviar paquete de broadcast
            DatagramPacket paquete = new DatagramPacket(
                bytesMensaje, 
                bytesMensaje.length, 
                direccionBroadcast, 
                PUERTO_DESCUBRIMIENTO
            );
            socket.send(paquete);
            System.out.println("Mensaje de Broadcast enviado para encontrar el servidor");

            // Preparar buffer para recibir respuesta
            byte[] bufferRecibimiento = new byte[256];
            DatagramPacket paqueteRespuesta = new DatagramPacket(
                bufferRecibimiento, 
                bufferRecibimiento.length
            );

            // Configurar timeout para evitar espera infinita
            socket.setSoTimeout(5000); // 5 segundos de espera

            try {
                // Esperar respuesta del servidor
                socket.receive(paqueteRespuesta);

                // Convertir respuesta a IP del servidor
                IPServidor = new String(
                    paqueteRespuesta.getData(), 
                    0, 
                    paqueteRespuesta.getLength()
                ).trim();

                System.out.println("Servidor encontrado en IP: " + IPServidor);
                return IPServidor;

            } catch (SocketTimeoutException e) {
                System.out.println("No se encontró servidor en la red.");
                return null;
            }
        }
    }

    // Método para el lado del servidor que responde al descubrimiento
    public static class ServidorDescubrimiento extends Thread {
        private DatagramSocket socket;
        private int puerto;

        public ServidorDescubrimiento(int puerto) {
            this.puerto = puerto;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(puerto);
                socket.setBroadcast(true);

                while (!isInterrupted()) {
                    // Preparar buffer para recibir mensaje de descubrimiento
                    byte[] bufferRecepcion = new byte[256];
                    DatagramPacket paqueteRecibido = new DatagramPacket(
                        bufferRecepcion, 
                        bufferRecepcion.length
                    );

                    // Recibir paquete de descubrimiento
                    socket.receive(paqueteRecibido);

                    // Verificar si es un mensaje de descubrimiento
                    String mensajeRecibido = new String(
                        paqueteRecibido.getData(), 
                        0, 
                        paqueteRecibido.getLength()
                    ).trim();

                    if ("Servidor_Descubierto".equals(mensajeRecibido)) {
                        // Obtener la dirección IP local
                        String miDireccionIP = InetAddress.getLocalHost().getHostAddress();

                        // Responder al cliente con mi dirección IP
                        byte[] respuesta = miDireccionIP.getBytes();
                        DatagramPacket paqueteRespuesta = new DatagramPacket(
                            respuesta, 
                            respuesta.length, 
                            paqueteRecibido.getAddress(), 
                            paqueteRecibido.getPort()
                        );

                        socket.send(paqueteRespuesta);
                        System.out.println("Respondiendo a solicitud de descubrimiento desde " + 
                            paqueteRecibido.getAddress());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }
}