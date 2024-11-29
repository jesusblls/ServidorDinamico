package test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClienteHandler implements Runnable {
    private Socket clientSocket;

    public ClienteHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());

            // Obtener el puntaje del cliente
            int puntajeCliente = (int) in.readObject();
            System.out.println("Cliente con puntaje: " + puntajeCliente);

            // Si el cliente tiene mejor puntaje, cambiar el host
            if (puntajeCliente > Host.calcularPuntaje()) {
                out.writeObject("Eres el nuevo Host!");
                System.out.println("Nuevo Host elegido.");
                // Aquí se podría implementar lógica para reconectar todos los clientes al nuevo host
            }

            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
