package test;

import java.io.*;
import java.net.*;

public class ClientConnection extends Thread {
    private Socket socketCliente;
    private HostNode hostPadre;
    private double scoreCliente;

    public ClientConnection(Socket socket, HostNode host) {
        this.socketCliente = socket;
        this.hostPadre = host;
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(socketCliente.getInputStream());
            scoreCliente = in.readDouble();

            // Verificar si este cliente debe ser el nuevo host
            hostPadre.verificarCambioHost(scoreCliente, this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocketCliente() {
        return socketCliente;
    }
}