package test;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client implements Runnable {
    private String hostAddress;

    public Client(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    @Override
    public void run() {
        AtomicBoolean running = new AtomicBoolean(true);

        while (running.get()) {
            try (Socket socket = new Socket(hostAddress, 5432);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Conectado al host: " + hostAddress);

                int clientScore = SystemEvaluator.calculateSystemScore();
                out.println("CLIENT_SCORE:" + clientScore);

                // Hilo para escuchar mensajes del host
                Thread listenerThread = new Thread(() -> {
                    try {
                        String message;
                        while ((message = in.readLine()) != null) {
                            if (message.startsWith("NEW_HOST:")) {
                                String newHostAddress = message.split(":")[1];
                                System.out.println("El host ha cambiado a: " + newHostAddress);

                                // Si este cliente es el nuevo host
                                if (newHostAddress.equals(getLocalIPAddress())) {
                                    System.out.println("Convirtiéndome en el nuevo host...");
                                    running.set(false); // Detener reconexión
                                    iniciarComoHost(); // Convertirse en host
                                    break;
                                } else {
                                    hostAddress = newHostAddress;
                                    running.set(false); // Detener el bucle para reconectar
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Error escuchando al host: " + e.getMessage());
                    }
                });
                listenerThread.start();

                listenerThread.join(); // Esperar hasta que el hilo termine
            } catch (IOException | InterruptedException e) {
                System.out.println("No se pudo conectar al host. Reintentando...");
                try {
                    Thread.sleep(3000); // Esperar antes de reintentar
                } catch (InterruptedException ignored) {
                }
            }
        }

        // Reconectar al nuevo host si no es el nuevo servidor
        try {
			if (!hostAddress.equals(getLocalIPAddress())) {
			    System.out.println("Reconectando al nuevo host: " + hostAddress);
			    run(); // Volver a conectarse como cliente
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    private void iniciarComoHost() {
        Host newHost = new Host();
        new Thread(newHost).start();
    }

    private String getLocalIPAddress() throws IOException {
        try (Socket socket = new Socket("8.8.8.8", 10002)) {
            return socket.getLocalAddress().getHostAddress();
        }
    }
}


