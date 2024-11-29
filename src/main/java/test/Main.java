package test;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Elige tu modo:");
        System.out.println("1. Host");
        System.out.println("2. Cliente");
        
        int modo = scanner.nextInt();
        
        if (modo == 1) {
            // Iniciar como host
            new HostNode().start();
        } else {
            // Iniciar como cliente
            new ClientNode().start();
        }
    }
}