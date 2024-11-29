package test;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Host implements Runnable {
    private static final int PORT = 5432;
    private static int currentHostScore = SystemEvaluator.calculateSystemScore();
    private static ServerSocket serverSocket;
    private static final ConcurrentHashMap<Socket, PrintWriter> clients = new ConcurrentHashMap<>();

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Servidor iniciado. Esperando conexiones...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress().getHostAddress());
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            clients.put(clientSocket, out);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("CLIENT_SCORE:")) {
                    int clientScore = Integer.parseInt(message.split(":")[1]);
                    evaluateClient(clientScore, clientSocket);
                }
            }
        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + clientSocket.getInetAddress().getHostAddress());
        } finally {
            clients.remove(clientSocket);
            try {
                clientSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private synchronized void evaluateClient(int clientScore, Socket clientSocket) {
        if (clientScore > currentHostScore) {
            System.out.println("Un cliente tiene mejores prestaciones. Cambiando de host...");
            currentHostScore = clientScore;

            String newHostAddress = clientSocket.getInetAddress().getHostAddress();
            notifyClientsAboutNewHost(newHostAddress);

            try {
                serverSocket.close(); // Detener el servidor actual
                System.out.println("Servidor detenido. El nuevo host es: " + newHostAddress);
            } catch (IOException e) {
                System.out.println("Error al detener el servidor: " + e.getMessage());
            }
        }
    }

    private void notifyClientsAboutNewHost(String newHostAddress) {
        for (PrintWriter out : clients.values()) {
            out.println("NEW_HOST:" + newHostAddress);
        }
    }
}

