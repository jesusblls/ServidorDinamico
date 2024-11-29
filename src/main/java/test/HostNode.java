package test;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

public class HostNode extends Thread {
    private ServerSocket serverSocket;
    private List<ClientConnection> clientes = new CopyOnWriteArrayList<>();
    private int puerto = 5000;
    private SystemInfo systemInfo;
    private double scoreMaquina;

    public HostNode() {
        systemInfo = new SystemInfo();
        calcularScoreMaquina();
        
        new ServerDiscovery.ServidorDescubrimiento(2345).start();
    }

    private void calcularScoreMaquina() {
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        
        // Calcular score basado en CPU, RAM y otras métricas
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
        
        System.out.println("Score de máquina: " + scoreMaquina);
    }

    private double obtenerAnchosDeBanda() {
        // Obtener ancho de banda libre usando OSHI
        List<NetworkIF> redes = systemInfo.getHardware().getNetworkIFs();
        return redes.stream()
                    .mapToDouble(red -> red.getBytesRecv() + red.getBytesSent())
                    .average()
                    .orElse(0.0);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(puerto);
            System.out.println("Host iniciado en puerto " + puerto);

            while (!isInterrupted()) {
                Socket clienteSocket = serverSocket.accept();
                ClientConnection nuevoCliente = new ClientConnection(clienteSocket, this);
                clientes.add(nuevoCliente);
                nuevoCliente.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void verificarCambioHost(double scoreCliente, ClientConnection cliente) {
        if (scoreCliente > scoreMaquina) {
            // El cliente tiene mejores prestaciones, se convierte en nuevo host
            System.out.println("Nuevo host seleccionado: " + cliente.getSocketCliente().getInetAddress());
            migrarHost(cliente);
        }
    }

    private void migrarHost(ClientConnection nuevoHost) {
        // Lógica para migrar todos los clientes al nuevo host
        for (ClientConnection cliente : clientes) {
            if (cliente != nuevoHost) {
                enviarMensajeMigracion(cliente, nuevoHost.getSocketCliente().getInetAddress());
            }
        }
    }

    private void enviarMensajeMigracion(ClientConnection cliente, InetAddress nuevaIp) {
        try {
            DataOutputStream out = new DataOutputStream(cliente.getSocketCliente().getOutputStream());
            out.writeUTF("MIGRAR_HOST:" + nuevaIp.getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}