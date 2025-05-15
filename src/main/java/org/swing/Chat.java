package org.swing;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import com.mongodb.client.FindIterable;
import network.Client;
import org.bson.Document;
import org.example.MongoDBComponent;

public class Chat extends JFrame {
    private MongoDBComponent mongo;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;
    private String currentUser;
    private JTextField userField;
    private JPasswordField passwordField;
    public JPanel contentPane;
    public SecretKey secretKey = Client.establecerClaveSegura(Client.is,Client.os);
    public Cipher cipherAESencriptar = Cipher.getInstance("AES");
    public Cipher cipherAESDesencriptar = Cipher.getInstance("AES");

    // Constructor
    public Chat(String currentUser) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        this.currentUser = currentUser;
        mongo = new MongoDBComponent("mongodb://localhost:27017", "Chat");
        initUI();
    }

    private void initUI() {
        // Si el usuario no está autenticado, muestra la ventana de login.
        if (currentUser == null) {
            mostrarLogin(); // Mostrar pantalla de login
        } else {
            // Configurar la interfaz de chat
            setTitle("Aplicació de Xat");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(800, 600);
            setLocationRelativeTo(null);

            // Menu
            JMenuBar menuBar = new JMenuBar();
            JMenu menu = new JMenu("Opcions");
            JMenuItem consultarMissatges = new JMenuItem("Consultar missatges per data");
            consultarMissatges.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.CTRL_DOWN_MASK));
            consultarMissatges.addActionListener(e -> mostrarConsultaPerData());
            menu.add(consultarMissatges);
            menuBar.add(menu);
            setJMenuBar(menuBar);

            // Toolbar
            JToolBar toolBar = new JToolBar();
            JButton verUsuariosButton = new JButton("Veure usuaris");
            verUsuariosButton.addActionListener(e -> mostrarUsuarisConnectats());
            toolBar.add(verUsuariosButton);
            add(toolBar, BorderLayout.NORTH);

            // Panel principal
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

            // Área de chat
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            JScrollPane chatScrollPane = new JScrollPane(chatArea);
            mainPanel.add(chatScrollPane, BorderLayout.CENTER);

            // Campo de mensaje
            JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
            messageField = new JTextField();
            sendButton = new JButton("Enviar");
            sendButton.addActionListener(e -> enviarMensaje());
            inputPanel.add(messageField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            mainPanel.add(inputPanel, BorderLayout.SOUTH);

            // Lista de usuarios
            userListModel = new DefaultListModel<>();
            userList = new JList<>(userListModel);
            JScrollPane userScrollPane = new JScrollPane(userList);
            userScrollPane.setPreferredSize(new Dimension(150, 0));
            mainPanel.add(userScrollPane, BorderLayout.EAST);

            add(mainPanel);
            notificarConexion();
            setVisible(true);
        }
    }

    private void mostrarLogin() {
        // Configura la ventana de login
        setTitle("Inici de Sessió");
        setSize(300, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Usuari:"));
        userField = new JTextField();
        panel.add(userField);

        panel.add(new JLabel("Contrasenya:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Iniciar Sessió");
        loginButton.addActionListener(e -> {
            try {
                login();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        panel.add(loginButton);

        add(panel);
        setVisible(true);
    }

    private void login() throws IOException {
        try {
            cipherAESencriptar.init(Cipher.ENCRYPT_MODE,secretKey);
            cipherAESDesencriptar.init(Cipher.DECRYPT_MODE,secretKey);
            String usuari = userField.getText().trim();
            String contrasenya = new String(passwordField.getPassword()).trim();
            FindIterable<Document> result = mongo.findData("usuaris", new Document("nom", usuari).append("contrasenya", contrasenya));

            if (result.iterator().hasNext()) {
                dispose(); // Cierra la ventana de login
                SwingUtilities.invokeLater(() -> {
                    try {
                        new Chat(usuari);
                    } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                }); // Abre la app de chat con el usuario logueado
            } else {
                JOptionPane.showMessageDialog(this, "Credencials incorrectes.", "Error", JOptionPane.ERROR_MESSAGE);
            }
           Client.manejarNick(Client.dis,Client.dos,usuari,cipherAESencriptar,cipherAESDesencriptar);
        }catch (Exception e){

        }



    }
    private void enviarMensaje() {
        try {
            String mensaje = messageField.getText().trim();
            if (!mensaje.isEmpty()) {
                // Crear un documento con el mensaje
                Document mensajeDoc = new Document("text", mensaje)
                        .append("dataHora", new Date())  // Guardamos la fecha y hora del mensaje
                        .append("usuari", currentUser);  // Guardamos el usuario que envía el mensaje

                // Guardar el mensaje en la colección "missatges"
                mongo.guardarMensaje(currentUser,mensaje);  // Método para guardar el mensaje

                // Mostrar el mensaje en el área de chat
                chatArea.append(currentUser + ": " + mensaje + "\n");
                messageField.setText("");  // Limpiar el campo de entrada
                System.out.println("Mensaje enviado");
                Client.enviarMensaje(Client.dos, cipherAESencriptar,mensaje);

            }
        } catch (RuntimeException | IOException e) {
            throw new RuntimeException(e);
        }

    }


    private void mostrarUsuarisConnectats() {
        FindIterable<Document> usuarios = mongo.getData("usuarios");
        for (Document doc : usuarios) {
            System.out.println(doc.getString("usuari")); // O el campo que uses
        }
        if(!usuarios.iterator().hasNext()){
            System.out.println("No hi ha usuaris connectats");
        }
    }

    private void notificarConexion() {
        chatArea.append("[Sistema] Usuari connectat: " + currentUser + "\n");
    }

    private void mostrarConsultaPerData() {
        String fechaInput = JOptionPane.showInputDialog(this, "Introdueix la data (yyyy-MM-dd):");
        if (fechaInput != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date desde = sdf.parse(fechaInput);
                Date hasta = new Date(desde.getTime() + 86400000 - 1); // Fin del día
                List<Document> mensajes = mongo.obtenerMensajesPorFecha("mensajes", desde, hasta);
                chatArea.append("--- Missatges del " + fechaInput + " ---\n");
                for (Document msg : mensajes) {
                    chatArea.append(msg.getString("usuario") + ": " + msg.getString("mensaje") + "\n");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Data no vàlida.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new Chat(null);
            } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }); // Inicia con login
    }
}
