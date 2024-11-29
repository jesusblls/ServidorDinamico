package test;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host implements Runnable {
    private static final int PORT = 12345;

    @Override
    public void run() {
        try {
            // Inicializa el servidor
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Host iniciado. Esperando clientes...");

            // Executor para manejar conexiones de clientes
            ExecutorService executor = Executors.newFixedThreadPool(10);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClienteHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
