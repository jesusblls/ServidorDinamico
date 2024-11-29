package test;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("¿Quieres ser Host o Cliente? (0/1):");
        String role = scanner.nextLine().trim().toLowerCase();

        try {
            if (role.equals("0")) {
                Host host = new Host();
                host.run();
            } else if (role.equals("1")) {
                System.out.println("Ingresa la dirección IP del host:");
                String hostAddress = scanner.nextLine().trim();
                Client client = new Client(hostAddress);
                client.run();
            } else {
                System.out.println("Entrada no válida. Reinicia el programa e intenta de nuevo.");
            }
        } catch (Exception e) {
            System.err.println("Ocurrió un error: " + e.getMessage());
        }
    }
}
