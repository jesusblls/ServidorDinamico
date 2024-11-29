package test;

import java.io.*;
import java.net.Socket;

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

                int clientScore = SystemEvaluator.calculateSystemScore();
                out.println("CLIENT_SCORE:" + clientScore);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("NEW_HOST:")) {
                        String newHostAddress = message.split(":")[1];
                        System.out.println("El host ha cambiado a: " + newHostAddress);

                        if (newHostAddress.equals(getLocalIPAddress())) {
                            System.out.println("Convirtiéndome en el nuevo host...");
                            iniciarComoHost();
                            return;
                        } else {
                            hostAddress = newHostAddress;
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

    private void iniciarComoHost() {
        Host newHost = new Host();
        Thread hostThread = new Thread(newHost);
        hostThread.start();
    }

    private String getLocalIPAddress() throws IOException {
        try (Socket socket = new Socket("8.8.8.8", 10002)) {
            return socket.getLocalAddress().getHostAddress();
        }
    }
}
