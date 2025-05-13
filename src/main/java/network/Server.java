package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 12345;


    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor activo en el puerto " + PORT);


            while (true) {
                Socket socketCliente = serverSocket.accept();
                System.out.println("Nuevo cliente conectado: " + socketCliente);
                atender atenderCliente = new atender(socketCliente);
                atenderCliente.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

