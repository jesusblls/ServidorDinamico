package test;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Selecciona una opción:");
        System.out.println("1. Ser Host");
        System.out.println("2. Ser Cliente");

        int choice = scanner.nextInt();

        if (choice == 1) {
            // Si es Host, crea el servidor
            new Thread(new Host()).start();
        } else if (choice == 2) {
            // Si es Cliente, se conecta al Host
            new Thread(new Cliente()).start();
        } else {
            System.out.println("Opción no válida.");
        }

        scanner.close();
    }
}
