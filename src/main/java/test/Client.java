package test;

import java.io.*;
import java.net.Socket;

public class Client {
    private String hostAddress;

    public Client(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public void start() {
        while (true) {
            try (Socket socket = new Socket(hostAddress, 5432);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Conectado al host: " + hostAddress);

                // Calcular y enviar puntaje al host
                int clientScore = SystemEvaluator.calculateSystemScore();
                out.println("CLIENT_SCORE:" + clientScore);

                // Escuchar mensajes del host
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("NEW_HOST:")) {
                        hostAddress = message.split(":")[1];
                        System.out.println("El host ha cambiado a: " + hostAddress);
                        break; // Desconectar y conectar al nuevo host
                    }
                }
            } catch (IOException e) {
                System.out.println("No se pudo conectar al host. Reintentando...");
                try {
                    Thread.sleep(2000); // Esperar antes de intentar de nuevo
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}