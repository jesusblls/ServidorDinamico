package test;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Host implements Runnable {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int currentHostScore = 0;
    private boolean running = true;

    @Override
    public void run() {
        try {
            currentHostScore = SystemEvaluator.calculateSystemScore();
            System.out.println("Iniciando como Host con puntaje: " + currentHostScore);

            ServerSocket serverSocket = new ServerSocket(5432);
            System.out.println("Host escuchando en el puerto 5432");

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start(); // Cada cliente tiene su propio hilo
            }

            serverSocket.close(); // Cerrar el servidor cuando se detiene
        } catch (IOException e) {
            System.out.println("Error en el host: " + e.getMessage());
        }
    }

    public synchronized static void evaluateClient(int clientScore, Socket clientSocket) {
        if (clientScore > currentHostScore) {
            System.out.println("Un cliente tiene mejores prestaciones. Cambiando de host...");
            currentHostScore = clientScore;

            // Esperar antes de notificar el cambio de host
            notifyClientsAboutNewHost(clientSocket.getInetAddress().getHostAddress());
        }
    }

    private static void notifyClientsAboutNewHost(String newHostAddress) {
        for (ClientHandler client : clients) {
            client.notifyHostChange(newHostAddress);
        }
        System.out.println("Todos los clientes han sido notificados del nuevo host: " + newHostAddress);
    }

    public void detener() {
        running = false;
        System.out.println("Servidor detenido.");
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                String clientInfo = in.readLine();
                System.out.println("Información del cliente: " + clientInfo);

                int clientScore = Integer.parseInt(clientInfo.split(":")[1]);
                Host.evaluateClient(clientScore, socket);

            } catch (IOException e) {
                System.out.println("Error manejando cliente: " + e.getMessage());
            }
        }

        public void notifyHostChange(String newHost) {
            if (out != null) {
                out.println("NEW_HOST:" + newHost);
            }
        }
    }
}

