package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

public class ClientNode extends Thread {
    private Socket socketServidor;
    private double scoreMaquina;
    private SystemInfo systemInfo;
    private String ipLocal;

    public ClientNode() {
        systemInfo = new SystemInfo();
        calcularScoreMaquina();
        ipLocal = obtenerIPLocal();
    }

    private void calcularScoreMaquina() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        
        // Obtener ticks de CPU antes y después
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        try {
            // Esperar un momento para obtener una lectura precisa
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long[] currentTicks = processor.getSystemCpuLoadTicks();
        
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(currentTicks);
        
        double ramLibre = memory.getAvailable() / (1024.0 * 1024.0 * 1024.0);
        double ramTotal = memory.getTotal() / (1024.0 * 1024.0 * 1024.0);
        
        scoreMaquina = (cpuLoad * 0.4) + 
                       ((ramLibre / ramTotal) * 0.4) + 
                       (obtenerAnchosDeBanda() * 0.2);
        
        System.out.println("Score de máquina cliente: " + scoreMaquina);
    }

    private double obtenerAnchosDeBanda() {
        List<NetworkIF> redes = systemInfo.getHardware().getNetworkIFs();
        return redes.stream()
                    .mapToDouble(red -> red.getBytesRecv() + red.getBytesSent())
                    .average()
                    .orElse(0.0);
    }

    private String obtenerIPLocal() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }

    @Override
    public void run() {
        try {
            // Descubrir la IP del servidor automáticamente
            String servidorIP = ServerDiscovery.obtenerIPDelServidor();
            
            if (servidorIP == null) {
                System.out.println("No se pudo encontrar un servidor en la red.");
                return;
            }

            // Conectar al servidor usando la IP descubierta
            socketServidor = new Socket(servidorIP, 5000);
            
            // Enviar score de máquina
            DataOutputStream out = new DataOutputStream(socketServidor.getOutputStream());
            out.writeDouble(scoreMaquina);

            // Escuchar mensajes del servidor
            new Thread(() -> {
                try {
                    DataInputStream in = new DataInputStream(socketServidor.getInputStream());
                    while (true) {
                        String mensaje = in.readUTF();
                        if (mensaje.startsWith("MIGRAR_HOST:")) {
                            String nuevaIp = mensaje.split(":")[1];
                            reconectarNuevoHost(nuevaIp);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reconectarNuevoHost(String nuevaIp) {
        try {
            if (nuevaIp.equals(ipLocal)) {
                System.out.println("Este cliente se ha convertido en el nuevo host.");
                // Iniciar el nuevo host
                new HostNode().start();
                return;
            }
            socketServidor.close();
            // Esperar un momento para asegurarse de que el nuevo host esté listo
            Thread.sleep(2000);
            socketServidor = new Socket(nuevaIp, 5000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}