package test;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Host {
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int currentHostScore = 0;

    public void start() throws IOException {
        currentHostScore = SystemEvaluator.calculateSystemScore();
        System.out.println("Iniciando como Host con puntaje: " + currentHostScore);

        ServerSocket serverSocket = new ServerSocket(5432);
        System.out.println("Host escuchando en el puerto 12345");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Nuevo cliente conectado: " + clientSocket.getInetAddress());
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clients.add(clientHandler);
            new Thread(clientHandler).start();
        }
    }

    public synchronized static void evaluateClient(int clientScore, Socket clientSocket) {
        if (clientScore > currentHostScore) {
            System.out.println("Un cliente tiene mejores prestaciones. Cambiando de host...");
            currentHostScore = clientScore;

            // Informar a todos los clientes que el host ha cambiado
            for (ClientHandler client : clients) {
                client.notifyHostChange(clientSocket.getInetAddress().getHostAddress());
            }

            System.out.println("Nuevo host: " + clientSocket.getInetAddress().getHostAddress());
        }
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

                // Recibir y procesar las prestaciones del cliente
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