package network;

import java.io.*;
import java.net.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;


public class Client {
    private static String HOST;
    private static int PORT;
    private static Socket socket;

    static {
        try {
            socket = new Socket(HOST, PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream is;

    static {
        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static OutputStream os;

    static {
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DataInputStream dis = new DataInputStream(is);
    private static DataOutputStream dos = new DataOutputStream(os);
    private static BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

    public Client(String host, int port) {
        this.HOST = host;
        this.PORT = port;
        try {


        } catch (Exception e) {

        }

    }


    private static void enviarMensaje(DataOutputStream dos, Cipher cipherAESencriptar, String mensajeTexto)
            throws IOException {
        try {
            byte[] mensajeCifradoTexto = cipherAESencriptar.doFinal(mensajeTexto.getBytes());
            dos.writeInt(mensajeCifradoTexto.length);
            dos.write(mensajeCifradoTexto);
            dos.flush();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("Error al cifrar el mensaje.");
            e.printStackTrace();
        }
    }


    private static void manejarNick(DataInputStream dis, DataOutputStream dos, BufferedReader teclado,
                                    Cipher cipherAESencriptar, Cipher cipherAESdesencriptar) throws IOException {
        try {
            // Recibir el mensaje del servidor ("Introduce tu nick:")
            int longitudMensaje = dis.readInt();
            byte[] mensajeCifrado = new byte[longitudMensaje];
            dis.readFully(mensajeCifrado);


            byte[] mensajeDescifrado = cipherAESdesencriptar.doFinal(mensajeCifrado);
            System.out.println(new String(mensajeDescifrado));


            // Enviar el nick cifrado
            String nick = teclado.readLine();
            byte[] nickCifrado = cipherAESencriptar.doFinal(nick.getBytes());
            dos.writeInt(nickCifrado.length);
            dos.write(nickCifrado);
            dos.flush();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("Error al manejar el nick.");
            e.printStackTrace();
        }
    }


    private static SecretKey establecerClaveSegura(InputStream is, OutputStream os) throws IOException {
        try {
            DataInputStream dis = new DataInputStream(is);
            DataOutputStream dos = new DataOutputStream(os);


            // Generar par de claves RSA
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
            keygen.initialize(2048);
            KeyPair keypair = keygen.generateKeyPair();


            // Enviar clave pública al servidor
            byte[] claveEnBytes = keypair.getPublic().getEncoded();
            os.write(claveEnBytes);
            os.flush();


            // Recibir clave AES cifrada con RSA
            int longitudClave = dis.readInt();
            byte[] claveCifrada = new byte[longitudClave];
            dis.readFully(claveCifrada);


            // Descifrar clave AES
            Cipher dCipher = Cipher.getInstance("RSA");
            dCipher.init(Cipher.DECRYPT_MODE, keypair.getPrivate());
            byte[] claveAES = dCipher.doFinal(claveCifrada);


            return new SecretKeySpec(claveAES, "AES");


        } catch (GeneralSecurityException e) {
            throw new IOException("Error al establecer clave segura", e);
        }
    }


    static class HiloEscucha extends Thread {
        private DataInputStream dis;
        private SecretKey secretKey;


        public HiloEscucha(InputStream is, SecretKey secretKey) {
            this.dis = new DataInputStream(is);
            this.secretKey = secretKey;
        }


        public void run() {
            try {
                Cipher cipherAES = Cipher.getInstance("AES");
                cipherAES.init(Cipher.DECRYPT_MODE, secretKey);


                while (true) {
                    int longitud = dis.readInt(); // Leer la longitud del mensaje
                    byte[] mensajeCifrado = new byte[longitud];
                    dis.readFully(mensajeCifrado); // Leer el mensaje cifrado


                    byte[] mensajeDescifrado = cipherAES.doFinal(mensajeCifrado);
                    System.out.println(new String(mensajeDescifrado));
                }


            } catch (IOException e) {
                System.out.println("Conexión cerrada por el servidor.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}


