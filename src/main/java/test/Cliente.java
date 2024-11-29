package test;

import java.net.*;
import java.util.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Cliente implements Runnable {
    private static final int PORT = 12345;

    @Override
    public void run() {
        try {
            // Obtener la IP del host automáticamente en la red de Hamachi
            String hostIP = getHostIP();

            if (hostIP == null) {
                System.out.println("No se pudo encontrar un host en la red.");
                return;
            }

            // Conexión al host
            Socket socket = new Socket(hostIP, PORT);
            System.out.println("Conectado al Host en IP: " + hostIP);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

            // Enviar el puntaje del cliente al host
            out.writeObject(calcularPuntaje());

            out.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para obtener la IP del host automáticamente en la red de Hamachi
    private String getHostIP() {
        try {
            // Obtener todas las interfaces de red disponibles en el sistema
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Filtramos interfaces de Hamachi (generalmente comienza con "ham")
                if (networkInterface.getName().startsWith("ham")) {
                    // Buscar una dirección IP asociada con esta interfaz
                    for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                        InetAddress inetAddress = address.getAddress();
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress(); // Devolver la IP del host de Hamachi
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null; // Si no se encuentra una IP de Hamachi
    }

    // Calcular el puntaje basado en CPU y RAM
    public static int calcularPuntaje() {
        SystemInfo si = new SystemInfo();
        CentralProcessor processor = si.getHardware().getProcessor();
        GlobalMemory memory = si.getHardware().getMemory();

        // Obtenemos la carga de la CPU y la memoria
        double cpuLoad = processor.getSystemCpuLoad(0);
        double ram = memory.getTotal() / 1024.0 / 1024.0 / 1024.0; // GB

        // Calcular un puntaje simple basado en CPU y RAM
        return (int) ((1 - cpuLoad) * 100 + ram * 10);
    }
}
