package test;

import java.io.*;
import java.net.*;

public class Client implements Runnable {
    private String hostAddress;

    public Client(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    @Override
    public void run() {
        boolean connected = false;

        while (!connected) {
            try (Socket socket = new Socket(hostAddress, 5432);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Conectado al host: " + hostAddress);

                // Enviar el puntaje del cliente
                int clientScore = SystemEvaluator.calculateSystemScore();
                out.println("CLIENT_SCORE:" + clientScore);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("NEW_HOST:")) {
                        String newHostAddress = message.split(":")[1];
                        System.out.println("El host ha cambiado a: " + newHostAddress);

                        if (newHostAddress.equals(getLocalIPAddress())) {
                            System.out.println("Convirtiéndome en el nuevo host...");
                            Host newHost = new Host();
                            new Thread(newHost).start();
                            return;
                        } else {
                            hostAddress = newHostAddress;
                            break;
                        }
                    }
                }
                connected = true;
            } catch (IOException e) {
                System.out.println("No se pudo conectar al host. Reintentando...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private String getLocalIPAddress() {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println(localhost.getHostAddress());
            return localhost.getHostAddress().trim();
        } catch (UnknownHostException e) {
            System.out.println("Error obteniendo la dirección IP local: " + e.getMessage());
            return "127.0.0.1"; // Dirección IP de fallback en caso de error
        }
    }

}


