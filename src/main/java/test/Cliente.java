package test;

import java.net.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Cliente implements Runnable {
    private static final int PORT = 12345;
    private static final int PUERTO_DESCUBRIMIENTO = 2345;
    private static String IPServidor = null;

    @Override
    public void run() {
        try {
            // Intentar obtener la IP del servidor utilizando Broadcast UDP
            obtenerIPDelServidor();
            
            if (IPServidor == null) {
                System.out.println("No se pudo encontrar un servidor.");
                return;
            }

            // Conectarse al servidor con la IP obtenida
            Socket socket = new Socket(IPServidor, PORT);
            System.out.println("Conectado al servidor en IP: " + IPServidor);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // Enviar el puntaje del cliente al servidor
            out.writeObject(calcularPuntaje());

            out.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para realizar un Broadcast UDP para obtener la IP del servidor
    private static void obtenerIPDelServidor() throws Exception {
        InetAddress direccionBroadC = InetAddress.getByName("255.255.255.255");

        // Crear el DatagramSocket para el mensaje UDP
        DatagramSocket socket = new DatagramSocket();

        String mensajeDescubrimiento = "Servidor_Descubierto";

        byte[] bytesMensaje = mensajeDescubrimiento.getBytes();

        // Enviar el mensaje de Broadcast
        DatagramPacket paquete = new DatagramPacket(bytesMensaje, bytesMensaje.length, direccionBroadC, PUERTO_DESCUBRIMIENTO);
        System.out.println("Enviando mensaje de Broadcast para encontrar el servidor...");
        socket.send(paquete);

        // Preparación para recibir la respuesta del servidor
        byte[] bufferRecibimiento = new byte[256];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRecibimiento, bufferRecibimiento.length);

        // Esperar a que el servidor responda
        socket.receive(paqueteRespuesta);

        // Convertir los datos recibidos en una dirección IP (String)
        IPServidor = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        System.out.println("Servidor encontrado en IP: " + IPServidor);

        socket.close();
    }

    // Calcular el puntaje basado en el CPU y la memoria
    public static int calcularPuntaje() {
        SystemInfo si = new SystemInfo();
        CentralProcessor processor = si.getHardware().getProcessor();
        GlobalMemory memory = si.getHardware().getMemory();

        // Obtener la carga de la CPU y la memoria
        double cpuLoad = processor.getSystemCpuLoad(0);
        double ram = memory.getTotal() / 1024.0 / 1024.0 / 1024.0; // en GB

        // Calcular un puntaje simple basado en la CPU y RAM
        return (int) ((1 - cpuLoad) * 100 + ram * 10);
    }
}

