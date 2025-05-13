/*package org.example;

import org.bson.Document;
import org.example.MongoDBComponent;

import java.util.Date;

public class Inici {
   public static void main(String[] args) {
       //MongoDBComponent mongoDB = new MongoDBComponent("mongodb://localhost:27017", "Chat");

       // Insertar usuario
      Document usuari = new Document("nom", "patri")
               .append("contrasenya", "1234");
       mongoDB.insertData("usuaris", usuari);
       System.out.println("Usuari inserit");

       // Insertar missatge
       Document missatge = new Document("text", "Hola, soc la patri!")
               .append("dataHora", new Date())
               .append("usuari", "patri");
       mongoDB.insertData("missatges", missatge);
       System.out.println("Missatge inserit");
   }
}*/
