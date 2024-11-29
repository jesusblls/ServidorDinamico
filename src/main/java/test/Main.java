package test;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("¿Quieres ser Host o Cliente? (0/1):");
        int choice = scanner.nextInt();

        if (choice == 0) {
            try {
                Host host = new Host();
                new Thread(host).start();
            } catch (Exception e) {
                System.out.println("Error al iniciar como host: " + e.getMessage());
            }
        } else {
            scanner.nextLine(); // Limpiar el buffer
            System.out.println("Ingresa la dirección IP del host:");
            String hostAddress = scanner.nextLine();

            Client client = new Client(hostAddress);
            new Thread(client).start();
        }

        scanner.close();
    }
}
