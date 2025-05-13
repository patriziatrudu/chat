package network;


import java.io.*;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

class atender extends Thread {
    private Socket socket;
    private SecretKey claveAES;
    private String nick;


    private static final Semaphore semaforo = new Semaphore(1);
    private static final ArrayList<String> nicks = new ArrayList<>();
    private static final ArrayList<Socket> sockets = new ArrayList<>();
    private static final ArrayList<SecretKey> clavesAES = new ArrayList<>();


    public atender(Socket socket) {
        this.socket = socket;
    }


    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            DataInputStream dis = new DataInputStream(is);
            DataOutputStream dos = new DataOutputStream(os);


            // Intercambio de claves
            establecerClaveSegura(dis, dos);


            // Enviar mensaje cifrado "Introduce tu nick:"
            Cipher cipherAES = Cipher.getInstance("AES");
            cipherAES.init(Cipher.ENCRYPT_MODE, claveAES);
            byte[] msgNick = cipherAES.doFinal("Introduce tu nick:".getBytes());
            dos.writeInt(msgNick.length);
            dos.write(msgNick);
            dos.flush();


            // Leer el nick cifrado del cliente
            int longitudNick = dis.readInt();
            byte[] nickCifrado = new byte[longitudNick];
            dis.readFully(nickCifrado);


            cipherAES.init(Cipher.DECRYPT_MODE, claveAES);
            byte[] nickBytes = cipherAES.doFinal(nickCifrado);
            nick = new String(nickBytes);


            // Registrar cliente
            semaforo.acquire();
            if (nicks.contains(nick)) {
                String msg = "Este nick ya está en uso. Conexión cerrada.";
                byte[] errorMsg = cipherAES.doFinal(msg.getBytes());
                dos.writeInt(errorMsg.length);
                dos.write(errorMsg);
                dos.flush();
                semaforo.release();
                socket.close();
                return;
            } else {
                nicks.add(nick);
                sockets.add(socket);
                clavesAES.add(claveAES);
            }
            semaforo.release();


            broadcast("** " + nick + " se ha unido al chat **", socket);


            // Escuchar mensajes cifrados
            while (true) {
                int longitudMensaje = dis.readInt();
                byte[] mensajeCifrado = new byte[longitudMensaje];
                dis.readFully(mensajeCifrado);


                cipherAES.init(Cipher.DECRYPT_MODE, claveAES);
                byte[] mensajeBytes = cipherAES.doFinal(mensajeCifrado);
                String mensaje = new String(mensajeBytes);


                if (mensaje.equalsIgnoreCase("salir")) {
                    break;
                }


                broadcast("[" + nick + "]: " + mensaje, socket);
            }


        } catch (Exception e) {
            System.out.println("Error con el cliente " + nick);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }


            try {
                semaforo.acquire();
                int index = sockets.indexOf(socket);
                if (index != -1) {
                    nicks.remove(index);
                    sockets.remove(index);
                    clavesAES.remove(index);
                    broadcast("** " + nick + " ha salido del chat **", socket);
                }
                semaforo.release();
            } catch (InterruptedException ignored) {
            }
        }
    }


    private void establecerClaveSegura(DataInputStream dis, DataOutputStream dos) throws Exception {
        // Recibir clave pública
        byte[] claveEnBytes = new byte[294]; // Para claves RSA 2048
        dis.readFully(claveEnBytes);


        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey clavePublica = keyFactory.generatePublic(new X509EncodedKeySpec(claveEnBytes));


        // Generar y cifrar clave AES
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(128);
        claveAES = keygen.generateKey();


        Cipher cipherRSA = Cipher.getInstance("RSA");
        cipherRSA.init(Cipher.ENCRYPT_MODE, clavePublica);
        byte[] claveCifrada = cipherRSA.doFinal(claveAES.getEncoded());


        // Enviar clave cifrada al cliente
        dos.writeInt(claveCifrada.length);
        dos.write(claveCifrada);
        dos.flush();
    }


    private void broadcast(String mensaje, Socket origen) {
        try {
            semaforo.acquire();
            for (int i = 0; i < sockets.size(); i++) {
                Socket s = sockets.get(i);
                if (!s.equals(origen)) {
                    SecretKey clave = clavesAES.get(i);
                    Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.ENCRYPT_MODE, clave);
                    byte[] mensajeCifrado = cipher.doFinal(mensaje.getBytes());


                    DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                    dos.writeInt(mensajeCifrado.length);
                    dos.write(mensajeCifrado);
                    dos.flush();
                }
            }
            System.out.println(mensaje);


            semaforo.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}





