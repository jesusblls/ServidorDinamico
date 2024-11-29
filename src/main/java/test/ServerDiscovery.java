package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

public class ServerDiscovery {
    private static final int PUERTO_DESCUBRIMIENTO = 2345;
    private static final int PUERTO_SERVIDOR = 5000;

    public static String obtenerIPDelServidor() throws Exception {
        // Intentar múltiples métodos de descubrimiento
        
        // Método 1: Broadcast UDP
        String ipPorBroadcast = descubrirPorBroadcast();
        if (ipPorBroadcast != null) {
            return ipPorBroadcast;
        }

        // Método 2: Escaneo de interfaces de red
        String ipPorInterfaz = descubrirPorInterfaces();
        if (ipPorInterfaz != null) {
            return ipPorInterfaz;
        }

        System.out.println("No se pudo descubrir el servidor por ningún método.");
        return null;
    }

    private static String descubrirPorBroadcast() {
        try {
            // Obtener todas las interfaces de red
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Saltar interfaces que no estén activas o sean loopback
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                // Buscar dirección de broadcast
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress broadcast = interfaceAddress.getBroadcast();
                    if (broadcast == null) continue;

                    try (DatagramSocket socket = new DatagramSocket()) {
                        socket.setBroadcast(true);
                        socket.setSoTimeout(5000); // 5 segundos de timeout

                        String mensajeDescubrimiento = "Servidor_Descubierto";
                        byte[] bytesMensaje = mensajeDescubrimiento.getBytes();

                        DatagramPacket paquete = new DatagramPacket(
                            bytesMensaje, 
                            bytesMensaje.length, 
                            broadcast, 
                            PUERTO_DESCUBRIMIENTO
                        );
                        socket.send(paquete);
                        System.out.println("Enviando broadcast por " + networkInterface.getName() + 
                                           " a dirección: " + broadcast.getHostAddress());

                        // Preparar buffer para recibir respuesta
                        byte[] bufferRecibimiento = new byte[256];
                        DatagramPacket paqueteRespuesta = new DatagramPacket(
                            bufferRecibimiento, 
                            bufferRecibimiento.length
                        );

                        // Intentar recibir respuesta
                        socket.receive(paqueteRespuesta);

                        // Convertir respuesta a IP del servidor
                        String ipServidor = new String(
                            paqueteRespuesta.getData(), 
                            0, 
                            paqueteRespuesta.getLength()
                        ).trim();

                        System.out.println("Servidor encontrado: " + ipServidor);
                        return ipServidor;

                    } catch (SocketTimeoutException e) {
                        System.out.println("Timeout en interfaz " + networkInterface.getName());
                    } catch (IOException e) {
                        System.out.println("Error en interfaz " + networkInterface.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en descubrimiento por broadcast: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static String descubrirPorInterfaces() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }

                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress ip = interfaceAddress.getAddress();
                    
                    // Ignorar IPv6 y localhost
                    if (ip.isLoopbackAddress() || ip instanceof Inet6Address) {
                        continue;
                    }

                    // Intentar conectar al puerto del servidor
                    String baseIP = ip.getHostAddress().substring(0, ip.getHostAddress().lastIndexOf(".") + 1);
                    
                    for (int i = 1; i <= 254; i++) {
                        String testIP = baseIP + i;
                        try {
                            Socket socket = new Socket();
                            socket.connect(new InetSocketAddress(testIP, PUERTO_SERVIDOR), 500);
                            socket.close();
                            System.out.println("Servidor encontrado en: " + testIP);
                            return testIP;
                        } catch (Exception e) {
                            // No se puede conectar, continuar
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error en descubrimiento por interfaces: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Clase para el lado del servidor que responde al descubrimiento
    public static class ServidorDescubrimiento extends Thread {
        private DatagramSocket socket;
        private int puerto;

        public ServidorDescubrimiento(int puerto) {
            this.puerto = puerto;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(puerto);
                socket.setBroadcast(true);

                System.out.println("Servidor de descubrimiento iniciado en puerto " + puerto);

                while (!isInterrupted()) {
                    byte[] bufferRecepcion = new byte[256];
                    DatagramPacket paqueteRecibido = new DatagramPacket(
                        bufferRecepcion, 
                        bufferRecepcion.length
                    );

                    // Recibir paquete de descubrimiento
                    socket.receive(paqueteRecibido);

                    // Verificar si es un mensaje de descubrimiento
                    String mensajeRecibido = new String(
                        paqueteRecibido.getData(), 
                        0, 
                        paqueteRecibido.getLength()
                    ).trim();

                    System.out.println("Mensaje recibido: " + mensajeRecibido);

                    if ("Servidor_Descubierto".equals(mensajeRecibido)) {
                        // Obtener la dirección IP local
                        String miDireccionIP = obtenerIPLocal();

                        System.out.println("Respondiendo con IP: " + miDireccionIP);

                        // Responder al cliente con mi dirección IP
                        byte[] respuesta = miDireccionIP.getBytes();
                        DatagramPacket paqueteRespuesta = new DatagramPacket(
                            respuesta, 
                            respuesta.length, 
                            paqueteRecibido.getAddress(), 
                            paqueteRecibido.getPort()
                        );

                        socket.send(paqueteRespuesta);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error en servidor de descubrimiento: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }

        private String obtenerIPLocal() {
            try {
                // Intentar obtener IP de la interfaz de Hamachi
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                        continue;
                    }
        
                    // Filtrar por nombre de interfaz que contenga "Hamachi"
                    if (networkInterface.getName().contains("Hamachi")) {
                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            InetAddress ip = interfaceAddress.getAddress();
                            if (ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
                                return ip.getHostAddress();
                            }
                        }
                    }
                }
        
                // Fallback a localhost si no se encuentra otra IP
                return InetAddress.getLocalHost().getHostAddress();
            } catch (Exception e) {
                System.out.println("Error obteniendo IP local: " + e.getMessage());
                return "127.0.0.1";
            }
        }
    }
}