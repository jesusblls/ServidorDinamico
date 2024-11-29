package test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        
        // Recalcular el puntaje del host periódicamente
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::calcularScoreMaquina, 0, 1, TimeUnit.MINUTES);
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
        } finally {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void verificarCambioHost(double scoreCliente, ClientConnection cliente) {
        // Evaluar el puntaje del host y de todos los clientes
        double mejorScore = scoreMaquina;
        ClientConnection nuevoHost = null;

        System.out.println("Verificando cambio de host...");
        System.out.println("Score del host actual: " + scoreMaquina);

        for (ClientConnection c : clientes) {
            double scoreClienteActual = c.getScoreCliente();
            System.out.println("Score del cliente " + c.getSocketCliente().getInetAddress() + ": " + scoreClienteActual);
            System.out.println("Mejor score actual: " + mejorScore);
            // verificar si el cliente tiene un puntaje mayor y retornar true en consola
            System.out.println(scoreClienteActual > mejorScore);
            if (scoreClienteActual > mejorScore) {
                System.out.println("Nuevo mejor score: " + scoreClienteActual);
                mejorScore = scoreClienteActual;
                nuevoHost = c;
            }
        }

        System.out.println("Nuevo host seleccionado: " + nuevoHost);

        if (nuevoHost != null) {
            System.out.println("Nuevo host seleccionado: " + nuevoHost.getSocketCliente().getInetAddress());
            migrarHost(nuevoHost);
        } else {
            System.out.println("No se requiere cambio de host.");
        }
    }

    private void migrarHost(ClientConnection nuevoHost) {
        // Lógica para migrar todos los clientes al nuevo host
        System.out.println("Migrando host a: " + nuevoHost.getSocketCliente().getInetAddress());
        for (ClientConnection cliente : clientes) {
            enviarMensajeMigracion(cliente, nuevoHost.getSocketCliente().getInetAddress());
        }
        // Cerrar el servidor actual y reconectar al nuevo host
        try {
            serverSocket.close();
            // Interrumpir el hilo del anterior host
            this.interrupt();
            // Reconectar al nuevo host
            reconectarNuevoHost(nuevoHost.getSocketCliente().getInetAddress().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enviarMensajeMigracion(ClientConnection cliente, InetAddress nuevaIp) {
        try {
            DataOutputStream out = new DataOutputStream(cliente.getSocketCliente().getOutputStream());
            out.writeUTF("MIGRAR_HOST:" + nuevaIp.getHostAddress());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reconectarNuevoHost(String nuevaIp) {
        try {
            Socket nuevoSocket = new Socket(nuevaIp, 5000);
            DataOutputStream out = new DataOutputStream(nuevoSocket.getOutputStream());
            out.writeDouble(scoreMaquina);
            out.flush();
            System.out.println("Reconectado al nuevo host en: " + nuevaIp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}