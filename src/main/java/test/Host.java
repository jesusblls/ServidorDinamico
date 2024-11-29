package test;

import java.io.*;
import java.net.*;
import java.util.*;

public class Host implements Runnable {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int currentHostScore = 0;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public static void stopHost(Host hostInstance) {
        if (hostInstance != null) {
            hostInstance.stopServer();
        }
    }

    @Override
    public void run() {
        try {
            currentHostScore = SystemEvaluator.calculateSystemScore();
            System.out.println("Iniciando como Host con puntaje: " + currentHostScore);

            serverSocket = new ServerSocket(5432);
            System.out.println("Host escuchando en el puerto 5432");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                } catch (SocketException e) {
                    if (!running) {
                        System.out.println("El servidor ha sido detenido.");
                    } else {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error en el host: " + e.getMessage());
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error al detener el servidor: " + e.getMessage());
        }
    }

    public synchronized static void evaluateClient(int clientScore, Socket clientSocket) {
        if (clientScore > currentHostScore) {
            System.out.println("Un cliente tiene mejores prestaciones. Cambiando de host...");
            currentHostScore = clientScore;

            String newHostAddress = clientSocket.getInetAddress().getHostAddress();
            notifyClientsAboutNewHost(newHostAddress);
        }
    }

    private static void notifyClientsAboutNewHost(String newHostAddress) {
        for (ClientHandler client : clients) {
            client.notifyHostChange(newHostAddress);
        }
        System.out.println("Todos los clientes han sido notificados del nuevo host: " + newHostAddress);
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

